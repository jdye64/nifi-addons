/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeremydyer.nifi.processors.google;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

@Tags({"Google", "Speech", "speech to text"})
@CapabilityDescription("Provide a description")
@SeeAlso({GoogleVisionProcessor.class})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class GoogleSpeechProcessor
        extends AbstractProcessor {

    public static final PropertyDescriptor MY_PROPERTY = new PropertyDescriptor
            .Builder().name("My Property")
            .description("Example Property")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Example relationship")
            .build();

    public static final Relationship REL_ORIGINAL = new Relationship.Builder()
            .name("original")
            .description("Original input flowfile")
            .build();

    public static final Relationship REL_NO_RESULTS = new Relationship.Builder()
            .name("no results")
            .description("No speech to text results were returned from the Google API")
            .build();

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(MY_PROPERTY);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_ORIGINAL);
        relationships.add(REL_NO_RESULTS);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    private SpeechClient speechClient = null;

    @OnScheduled
    public void onScheduled(final ProcessContext context) throws IOException, GeneralSecurityException {

        try {
            speechClient = SpeechClient.create();
        } catch (Exception ex) {
            System.out.println("Exception thrown here");
            throw ex;
        }

    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {

        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        try {

            final AtomicReference<List<SpeechRecognitionResult>> speechResults = new AtomicReference<>();

            session.read(flowFile, new InputStreamCallback() {
                @Override
                public void process(InputStream inputStream) throws IOException {
                    byte[] data = IOUtils.toByteArray(inputStream);
                    ByteString audioBytes = ByteString.copyFrom(data);

                    // Configure request with local raw PCM audio
                    RecognitionConfig config = RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setLanguageCode("en-US")
                            .setSampleRateHertz(16000)
                            .build();
                    RecognitionAudio audio = RecognitionAudio.newBuilder()
                            .setContent(audioBytes)
                            .build();

                    // Use blocking call to get audio transcript
                    RecognizeResponse response = speechClient.recognize(config, audio);
                    speechResults.set(response.getResultsList());
                }
            });

            if (speechResults.get().size() > 0) {
                for (final SpeechRecognitionResult result : speechResults.get()) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    FlowFile ff = session.write(session.create(), new OutputStreamCallback() {
                        @Override
                        public void process(OutputStream outputStream) throws IOException {
                            outputStream.write(alternative.getTranscript().getBytes());
                        }
                    });

                    // Updates the attributes based on the response from Google.
                    session.putAttribute(ff, "google.speech.confidence", String.valueOf(alternative.getConfidence()));
                    session.putAttribute(ff, "google.speech.serialized.size", String.valueOf(alternative.getSerializedSize()));
                    session.putAttribute(ff, "google.speech.words.count", String.valueOf(alternative.getWordsCount()));

                    session.transfer(ff, REL_SUCCESS);
                    session.transfer(flowFile, REL_ORIGINAL);
                }
            } else {
                // No results were found ....
                session.transfer(flowFile, REL_NO_RESULTS);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

}

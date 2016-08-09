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
package com.jeremydyer.processors.spinx;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Tags({"spinx", "speech", "text"})
@CapabilityDescription("Processor for ")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class SpeechToText extends AbstractProcessor {

    public static final PropertyDescriptor ACOUSTIC_MODEL_PATH = new PropertyDescriptor
            .Builder().name("Acoustic Model Path")
            .description("URI to the Spinx Acoustic Model Path")
            .defaultValue("resource:/edu/cmu/sphinx/models/en-us/en-us")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor DICTIONARY_PATH = new PropertyDescriptor
            .Builder().name("Acoustic Model Path")
            .description("URI to the Spinx Acoustic Model Path")
            .defaultValue("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor LANGUAGE_MODEL_PATH = new PropertyDescriptor
            .Builder().name("Language Model Path")
            .description("URI to the Spinx Language Model Path")
            .defaultValue("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("successfully performed speech to text")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("failure encountered while attempting speech to text")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
//        descriptors.add(ACOUSTIC_MODEL_PATH);
//        descriptors.add(DICTIONARY_PATH);
//        descriptors.add(LANGUAGE_MODEL_PATH);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
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

    private final Configuration configuration = new Configuration();
    private final AtomicReference<StreamSpeechRecognizer> speechRecognizer = new AtomicReference<>();

    @OnScheduled
    public void onScheduled(final ProcessContext context) throws IOException {
//        configuration
//                .setAcousticModelPath(context.getProperty(ACOUSTIC_MODEL_PATH).getValue());
//        configuration
//                .setDictionaryPath(context.getProperty(DICTIONARY_PATH).getValue());
//        configuration
//                .setLanguageModelPath(context.getProperty(LANGUAGE_MODEL_PATH).getValue());

        configuration
                .setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration
                .setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration
                .setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        speechRecognizer.set(new StreamSpeechRecognizer(configuration));
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        try {
            FlowFile ff = session.write(flowFile, new StreamCallback() {
                @Override
                public void process(InputStream inputStream, OutputStream outputStream) throws IOException {

                    getLogger().debug("Beginning SpeechToText");
                    System.out.println("Starting");
                    long start = System.currentTimeMillis();

                    StreamSpeechRecognizer recognizer = speechRecognizer.get();
                    recognizer.startRecognition(inputStream);
                    SpeechResult result;

                    StringBuffer buffer = new StringBuffer();

                    while ((result = recognizer.getResult()) != null) {
                        getLogger().info("Hypothesis: " + result.getHypothesis());
                        buffer.append(result.getHypothesis());
                        buffer.append(" ");
                    }
                    recognizer.stopRecognition();

                    getLogger().info("Hypothesis: " + buffer.toString());

                    //Writes the output out
                    outputStream.write(buffer.toString().getBytes());

                    System.out.println("Processing took: " + (System.currentTimeMillis() - start) + "ms");
                }
            });

            session.transfer(ff, REL_SUCCESS);

        } catch (Exception ex) {
            getLogger().error(ex.getMessage());
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}

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
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
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

import com.google.cloud.language.v1.AnalyzeSentimentResponse;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;

@Tags({"Google", "language", "sentiment", "entities", "syntax", "categories"})
@CapabilityDescription("Processor that integrates with Google Cloud Language. Google Cloud Natural Language reveals the structure and meaning of text " +
        "by offering powerful machine learning models. You can use it to extract information about people, places, events and much more, mentioned in text " +
        "documents, news articles or blog posts. You can use it to understand sentiment about your product on social media or parse intent from " +
        "customer conversations happening in a call center or a messaging app")
@SeeAlso({GoogleVisionProcessor.class, GoogleSpeechProcessor.class})
@WritesAttributes({
        @WritesAttribute(attribute="google.cloud.language.sentiment.score", description="Sentiment returned from Google Cloud Language for the text sent"),
        @WritesAttribute(attribute = "google.cloud.language.sentiment.score", description = "Magnitude returned from Google Cloud Language for the text sent")
})
public class GoogleLanguageProcessor
    extends AbstractProcessor {

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Example relationship")
            .build();

    public static final Relationship REL_NO_RESULTS = new Relationship.Builder()
            .name("no results")
            .description("No speech to text results were returned from the Google API")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("An error occurred while processing the Google Cloud Language request")
            .build();

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_NO_RESULTS);
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

    private LanguageServiceClient languageClient = null;

    @OnScheduled
    public void onScheduled(final ProcessContext context) throws IOException, GeneralSecurityException {
        try {
            languageClient = LanguageServiceClient.create();
        } catch (Exception ex) {
            System.out.println("Exception thrown here");
            throw ex;
        }
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        try {

            final AtomicReference<AnalyzeSentimentResponse> result = new AtomicReference<>();

            // Read the incoming flowfile contents and pass that data to the Google Cloud Language service.
            session.read(flowFile, new InputStreamCallback() {
                @Override
                public void process(InputStream inputStream) throws IOException {
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(inputStream, writer, "UTF-8");
                    String flowFileString = writer.toString();

                    Document doc = Document.newBuilder()
                            .setContent(flowFileString).setType(Document.Type.PLAIN_TEXT).build();

                    result.set(languageClient.analyzeSentiment(doc));
                }
            });

            if (result.get().hasDocumentSentiment()) {
                Sentiment sentiment = result.get().getDocumentSentiment();

                flowFile = session.putAttribute(flowFile, "google.cloud.language.sentiment.score", String.valueOf(sentiment.getScore()));
                flowFile = session.putAttribute(flowFile, "google.cloud.language.sentiment.magnitude", String.valueOf(sentiment.getMagnitude()));

                session.transfer(flowFile, REL_SUCCESS);
            } else {
                // No sentiment results were found.
                session.transfer(flowFile, REL_NO_RESULTS);
            }

        } catch (Exception ex) {
            getLogger().error(ex.getMessage());
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}

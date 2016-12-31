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
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
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
import org.apache.nifi.processor.util.StandardValidators;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.common.collect.ImmutableList;

@Tags({"Google", "Speech", "speech to text"})
@CapabilityDescription("Provide a description")
@SeeAlso({GoogleVisionProcessor.class})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class GoogleSpeechProcessor extends AbstractProcessor {

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

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;


    private static final int MAX_RESULTS = 4;
    private static final String APPLICATION_NAME = "Google-VisionDetectLandmark/1.0";


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(MY_PROPERTY);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
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

    @OnScheduled
    public void onScheduled(final ProcessContext context) throws IOException, GeneralSecurityException {
        this.vision = getVisionService();
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        try {

            AtomicReference<String> image = new AtomicReference<>();

            session.read(flowFile, new InputStreamCallback() {
                @Override
                public void process(InputStream inputStream) throws IOException {
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    byte[] encImage = Base64.getEncoder().encode(bytes);
                    image.set(new String(encImage));
                }
            });

            AnnotateImageRequest request =
                    new AnnotateImageRequest()
                            .setImage(new Image().setContent(new String(image.get())))
                            .setFeatures(ImmutableList.of(
                                    new Feature()
                                            .setType("LANDMARK_DETECTION")
                                            .setMaxResults(MAX_RESULTS)));

            Vision.Images.Annotate annotate =
                    vision.images()
                            .annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));

            BatchAnnotateImagesResponse batchResponse = annotate.execute();
            assert batchResponse.getResponses().size() == 1;
            AnnotateImageResponse response = batchResponse.getResponses().get(0);
            if (response.getLandmarkAnnotations() == null) {
                throw new IOException(
                        response.getError() != null
                                ? response.getError().getMessage()
                                : "Unknown error getting image annotations");
            }

            StringBuilder lndMarks = new StringBuilder();

            List<EntityAnnotation> landmarks = response.getLandmarkAnnotations();
            System.out.printf("Found %d landmark%s\n", landmarks.size(), landmarks.size() == 1 ? "" : "s");
            for (EntityAnnotation annotation : landmarks) {
                System.out.printf("\t%s\n", annotation.getDescription());
                lndMarks.append(annotation.getDescription());
                lndMarks.append(", ");
            }

            flowFile = session.putAttribute(flowFile, "landmarks", lndMarks.toString());

            session.transfer(flowFile, REL_SUCCESS);

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    private Vision vision = null;


    // [START authenticate]
    /**
     * Connects to the Vision API using Application Default Credentials.
     */
    public static Vision getVisionService() throws IOException, GeneralSecurityException {
        GoogleCredential credential =
                GoogleCredential.getApplicationDefault().createScoped(VisionScopes.all());
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

}

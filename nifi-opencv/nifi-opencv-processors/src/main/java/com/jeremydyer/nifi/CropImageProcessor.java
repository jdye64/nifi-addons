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

package com.jeremydyer.nifi;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Tags({"opencv", "crop", "image"})
@CapabilityDescription("Crops an image to the desired dimensions")
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes(
        {
                @WritesAttribute(attribute="image.width", description = "Width of the original image"),
                @WritesAttribute(attribute="image.height", description = "Height of the original image")
        }
)
public class CropImageProcessor extends AbstractProcessor {

    public static final PropertyDescriptor X_POINT = new PropertyDescriptor
            .Builder().name("X Point")
            .description("X coordinate location to begin the image cropping.")
            .required(true)
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    public static final PropertyDescriptor Y_POINT = new PropertyDescriptor
            .Builder().name("Y Point")
            .description("Y coordinate location to begin the image cropping.")
            .required(true)
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    public static final PropertyDescriptor CROP_WIDTH = new PropertyDescriptor
            .Builder().name("Crop Width")
            .description("Number of pixels width to crop the image")
            .required(true)
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    public static final PropertyDescriptor CROP_HEIGHT = new PropertyDescriptor
            .Builder().name("Crop Height")
            .description("Number of pixels heigh to crop the image")
            .required(true)
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    public static final Relationship REL_ORIGINAL = new Relationship.Builder()
            .name("original")
            .description("original input")
            .build();

    public static final Relationship REL_CROPPED_IMAGE = new Relationship.Builder()
            .name("cropped_image")
            .description("cropped image")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("failure during processing")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(X_POINT);
        descriptors.add(Y_POINT);
        descriptors.add(CROP_WIDTH);
        descriptors.add(CROP_HEIGHT);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_ORIGINAL);
        relationships.add(REL_CROPPED_IMAGE);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);

        //Load the OpenCV Native Library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile original = session.get();
        if ( original == null ) {
            return;
        }

        session.read(original, new InputStreamCallback() {
            @Override
            public void process(InputStream inputStream) throws IOException {

                try {
                    byte[] imgData = IOUtils.toByteArray(inputStream);
                    Mat image = Imgcodecs.imdecode(new MatOfByte(imgData), Imgcodecs.CV_LOAD_IMAGE_COLOR);
                    cropImage(session, context, original, image);
                } catch (Exception ex) {
                    getLogger().error(ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        session.transfer(original, REL_ORIGINAL);

    }

    final public void cropImage(final ProcessSession session, ProcessContext context, FlowFile original, final Mat image) {

        final int x = context.getProperty(X_POINT).evaluateAttributeExpressions(original).asInteger();
        final int y = context.getProperty(Y_POINT).evaluateAttributeExpressions(original).asInteger();
        final int width = context.getProperty(CROP_WIDTH).evaluateAttributeExpressions(original).asInteger();
        final int height = context.getProperty(CROP_HEIGHT).evaluateAttributeExpressions(original).asInteger();

        FlowFile croppedImage = session.write(session.create(original), new OutputStreamCallback() {
            @Override
            public void process(OutputStream outputStream) throws IOException {
                Rect rectCrop = new Rect(x, y, width, height);
                Mat croppedImage = new Mat(image, rectCrop);

                MatOfByte updatedImage = new MatOfByte();
                Imgcodecs.imencode(".jpg", croppedImage, updatedImage);
                outputStream.write(updatedImage.toArray());
            }
        });

        Map<String, String> atts = new HashMap<>();
        atts.put("image.width", new Integer(width).toString());
        atts.put("image.height", new Integer(height).toString());

        croppedImage = session.putAllAttributes(croppedImage, atts);

        session.transfer(croppedImage, REL_CROPPED_IMAGE);
    }
}
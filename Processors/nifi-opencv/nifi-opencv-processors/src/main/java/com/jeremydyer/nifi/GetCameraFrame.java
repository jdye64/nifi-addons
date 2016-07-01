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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
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
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.stream.io.ByteArrayInputStream;
import org.opencv.core.Core;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;


@Tags({ "image" })
@CapabilityDescription("Extract a frame from the camera")
@WritesAttributes({ @WritesAttribute(attribute = "", description = "") })
public class GetCameraFrame extends AbstractProcessor {

    public static final PropertyDescriptor CAMERA = new PropertyDescriptor.Builder()
            .name("Camera").description("Camera Device Identifier")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("FlowFiles are generated from image frames").build();

    public static final PropertyDescriptor FRAME_WIDTH = new PropertyDescriptor.Builder()
            .name("Width").description("Width in pixels of capture")
            .required(false).defaultValue("1280")
            .addValidator(StandardValidators.INTEGER_VALIDATOR).build();
    public static final PropertyDescriptor FRAME_HEIGHT = new PropertyDescriptor.Builder()
            .name("Height").description("Height in pixels of capture")
            .required(false).defaultValue("720")
            .addValidator(StandardValidators.INTEGER_VALIDATOR).build();

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    private VideoCapture camera;
    private final int CAMERA_ID = 0;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        //descriptors.add(CAMERA);
        descriptors.add(FRAME_WIDTH);
        descriptors.add(FRAME_HEIGHT);

        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        this.relationships = Collections.unmodifiableSet(relationships);

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        camera = new VideoCapture(CAMERA_ID);
        try {
            Thread.sleep(2000);             //Give the Camera a few seconds to initialize
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        camera.open(CAMERA_ID);

        if(!camera.isOpened()){
            getLogger().error("Camera Error");
        }
        else{
            getLogger().error("Camera OK?");
        }

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
    public void onScheduled(final ProcessContext context) {
        double width = context.getProperty(FRAME_WIDTH).asDouble()
                .doubleValue();

        camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, width);
        double height = context.getProperty(FRAME_HEIGHT).asDouble()
                .doubleValue();
        camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, height);

    }

    @Override
    public void onTrigger(final ProcessContext context,
            final ProcessSession session) throws ProcessException {
        MatOfByte image = new MatOfByte();
        MatOfByte bytemat = new MatOfByte();

        camera.read(image);
        Imgcodecs.imencode(".png", image, bytemat);
        byte[] bytes = bytemat.toArray();

        InputStream in = new ByteArrayInputStream(bytes);

        try {
            final BufferedImage img = ImageIO.read(in);
            FlowFile flowFile = session.create();
            flowFile = session.write(flowFile, new OutputStreamCallback() {
                @Override
                public void process(final OutputStream out) throws IOException {
                    ImageIO.write(img, "PNG", out);
                }
            });
            session.getProvenanceReporter().create(flowFile);
            session.transfer(flowFile, REL_SUCCESS);
        } catch (IOException e) {

        }

    }

}
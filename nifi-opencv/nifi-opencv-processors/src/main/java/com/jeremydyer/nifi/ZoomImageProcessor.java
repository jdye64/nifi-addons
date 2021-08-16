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
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

@Tags({"opencv", "zoom", "image"})
@CapabilityDescription("Takes the incoming image and zooms in on it")
public class ZoomImageProcessor extends AbstractProcessor {

    public static final PropertyDescriptor ZOOMING_FACTOR = new PropertyDescriptor
            .Builder().name("Zooming Factor")
            .description("FActor for how zoomed in the resulting image will be")
            .required(true)
            .defaultValue("2")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    public static final Relationship REL_ORIGINAL = new Relationship.Builder()
            .name("original")
            .description("original input")
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("successful zoom")
            .description("zoomed in image")
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
        descriptors.add(ZOOMING_FACTOR);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_ORIGINAL);
        relationships.add(REL_SUCCESS);
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

        session.transfer(session.clone(original), REL_ORIGINAL);

        FlowFile ff = session.write(original, new StreamCallback() {
            @Override
            public void process(InputStream inputStream, OutputStream outputStream) throws IOException {
                try {
                    int zoomingFactor = context.getProperty(ZOOMING_FACTOR).asInteger();

                    BufferedImage image = ImageIO.read(inputStream);
                    byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                    Mat source = new Mat(image.getHeight(), image.getWidth(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
                    source.put(0, 0, pixels);

                    Mat destination = new Mat(source.rows() * zoomingFactor, source.cols() * zoomingFactor, source.type());
                    Imgproc.resize(source, destination, destination.size(), zoomingFactor, zoomingFactor, Imgproc.INTER_NEAREST);

                    MatOfByte bytemat = new MatOfByte();
                    Imgcodecs.imencode(".png", destination, bytemat);
                    pixels = bytemat.toArray();
                    outputStream.write(pixels);

                } catch (Exception ex) {
                    getLogger().error(ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        session.transfer(ff, REL_SUCCESS);

    }
}
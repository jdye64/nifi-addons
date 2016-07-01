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
package com.jeremydyer.nifi.processors.barcode;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
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
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.StreamCallback;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

@Tags({"barcode, scanner"})
@CapabilityDescription("Scans an image and searches for a barcode. The text of the found barcode is emitted by the processor.")
@WritesAttributes(
        {
                @WritesAttribute(attribute="barcode", description = "Barcode number pulled from the image"),
        }
)
public class BarcodeScannerProcessor extends AbstractProcessor {

    public static final String BARCODE_ATTRIBUTE_NAME = "barcode";

    public static final String DESTINATION_ATTRIBUTE = "flowfile-attribute";
    public static final String DESTINATION_CONTENT = "flowfile-content";

    public static final PropertyDescriptor DESTINATION = new PropertyDescriptor.Builder()
            .name("Destination")
            .description("Control if detected barcode value is written as a new flowfile attribute '" + BARCODE_ATTRIBUTE_NAME + "' " +
                    "or written in the flowfile content. Writing to flowfile content will overwrite any " +
                    "existing flowfile content.")
            .required(true)
            .allowableValues(DESTINATION_ATTRIBUTE, DESTINATION_CONTENT)
            .defaultValue(DESTINATION_CONTENT)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Successfully found barcode")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Failed to obtain barcode from image")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(DESTINATION);
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

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        final AtomicBoolean errors = new AtomicBoolean(false);

        switch (context.getProperty(DESTINATION).getValue()) {
            case DESTINATION_ATTRIBUTE:
                final AtomicReference<String> BC = new AtomicReference<>();
                session.read(flowFile, new InputStreamCallback() {
                    @Override
                    public void process(InputStream inputStream) throws IOException {

                        Map hintMap = new HashMap();
                        hintMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

                        try {
                            BufferedImage barCodeBufferedImage = ImageIO.read(inputStream);

                            LuminanceSource source = new BufferedImageLuminanceSource(barCodeBufferedImage);
                            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                            Reader reader = new MultiFormatReader();
                            Result result = reader.decode(bitmap, hintMap);
                            BC.set(result.getText());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            //session.transfer(flowFile, REL_FAILURE);
                            errors.set(true);
                        }
                    }
                });

                if (StringUtils.isNotEmpty(BC.get())) {
                    FlowFile atFlowFile = session.putAttribute(flowFile, BARCODE_ATTRIBUTE_NAME, BC.get());
                    if (!errors.get()) {
                        session.transfer(atFlowFile, REL_SUCCESS);
                    } else {
                        session.transfer(atFlowFile, REL_FAILURE);
                    }
                } else {
                    if (!errors.get()) {
                        session.transfer(flowFile, REL_SUCCESS);
                    } else {
                        session.transfer(flowFile, REL_FAILURE);
                    }
                }

                break;
            case DESTINATION_CONTENT:
                FlowFile conFlowFile = session.write(flowFile, new StreamCallback() {
                    @Override
                    public void process(InputStream inputStream, OutputStream outputStream) throws IOException {

                        Map hintMap = new HashMap();
                        hintMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

                        try {
                            BufferedImage barCodeBufferedImage = ImageIO.read(inputStream);

                            LuminanceSource source = new BufferedImageLuminanceSource(barCodeBufferedImage);
                            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                            Reader reader = new MultiFormatReader();
                            Result result = reader.decode(bitmap, hintMap);
                            getLogger().info("Barcode Format: " + result.getBarcodeFormat().toString());
                            getLogger().info("Barcode Text is: ' " + result.getText() + "'");
                            outputStream.write(result.getText().getBytes());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            //session.transfer(flowFile, REL_FAILURE);
                            errors.set(true);
                        }
                    }
                });

                if (!errors.get()) {
                    session.transfer(conFlowFile, REL_SUCCESS);
                } else {
                    session.transfer(conFlowFile, REL_FAILURE);
                }

                break;
        }
    }
}

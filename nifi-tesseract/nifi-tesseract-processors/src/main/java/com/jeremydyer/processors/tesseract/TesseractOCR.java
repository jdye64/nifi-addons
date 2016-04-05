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
package com.jeremydyer.processors.tesseract;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract1;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Tags({"ocr", "tesseract", "image"})
@CapabilityDescription("Reads the input image and attempts to perform OCR on the image and output the text " +
        "read from the image the the content of the output FlowFile")
public class TesseractOCR extends AbstractProcessor {

    public static final PropertyDescriptor TESSERACT_INSTALL_DIR = new PropertyDescriptor
            .Builder().name("Tesseract Installation Directory")
            .description("Base location on the local filesystem where Tesseract is installed")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("/usr/bin/tesseract")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("successfully completed OCR on image")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Failed to attempt OCR on input image")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(TESSERACT_INSTALL_DIR);
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

    //TODO: This is bad. Hardcoded to my brew OS X install now just for demo purposes.
    private final static String DEFAULT_PAGE_SEG_MODE = "3";
    private final static String DEFAULT_LANG = "eng";

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        //Reads in the image data and runs it through Tesseract for OCR
        FlowFile ff = session.write(flowFile, new StreamCallback() {
            @Override
            public void process(InputStream inputStream, OutputStream outputStream) throws IOException {
                ITesseract instance = new Tesseract1();
                instance.setLanguage(DEFAULT_LANG);
                instance.setDatapath(context.getProperty(TESSERACT_INSTALL_DIR).evaluateAttributeExpressions(flowFile).getValue());
                instance.setPageSegMode(Integer.parseInt(DEFAULT_PAGE_SEG_MODE));

                try {
                    BufferedImage imBuff = ImageIO.read(inputStream);
                    outputStream.write(instance.doOCR(imBuff).getBytes());
                } catch (Exception ex) {
                    getLogger().error(ex.getMessage());
                    session.transfer(flowFile, REL_FAILURE);
                }
            }
        });

        session.transfer(ff, REL_SUCCESS);
    }
}

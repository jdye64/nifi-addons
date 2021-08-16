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

package org.apache.nifi.processors.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
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

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Tags({"ocr", "tesseract", "image", "text"})
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@CapabilityDescription("Extracts text from images using Optical Character Recognition (OCR). The images are pulled from the incoming" +
        " Flowfile's content. Supported image types are TIFF, JPEG, GIF, PNG, BMP, and PDF. Any Flowfile that doesn't contain" +
        " a supported image type in its content body will be routed to the 'unsupported image format' relationship and no OCR will be performed." +
        " This processor uses Tesseract to perform its duties and part of that requires that a valid Tesseract data (Tessdata) directory" +
        " be specified in the 'Tessdata Directory' Property. This processor considers a valid Tessdata directory to be an existing directory on the" +
        " local NiFi instance that contains one or more files ending with the '.traineddata' extension. The list of supported languages" +
        " is built from the Tessdata directory configured by listing all files ending with '.traineddata' and considering those" +
        " Tesseract language models. You can create you own Tesseract language models and place them in your Tessedata directory" +
        " and the processor will display it in the dropdown list of languages available. All valid Tesseract configuration values" +
        " may be passed to this processor by use of the 'Tesseract configuration values' which accepts a comma separated list" +
        " of key=value pairs representing Tesseract configurations. 'Tesseract configuration values' is where all of your tuning" +
        " values can be passed in to help increase the accuracy of your OCR operations based on your expected input images." +
        " TesseractOCRProcessor only supports installations of Tesseract version 3.0 and greater.")
public class TesseractOCRProcessor extends AbstractProcessor {

    public static Set<String> SUPPORTED_LANGUAGES;
    private static final String TESS_LANG_EXTENSION = ".traineddata";
    //private static List<AllowableValue> PAGE_SEGMENTATION_MODES;        //NOTE: changed from List<AllowableValue> to Set<Strin> to work with NiFi 0.6.1
    private static Set<String> PAGE_SEGMENTATION_MODES;
    private volatile ITesseract tesseract;
    private static final  List<PropertyDescriptor> descriptors;
    private static final Set<Relationship> relationships;

    static {
        SUPPORTED_LANGUAGES = new HashSet<>();
        SUPPORTED_LANGUAGES.add("eng"); //Since this is the default value we need to ensure it is present in the allowableValues.

        //NOTE: this works with the latest version of NiFi but changed to work with older NiFi 0.6.1
//        PAGE_SEGMENTATION_MODES = new ArrayList<AllowableValue>();
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("0","0 = Orientation and script detection (OSD) only"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("1","1 = Automatic page segmentation with OSD"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("2","2 = Automatic page segmentation, but no OSD, or OCR"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("3","3 = Fully automatic page segmentation, but no OSD"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("4","4 = Assume a single column of text of variable sizes"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("5","5 = Assume a single uniform block of vertically aligned text"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("6","6 = Assume a single uniform block of text"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("7","7 = Treat the image as a single text line"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("8","8 = Treat the image as a single word"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("9","9 = Treat the image as a single word in a circle"));
//        PAGE_SEGMENTATION_MODES.add(new AllowableValue("10","10 = Treat the image as a single character"));

        PAGE_SEGMENTATION_MODES = new HashSet<>();
        PAGE_SEGMENTATION_MODES.add("0 = Orientation and script detection (OSD) only");
        PAGE_SEGMENTATION_MODES.add("1 = Automatic page segmentation with OSD");
        PAGE_SEGMENTATION_MODES.add("2 = Automatic page segmentation, but no OSD, or OCR");
        PAGE_SEGMENTATION_MODES.add("3 = Fully automatic page segmentation, but no OSD");
        PAGE_SEGMENTATION_MODES.add("4 = Assume a single column of text of variable sizes");
        PAGE_SEGMENTATION_MODES.add("5 = Assume a single uniform block of vertically aligned text");
        PAGE_SEGMENTATION_MODES.add("6 = Assume a single uniform block of text");
        PAGE_SEGMENTATION_MODES.add("7 = Treat the image as a single text line");
        PAGE_SEGMENTATION_MODES.add("8 = Treat the image as a single word");
        PAGE_SEGMENTATION_MODES.add("9 = Treat the image as a single word in a circle");
        PAGE_SEGMENTATION_MODES.add("10 = Treat the image as a single character");
    }

    public static final PropertyDescriptor TESS_DATA_PATH = new PropertyDescriptor
            .Builder().name("Tessdata Directory")
            .description("Directory on the local NiFi instance where the Tesseract languages and configurations are installed.")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("/usr/local/Cellar/tesseract/3.04.00/share/tessdata")
            .addValidator(StandardValidators.createDirectoryExistsValidator(true, false))
            .addValidator(new TessdataDirectoryValidator())
            .build();

    /**
     * Validates the TessData directory by ensuring that the specified directory exists and also that at least
     * once language is present. A language file ends with TESS_LANG_EXTENSION
     */
    public static class TessdataDirectoryValidator implements Validator {

        @Override
        public ValidationResult validate(final String subject, final String value, final ValidationContext context) {
            if (context.isExpressionLanguageSupported(subject) && context.isExpressionLanguagePresent(value)) {
                return new ValidationResult.Builder()
                        .subject(subject).input(value).explanation("Expression Language Present").valid(true).build();
            }

            String reason = null;
            try {
                //There must be lanauges present to ensure the Tessdata directory is valid.
                File[] languages = getTesseractLanguages(value);
                if (languages == null || languages.length == 0) {
                    reason =  "No valid languages found in directory. Languages end with '" + TESS_LANG_EXTENSION + "'";
                }
            } catch (final Exception e) {
                reason = "Value is not a valid directory name";
            }

            return new ValidationResult.Builder().subject(subject).input(value).explanation(reason).valid(reason == null).build();
        }
    }

    public static final PropertyDescriptor TESSERACT_LANGUAGE = new PropertyDescriptor
            .Builder().name("Tesseract Language")
            .description("Language that Tesseract will use to perform OCR on image coming in the incoming FlowFile's content")
            .required(true)
            //.defaultValue(SUPPORTED_LANGUAGES.iterator().next())
            .defaultValue("eng")
            .allowableValues(SUPPORTED_LANGUAGES)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor TESSERACT_PAGE_SEG_MODE = new PropertyDescriptor
            .Builder().name("Tesseract Page Segmentation Mode")
            .description("Set Tesseract to only run a subset of layout analysis and assume a certain form of image.")
            .required(true)
            //.defaultValue(PAGE_SEGMENTATION_MODES.get(3).getValue()) //Removed to work with NiFi 0.6.1
            .defaultValue("3 = Fully automatic page segmentation, but no OSD")
            .allowableValues(PAGE_SEGMENTATION_MODES)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor TESSERACT_CONFIGS = new PropertyDescriptor
            .Builder().name("Tesseract configuration values")
            .description("Comma separated list of key=value pairs that will be used to configure the Tesseract instance." +
                    " If a Tesseract configuration file is specified that will take precedence over these configurations. Values" +
                    " placed into this property will not be validated so take care to pass only valid Tesseract configuration values." +
                    " EX: textord_min_linesize=3.25,tessedit_write_images=true")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("successfully completed OCR on image")
            .build();

    public static final Relationship REL_UNSUPPORTED_IMAGE_FORMAT = new Relationship.Builder()
            .name("unsupported image format")
            .description("The image format in the FlowFile content is not supported by Tesseract")
            .build();

    public static final Relationship REL_ORIGINAL = new Relationship.Builder()
            .name("original")
            .description("The original image that OCR was performed on")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Failed to attempt OCR on input image")
            .build();

    static {
        final List<PropertyDescriptor> _descriptors = new ArrayList<>();
        _descriptors.add(TESS_DATA_PATH);
        _descriptors.add(TESSERACT_LANGUAGE);
        _descriptors.add(TESSERACT_PAGE_SEG_MODE);
        _descriptors.add(TESSERACT_CONFIGS);
        descriptors = Collections.unmodifiableList(_descriptors);

        final Set<Relationship> _relationships = new HashSet<>();
        _relationships.add(REL_SUCCESS);
        _relationships.add(REL_FAILURE);
        _relationships.add(REL_UNSUPPORTED_IMAGE_FORMAT);
        _relationships.add(REL_ORIGINAL);
        relationships = Collections.unmodifiableSet(_relationships);
    }


    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {

        List<PropertyDescriptor> descriptorsNew = new ArrayList<>();

        descriptorsNew.add(TESS_DATA_PATH);
        descriptorsNew.add(TESSERACT_PAGE_SEG_MODE);
        descriptorsNew.add(TESSERACT_CONFIGS);
        descriptorsNew.add(new PropertyDescriptor.Builder()
                .fromPropertyDescriptor(TESSERACT_LANGUAGE)
                .allowableValues(SUPPORTED_LANGUAGES)
                .build());

        return descriptorsNew;
    }

    @Override
    public void onPropertyModified(PropertyDescriptor descriptor, String oldValue, String newValue) {
        super.onPropertyModified(descriptor, oldValue, newValue);

        if (descriptor.equals(TESS_DATA_PATH)) {
            getLogger().debug("Tesseract Install path was changed. Building list of supported languages");
            SUPPORTED_LANGUAGES.clear();
            SUPPORTED_LANGUAGES.add("eng");
            //File will always exist since the Validator will take care of that.
            File[] files = getTesseractLanguages(newValue);

            //Guard against creating an empty list of allowable values in case the user points to an invalid directory
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Found Tesseract supported language: " + files[i].getName());
                    }
                    SUPPORTED_LANGUAGES.add(StringUtils.split(files[i].getName(), ".")[0]);
                }
            } else {
                getLogger().debug("No languages found in user specified Tessdata directory: '" + newValue + "'");
            }

        }
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        //Setup the Tesseract instance once the processor is scheuled
        tesseract = new Tesseract();
        tesseract.setLanguage(context.getProperty(TESSERACT_LANGUAGE).getValue());
        //tesseract.setPageSegMode((context.getProperty(TESSERACT_PAGE_SEG_MODE).asInteger()));
        tesseract.setPageSegMode(3);
        //tesseract.setPageSegMode(7); this works for the powermeter but not cat in the hat
    }



    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        //Transfer the original
        session.transfer(session.clone(flowFile), REL_ORIGINAL);
        final AtomicBoolean errors = new AtomicBoolean(false);

        FlowFile ff = session.write(flowFile, new StreamCallback() {
            @Override
            public void process(InputStream inputStream, OutputStream outputStream) throws IOException {
                tesseract.setDatapath(context.getProperty(TESS_DATA_PATH).evaluateAttributeExpressions(flowFile).getValue());

                //Builds the list of Tesseract configs.
                Map<String, String> configs = buildTesseractConfigs(context.getProperty(TESSERACT_CONFIGS).getValue());
                for (Map.Entry<String, String> entry : configs.entrySet()) {
                    getLogger().debug("Tesseract Config Key : '" + entry.getKey()
                            + "' Tesseract Config Value : '" + entry.getValue() + "'");
                    tesseract.setTessVariable(entry.getKey(), entry.getValue());
                }

                try {
                    BufferedImage imBuff = ImageIO.read(inputStream);
                    outputStream.write(tesseract.doOCR(imBuff).getBytes());
                } catch (TesseractException te) {
                    getLogger().error(te.getMessage());
                    if (te.getCause().getMessage().equals("image == null!")) {
                        session.transfer(flowFile, REL_UNSUPPORTED_IMAGE_FORMAT);
                    } else {
                        session.transfer(session.penalize(flowFile), REL_FAILURE);
                    }
                    errors.set(true);

                } catch (Exception ex) {
                    getLogger().error(ex.getMessage());
                    session.transfer(flowFile, REL_FAILURE);
                    errors.set(true);
                }
            }
        });

        if (!errors.get()) {
            session.transfer(ff, REL_SUCCESS);
        }

    }



    /**
     * Build the key/value pairs of Tesseract configuration values that will be passed to Tesseract.
     *
     * @param commaDelimitedConfigs
     *  Comma separated list of key=value Tesseract configuration pairs.
     *
     * @return
     *  Map of key/value pairs that were parsed from the incoming commaDelimtedConfigs string.
     */
    private Map<String, String> buildTesseractConfigs(String commaDelimitedConfigs) {
        Map<String, String> configs = new HashMap<>();
        if (!StringUtils.isEmpty(commaDelimitedConfigs)) {
            String[] keyValuePairs = StringUtils.split(commaDelimitedConfigs, ",");
            if (keyValuePairs != null && keyValuePairs.length > 0) {
                for (String kp : keyValuePairs) {
                    String[] keyValue = StringUtils.split(kp, "=");
                    configs.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return configs;
    }

    /**
     * Traverses the TessData directory and locates all of the installed languages so they may be
     * presented to the user as a list of allowableValues.
     *
     * @param tessDataPath
     *  PropertyDescriptor String value described the path of the TessData directory.
     *
     * @return
     *  Array of File references to the Tesseract language files.
     */
    public static File[] getTesseractLanguages(String tessDataPath) {
        //File will always exist since the Validator will take care of that.
        File installFile = new File(tessDataPath);
        File[] files = installFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(TESS_LANG_EXTENSION);
            }
        });
        return files;
    }
}
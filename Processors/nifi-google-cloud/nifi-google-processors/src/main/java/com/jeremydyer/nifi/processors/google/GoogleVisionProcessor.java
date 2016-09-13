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
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.common.collect.ImmutableList;

@Tags({"Google", "Vision", "face detection", "landmark detection", "ocr"})
public class GoogleVisionProcessor
        extends AbstractProcessor {

    private static final String LABEL_DETECTION_VAL = "LABEL_DETECTION";
    private static final String TEXT_DETECTION_VAL = "TEXT_DETECTION";
    private static final String SAFE_SEARCH_DETECTION_VAL = "SAFE_SEARCH_DETECTION";
    private static final String FACE_DETECTION_VAL = "FACE_DETECTION";
    private static final String LANDMARK_DETECTION_VAL = "LANDMARK_DETECTION";
    private static final String LOGO_DETECTION_VAL = "LOGO_DETECTION";
    private static final String IMAGE_PROPERTIES_VAL = "IMAGE_PROPERTIES";

    public static final PropertyDescriptor APP_NAME = new PropertyDescriptor
            .Builder().name("Google Application name")
            .description("Google application name that will passed along as part of the API request")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MAX_RESULTS = new PropertyDescriptor
            .Builder().name("Maximum results")
            .description("Maximum number of EntityAnnotations that will be returned from the API request")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("10")
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
            .build();

    public static final PropertyDescriptor DETECT_LABELS = new PropertyDescriptor
            .Builder().name("Labels Detection")
            .description("Add labels based on image content")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PERFORM_OCR = new PropertyDescriptor
            .Builder().name("Perform OCR")
            .description("Perform Optical Character Recognition (OCR) on text within the image")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor EXPLICIT_CONTENT_DETECTION = new PropertyDescriptor
            .Builder().name("Explicit Content Detection")
            .description("Determine image safe search properties on the image")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor FACIAL_DETECTION = new PropertyDescriptor
            .Builder().name("Facial Detection")
            .description("Detect faces within the image")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor LANDMARK_DETECTION = new PropertyDescriptor
            .Builder().name("Landmark Detection")
            .description("Detect geographic landmarks within the image")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor LOGO_DETECTION = new PropertyDescriptor
            .Builder().name("Logo Detection")
            .description("Detect company logos within the image")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor IMAGE_PROPERTIES = new PropertyDescriptor
            .Builder().name("Image Properties")
            .description("Compute a set of properties about the image (such as the image's dominant colors)")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("vision api response was received")
            .build();

    public static final Relationship REL_ERROR = new Relationship.Builder()
            .name("failure")
            .description("error was encountered and no google vision api response was received")
            .build();

    public static final Relationship REL_NO_DETECTIONS = new Relationship.Builder()
            .name("no detections")
            .description("no detections were found in the image")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;


    private Vision vision = null;
    private List<Feature> featuresList = new ArrayList<>();


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(APP_NAME);
        descriptors.add(MAX_RESULTS);
        descriptors.add(DETECT_LABELS);
        descriptors.add(PERFORM_OCR);
        descriptors.add(EXPLICIT_CONTENT_DETECTION);
        descriptors.add(FACIAL_DETECTION);
        descriptors.add(LANDMARK_DETECTION);
        descriptors.add(LOGO_DETECTION);
        descriptors.add(IMAGE_PROPERTIES);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_ERROR);
        relationships.add(REL_NO_DETECTIONS);
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
        this.vision = getVisionService(context);

        ArrayList<Feature> fs = new ArrayList<>();
        if (context.getProperty(DETECT_LABELS).asBoolean()) {
            fs.add(new Feature().setType(LABEL_DETECTION_VAL).setMaxResults(context.getProperty(MAX_RESULTS).evaluateAttributeExpressions().asInteger()));
        }
        if (context.getProperty(PERFORM_OCR).asBoolean()) {
            fs.add(new Feature().setType(TEXT_DETECTION_VAL).setMaxResults(context.getProperty(MAX_RESULTS).evaluateAttributeExpressions().asInteger()));
        }
        if (context.getProperty(EXPLICIT_CONTENT_DETECTION).asBoolean()) {
            fs.add(new Feature().setType(SAFE_SEARCH_DETECTION_VAL).setMaxResults(context.getProperty(MAX_RESULTS).evaluateAttributeExpressions().asInteger()));
        }
        if (context.getProperty(FACIAL_DETECTION).asBoolean()) {
            fs.add(new Feature().setType(FACE_DETECTION_VAL).setMaxResults(context.getProperty(MAX_RESULTS).evaluateAttributeExpressions().asInteger()));
        }
        if (context.getProperty(LANDMARK_DETECTION).asBoolean()) {
            fs.add(new Feature().setType(LANDMARK_DETECTION_VAL).setMaxResults(context.getProperty(MAX_RESULTS).evaluateAttributeExpressions().asInteger()));
        }
        if (context.getProperty(LOGO_DETECTION).asBoolean()) {
            fs.add(new Feature().setType(LOGO_DETECTION_VAL).setMaxResults(context.getProperty(MAX_RESULTS).evaluateAttributeExpressions().asInteger()));
        }
        if (context.getProperty(IMAGE_PROPERTIES).asBoolean()) {
            fs.add(new Feature().setType(IMAGE_PROPERTIES_VAL).setMaxResults(context.getProperty(MAX_RESULTS).evaluateAttributeExpressions().asInteger()));
        }
        this.featuresList = fs;
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
                            .setFeatures(this.featuresList);

            Vision.Images.Annotate annotate =
                    vision.images()
                            .annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));


            BatchAnnotateImagesResponse batchResponse = annotate.execute();

            if (batchResponse.getResponses() != null
                    && batchResponse.getResponses().size() > 0) {

                AnnotateImageResponse response = batchResponse.getResponses().get(0);

                flowFile = session.putAttribute(flowFile, "AnnotationResponseSize", new Integer(batchResponse.getResponses().size()).toString());

                if (context.getProperty(DETECT_LABELS).asBoolean()) {
                    if (response.getLabelAnnotations() != null
                            && response.getLabelAnnotations().size() > 0) {
                        flowFile = session.putAttribute(flowFile, LABEL_DETECTION_VAL, response.getLabelAnnotations().toString());
                    }
                }
                if (context.getProperty(PERFORM_OCR).asBoolean()) {
                    if (response.getTextAnnotations() != null
                            && response.getTextAnnotations().size() > 0) {
                        flowFile = session.putAttribute(flowFile, TEXT_DETECTION_VAL, response.getTextAnnotations().toString());
                    }
                }
                if (context.getProperty(EXPLICIT_CONTENT_DETECTION).asBoolean()) {
                    if (response.getSafeSearchAnnotation() != null
                            && response.getSafeSearchAnnotation().size() > 0) {
                        flowFile = session.putAttribute(flowFile, SAFE_SEARCH_DETECTION_VAL, response.getSafeSearchAnnotation().toString());
                    }
                }
                if (context.getProperty(FACIAL_DETECTION).asBoolean()) {
                    if (response.getFaceAnnotations() != null
                            && response.getFaceAnnotations().size() > 0) {
                        flowFile = session.putAttribute(flowFile, FACE_DETECTION_VAL, response.getFaceAnnotations().toString());
                    }
                }
                if (context.getProperty(LANDMARK_DETECTION).asBoolean()) {
                    if (response.getLandmarkAnnotations() != null
                            && response.getLandmarkAnnotations().size() > 0) {
                        flowFile = session.putAttribute(flowFile, LANDMARK_DETECTION_VAL, response.getLandmarkAnnotations().toString());
                    }
                }
                if (context.getProperty(LOGO_DETECTION).asBoolean()) {
                    if (response.getLogoAnnotations() != null
                            && response.getLogoAnnotations().size() > 0) {
                        flowFile = session.putAttribute(flowFile, LOGO_DETECTION_VAL, response.getLogoAnnotations().toString());
                    }
                }
                if (context.getProperty(IMAGE_PROPERTIES).asBoolean()) {
                    if (response.getImagePropertiesAnnotation() != null
                            && response.getImagePropertiesAnnotation().size() > 0) {
                        flowFile = session.putAttribute(flowFile, IMAGE_PROPERTIES_VAL, response.getImagePropertiesAnnotation().toString());
                    }
                }

                session.transfer(flowFile, REL_SUCCESS);

            } else {
                session.transfer(flowFile, REL_NO_DETECTIONS);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            session.transfer(flowFile, REL_ERROR);
        }

    }

    /**
     * Connects to the Vision API using Application Default Credentials.
     */
    public static Vision getVisionService(ProcessContext context) throws IOException, GeneralSecurityException {
        GoogleCredential credential =
                GoogleCredential.getApplicationDefault().createScoped(VisionScopes.all());
        
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        return new Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
                .setApplicationName(context.getProperty(APP_NAME).evaluateAttributeExpressions().getValue())
                .build();
    }

}

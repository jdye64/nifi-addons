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
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.core.*;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Tags({"opencv, object detection"})
@CapabilityDescription("Detects objects from the input images based on the configured OpenCV CascadeClassifier loaded." +
        "This Processor REQUIRES the OpenCV native Java bindings be installed on the NiFi instance")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes(
        {
                @WritesAttribute(attribute="object.detection.name", description=""),
                @WritesAttribute(attribute="object.detection.id", description="")
        }
)
public class ObjectDetectionProcessor extends AbstractProcessor {

    public static final PropertyDescriptor DETECTION_DEFINITION_JSON = new PropertyDescriptor
            .Builder().name("Object Detection Definition")
            .description("JSON detection definition")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_ORIGINAL = new Relationship.Builder()
            .name("original")
            .description("original input")
            .build();

    public static final Relationship REL_OBJECT_DETECTED = new Relationship.Builder()
            .name("object_detected")
            .description("object detected in image")
            .build();

    public static final Relationship REL_NO_OBJECT_DETECTED = new Relationship.Builder()
            .name("no_object_detected")
            .description("no desired object detected in the image")
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
        descriptors.add(DETECTION_DEFINITION_JSON);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_ORIGINAL);
        relationships.add(REL_OBJECT_DETECTED);
        relationships.add(REL_NO_OBJECT_DETECTED);
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

        final JSONObject JSON = new JSONObject(context.getProperty(DETECTION_DEFINITION_JSON).getValue());

        session.read(original, new InputStreamCallback() {
            @Override
            public void process(InputStream inputStream) throws IOException {

                try {
                    byte[] imgData = IOUtils.toByteArray(inputStream);
                    Mat image = Imgcodecs.imdecode(new MatOfByte(imgData), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

                    //Loops through all of the detection definitions
                    JSONArray dds = JSON.getJSONArray("DetectionDefinition");

                    for (int i = 0; i < dds.length(); i++) {
                        JSONObject dd = dds.getJSONObject(i);
                        detectObjects(session, original, dd, image);
                    }

                } catch (Exception ex) {
                    getLogger().error(ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        session.transfer(original, REL_ORIGINAL);

    }

    final public Mat detectObjects(final ProcessSession session, FlowFile original, final JSONObject dd, final Mat image) {

        CascadeClassifier objectDetector = new CascadeClassifier(dd.getString("opencv_xml_cascade_path"));
        MatOfRect objectDetections = new MatOfRect();
        objectDetector.detectMultiScale(image, objectDetections);
        //getLogger().error("Detected " + objectDetections.toArray().length + " " + dd.getString("name") + " objects in the input flowfile");

        final AtomicReference<Mat> croppedImageReference = new AtomicReference<>();

        int counter = 0;
        for (int i = 0; i < objectDetections.toArray().length; i++) {
            final Rect rect = objectDetections.toArray()[i];
            FlowFile detection = session.write(session.create(original), new OutputStreamCallback() {
                @Override
                public void process(OutputStream outputStream) throws IOException {

                    Mat croppedImage = null;

                    //Should the image be cropped? If so there is no need to draw bounds because that would be the same as the cropping
                    if (dd.getBoolean("crop")) {
                        Rect rectCrop = new Rect(rect.x, rect.y, rect.width, rect.height);
                        croppedImage = new Mat(image, rectCrop);
                        MatOfByte updatedImage = new MatOfByte();
                        Imgcodecs.imencode(".jpg", croppedImage, updatedImage);
                        croppedImageReference.set(croppedImage);
                        outputStream.write(updatedImage.toArray());
                    } else {
                        //Should the image have a border drawn around it?
                        if (dd.getBoolean("drawBounds")) {
                            Mat imageWithBorder = image.clone();
                            Imgproc.rectangle(imageWithBorder, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 255, 255));
                            MatOfByte updatedImage = new MatOfByte();
                            Imgcodecs.imencode(".jpg", imageWithBorder, updatedImage);
                            outputStream.write(updatedImage.toArray());
                        } else {
                            MatOfByte updatedImage = new MatOfByte();
                            Imgcodecs.imencode(".jpg", image, updatedImage);
                            outputStream.write(updatedImage.toArray());
                        }
                    }

                }
            });

            Map<String, String> atts = new HashMap<>();
            atts.put("object.detection.name", dd.getString("name"));
            atts.put("object.detection.id", new Long(System.currentTimeMillis() + counter).toString());

            counter++;

            detection = session.putAllAttributes(detection, atts);
            session.transfer(detection, REL_OBJECT_DETECTED);
        }

        Mat childResponse = null;

        if (croppedImageReference.get() != null) {
            childResponse = croppedImageReference.get();
        } else {
            childResponse = image;
        }

        if (dd.has("children")) {
            JSONArray children = dd.getJSONArray("children");
            if (children != null) {

                for (int i = 0; i < children.length(); i++) {
                    JSONObject ddd = children.getJSONObject(i);
                    childResponse = detectObjects(session, original, ddd, childResponse);
                }
            }
        }

        return childResponse;
    }
}

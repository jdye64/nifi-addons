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
package com.github.jdye64.processors.tensorflow;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
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
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

@Tags({"tensorflow", "label", "image"})
@CapabilityDescription("Labels incoming images using Tensorflow")
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
public class LabelImageProcessor
        extends AbstractProcessor {

    // Tensorflow
    private byte[] graphDef = null;             // TensorFlow frozen graph file bytes. AKA .pb file
    private List<String> labels = null;         // TensorFlow labels loaded from correlated .txt file for .pb file.


    public static final PropertyDescriptor TF_FROZEN_GRAPH = new PropertyDescriptor
            .Builder().name("tensorflow-pb-file")
            .displayName("TensorFlow pb file")
            .description("TensorFlow frozen graph model that will be used for image labeling")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor TF_LABELS_FILE = new PropertyDescriptor
            .Builder().name("tensorflow-labels-file")
            .displayName("TensorFlow labels file")
            .description("File that contains the labels that correlate to the TensorFlow frozen graph (.pb) file")
            .expressionLanguageSupported(true)
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder().name("success")
            .description("successfully labeled image").build();

    public static final Relationship REL_FAILURE = new Relationship.Builder().name("failure")
            .description("error occurred while attempting to label image").build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(TF_FROZEN_GRAPH);
        descriptors.add(TF_LABELS_FILE);
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

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        this.graphDef = readAllBytes(Paths.get(context.getProperty(TF_FROZEN_GRAPH).evaluateAttributeExpressions().getValue()));

        // Make sure the graph bytes were read otherwise invalidate this processor
        if (graphDef == null) {
            getLogger().warn("TensorFlow file was not found. This processor is invalid!");
        }

        this.labels = readAllLines(Paths.get(context.getProperty(TF_LABELS_FILE).evaluateAttributeExpressions().getValue()));
        if (this.labels == null) {
            getLogger().warn("TensorFlow labels file was not found or unable to read. This processor is now invalid!");
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            getLogger().warn(this.getClass().getName() + " requires input for processing");
            return;
        }

        try {

            AtomicReference<Boolean> error = new AtomicReference<>();
            error.set(Boolean.TRUE);

            flowFile = session.write(flowFile, new StreamCallback() {
                @Override
                public void process(InputStream inputStream, OutputStream outputStream) throws IOException {
                    byte[] imageBytes = IOUtils.toByteArray(inputStream);

                    try (Tensor image = constructAndExecuteGraphToNormalizeImage(imageBytes)) {
                        float[] labelProbabilities = executeInceptionGraph(graphDef, image);
                        int bestLabelIdx = maxIndex(labelProbabilities);
                        String output = String.format("BEST MATCH: %s (%.2f%% likely)", labels.get(bestLabelIdx), labelProbabilities[bestLabelIdx] * 100f);
                        outputStream.write(output.getBytes());
                        error.set(Boolean.FALSE);
                    }
                    catch(Exception ex) {
                        getLogger().error(ex.getMessage(), ex);
                    }
                }
            });

            if (error.get()) {
                session.transfer(flowFile, REL_FAILURE);
            } else {
                session.transfer(flowFile, REL_SUCCESS);
            }

        } catch (final Throwable t) {
            getLogger().error("Unable to process TensorFlow Processor file " + t.getLocalizedMessage());
            getLogger().error("{} failed to process due to {}; rolling back session", new Object[] { this, t });
            throw t;
        }
    }


    private byte[] readAllBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            getLogger().error("Failed to read [" + path + "]: " + e.getMessage());
        }
        return null;
    }

    private List<String> readAllLines(Path path) {
        try {
            return Files.readAllLines(path, Charset.forName("UTF-8"));
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
        }
        return null;
    }

    // Translate the images to similar dimensions to those that the images were trained on.
    private Tensor constructAndExecuteGraphToNormalizeImage(byte[] imageBytes) {
        try (Graph g = new Graph()) {
            GraphBuilder b = new GraphBuilder(g);
            // Some constants specific to the pre-trained model at:
            // https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip
            //
            // - The model was trained with images scaled to 224x224 pixels.
            // - The colors, represented as R, G, B in 1-byte each were converted to
            //   float using (value - Mean)/Scale.
            final int H = 224;
            final int W = 224;
            final float mean = 117f;
            final float scale = 1f;

            // Since the graph is being constructed once per execution here, we can use a constant for the
            // input image. If the graph were to be re-used for multiple input images, a placeholder would
            // have been more appropriate.
            final Output input = b.constant("input", imageBytes);
            final Output output =
                    b.div(
                            b.sub(
                                    b.resizeBilinear(
                                            b.expandDims(
                                                    b.cast(b.decodeJpeg(input, 3), DataType.FLOAT),
                                                    b.constant("make_batch", 0)),
                                            b.constant("size", new int[] {H, W})),
                                    b.constant("mean", mean)),
                            b.constant("scale", scale));
            try (Session s = new Session(g)) {
                return s.runner().fetch(output).run().get(0);
            }
        }
    }

    private float[] executeInceptionGraph(byte[] graphDef, Tensor image) {
        try (Graph g = new Graph()) {
            g.importGraphDef(graphDef);
            try (Session s = new Session(g)) {
                
                Tensor result = s.runner().fetch("final_result:0").run().get(0);
                final long[] rshape = result.shape();
                if (result.numDimensions() != 2 || rshape[0] != 1) {
                    throw new RuntimeException(
                            String.format(
                                    "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                                    Arrays.toString(rshape)));
                }
                int nlabels = (int) rshape[1];
                return result.copyTo(new float[1][nlabels])[0];
            }
        }
    }

    private int maxIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
            if (probabilities[i] > probabilities[best]) {
                best = i;
            }
        }
        return best;
    }

    // In the fullness of time, equivalents of the methods of this class should be auto-generated from
    // the OpDefs linked into libtensorflow_jni.so. That would match what is done in other languages
    // like Python, C++ and Go.
    static class GraphBuilder {
        GraphBuilder(Graph g) {
            this.g = g;
        }

        Output div(Output x, Output y) {
            return binaryOp("Div", x, y);
        }

        Output sub(Output x, Output y) {
            return binaryOp("Sub", x, y);
        }

        Output resizeBilinear(Output images, Output size) {
            return binaryOp("ResizeBilinear", images, size);
        }

        Output expandDims(Output input, Output dim) {
            return binaryOp("ExpandDims", input, dim);
        }

        Output cast(Output value, DataType dtype) {
            return g.opBuilder("Cast", "Cast").addInput(value).setAttr("DstT", dtype).build().output(0);
        }

        Output decodeJpeg(Output contents, long channels) {
            return g.opBuilder("DecodeJpeg", "DecodeJpeg")
                    .addInput(contents)
                    .setAttr("channels", channels)
                    .build()
                    .output(0);
        }

        Output constant(String name, Object value) {
            try (Tensor t = Tensor.create(value)) {
                return g.opBuilder("Const", name)
                        .setAttr("dtype", t.dataType())
                        .setAttr("value", t)
                        .build()
                        .output(0);
            }
        }

        private Output binaryOp(String type, Output in1, Output in2) {
            return g.opBuilder(type, type).addInput(in1).addInput(in2).build().output(0);
        }

        private Graph g;
    }
}

package com.jeremydyer.processors.nvidia;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.jeremydyer.nifi.nvidia.AnacondaEnvironment;
import com.jeremydyer.nifi.nvidia.NvidiaGPU;
import com.jeremydyer.nifi.nvidia.ProcessResult;
import org.apache.nifi.annotation.behavior.*;
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
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;


@Tags({"anaconda", "python", "cudf"})
@CapabilityDescription("Invoke a Python script in a desired Anaconda environment")
@InputRequirement(InputRequirement.Requirement.INPUT_ALLOWED)
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class AnacondaPythonProcessor
        extends AbstractProcessor {

    public static final PropertyDescriptor ANACONDA_CONTROLLER = new PropertyDescriptor
            .Builder().name("Anaconda Controller Service")
            .identifiesControllerService(AnacondaEnvironment.class)
            .description("Provides an Anaconda environment controller service for executing Python scripts")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor GPU_SERVICE = new PropertyDescriptor
            .Builder().name("Service for handling which GPU is used")
            .identifiesControllerService(NvidiaGPU.class)
            .description("Controls the GPUs that are used for executing the Python script")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PYTHON = new PropertyDescriptor
            .Builder().name("Python")
            .description("Python that should be ran in the Anaconda environment")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Python script completed with exit code 0")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Python script ended with non 0 exit code")
            .build();


    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        this.descriptors = List.of(ANACONDA_CONTROLLER, PYTHON);
        this.relationships = Set.of(REL_SUCCESS, REL_FAILURE);
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
        AnacondaEnvironment anacondaEnvironment = context.getProperty(ANACONDA_CONTROLLER).asControllerService(AnacondaEnvironment.class);
        AtomicInteger exitCode = new AtomicInteger();
        FlowFile ff = session.write(session.create(), new OutputStreamCallback() {
            @Override
            public void process(OutputStream outputStream) throws IOException {
                ProcessResult pr = anacondaEnvironment.runPython(context.getProperty(PYTHON).getValue());
                exitCode.set(pr.getExitCode());
                outputStream.write(pr.getProcessOutput().getBytes(StandardCharsets.UTF_8));
            }
        });
        if (exitCode.get() > 0) {
            session.transfer(ff, REL_FAILURE);
        } else {
            session.transfer(ff, REL_SUCCESS);
        }
    }

}
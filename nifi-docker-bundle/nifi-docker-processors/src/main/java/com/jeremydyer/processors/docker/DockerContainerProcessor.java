package com.jeremydyer.processors.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.*;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.*;

@Tags({"docker", "container", "create", "list", "start"})
@CapabilityDescription("Processor for interacting with Docker containers on the specified Docker daemon instance.")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class DockerContainerProcessor
    extends AbstractDockerProcessor {

    private static final String CONTAINER_LIST_OP = "list";
    private static final String CONTAINER_CREATE_OP = "create";
    private static final String CONTAINER_START_OP = "start";
    private static final String CONTAINER_INSPECT_OP = "inspect";

    static final PropertyDescriptor DOCKER_CONTAINER_OP = new PropertyDescriptor.Builder()
            .name("Docker Container Operation")
            .description("The Docker Container operation that this processor is configured to perform")
            .required(true)
            .defaultValue(CONTAINER_LIST_OP)
            .allowableValues(CONTAINER_LIST_OP, CONTAINER_CREATE_OP, CONTAINER_START_OP, CONTAINER_INSPECT_OP)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    static final PropertyDescriptor DOCKER_DAEMON_URI = new PropertyDescriptor.Builder()
            .name("Docker Daemon URI")
            .description("URI for connecting to the Docker daemon")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("http://192.168.99.100:2375")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    static final PropertyDescriptor DOCKER_BINDING_PORTS = new PropertyDescriptor.Builder()
            .name("Docker binding ports")
            .description("Coma separated list of ports that the container when ran")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("80")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    static final PropertyDescriptor DOCKER_ENV_LIST = new PropertyDescriptor.Builder()
            .name("Docker Environment Variables")
            .description("Coma separated list of environment variables that should be passed the Docker container when starting it")
            .required(false)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    static final PropertyDescriptor DOCKER_IMAGE_TO_START = new PropertyDescriptor.Builder()
            .name("Docker Run Image")
            .description("When the conditions are met that determine the NiFi Cluster is under load this is the new Docker Image that will be started to help out")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("nginx")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    static final PropertyDescriptor DOCKER_MAX_ASSIST_CONTAINERS = new PropertyDescriptor.Builder()
            .name("Docker Max Assist Containers")
            .description("The maximum number of Docker containers that should be started by this ReportingTask" +
                    " to assist with computing")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("1")
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();


    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("successfully performed speech to text")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("failure encountered while attempting speech to text")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(DOCKER_CONTAINER_OP);
        descriptors.add(DOCKER_DAEMON_URI);
        descriptors.add(DOCKER_BINDING_PORTS);
        descriptors.add(DOCKER_ENV_LIST);
        descriptors.add(DOCKER_IMAGE_TO_START);
        descriptors.add(DOCKER_MAX_ASSIST_CONTAINERS);
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

        String mode = context.getProperty(DOCKER_CONTAINER_OP).evaluateAttributeExpressions().getValue();

        final DockerClient docker = DefaultDockerClient.builder()
                .uri(context.getProperty(DOCKER_DAEMON_URI).evaluateAttributeExpressions().getValue())
                .build();

        switch (mode) {
            case CONTAINER_LIST_OP:
                try {
                    FlowFile ff = session.write(session.create(), new StreamCallback() {
                        @Override
                        public void process(InputStream inputStream, OutputStream outputStream) throws IOException {

                            getLogger().info("Listing Docker containers");

                            try {
                                List<Container> containers = docker.listContainers();
                                ObjectMapper mapper = new ObjectMapper();
                                outputStream.write(mapper.writeValueAsBytes(containers));
                            } catch (DockerException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    session.transfer(ff, REL_SUCCESS);

                } catch (Exception ex) {
                    getLogger().error(ex.getMessage());
                    session.transfer(session.create(), REL_FAILURE);
                }
                break;
            case CONTAINER_CREATE_OP:
                try {
                    FlowFile ff = session.write(session.create(), new StreamCallback() {
                        @Override
                        public void process(InputStream inputStream, OutputStream outputStream) throws IOException {

                            // Bind container ports to host ports
                            final String[] ports = commaDelimitedToStringArray(context.getProperty(DOCKER_BINDING_PORTS).evaluateAttributeExpressions().getValue());
                            final Map<String, List<PortBinding>> portBindings = new HashMap<>();
                            for (String port : ports) {
                                List<PortBinding> hostPorts = new ArrayList<>();
                                hostPorts.add(PortBinding.of("0.0.0.0", port));
                                portBindings.put(port, hostPorts);
                            }

                            final HostConfig hostConfig = HostConfig.builder()
                                    .portBindings(portBindings)
                                    .build();

                            //Create a container
                            ContainerConfig cc = ContainerConfig.builder()
                                    .hostConfig(hostConfig)
                                    .image(context.getProperty(DOCKER_IMAGE_TO_START).evaluateAttributeExpressions().getValue())
                                    .exposedPorts(ports)
                                    .env(commaDelimitedToStringArray(context.getProperty(DOCKER_ENV_LIST).evaluateAttributeExpressions().getValue()))
                                    .build();

                            final ContainerCreation container;
                            try {
                                container = docker.createContainer(cc);
                                outputStream.write(container.id().getBytes());
                            } catch (DockerException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            getLogger().info("Created Docker container!");
                        }
                    });

                    session.transfer(ff, REL_SUCCESS);

                } catch (Exception ex) {
                    getLogger().error(ex.getMessage());
                    session.transfer(session.create(), REL_FAILURE);
                }
                break;
            case CONTAINER_START_OP:

                FlowFile flowFile = session.get();
                if (flowFile == null) {
                    getLogger().warn("Expecting incoming FlowFile and it was not present. Unable to continue");
                    return;
                }

                try {
                    session.read(flowFile, new InputStreamCallback() {
                        @Override
                        public void process(InputStream inputStream) throws IOException {
                            StringWriter writer = new StringWriter();
                            IOUtils.copy(inputStream, writer);
                            String cc = writer.toString();

                            try {
                                docker.startContainer(cc);
                            } catch (DockerException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            getLogger().info("Successfully started container: " + cc);
                        }
                    });

                    session.transfer(flowFile, REL_SUCCESS);

                } catch (Exception ex) {
                    getLogger().error(ex.getMessage());
                    session.transfer(flowFile, REL_FAILURE);
                }
                break;
            case CONTAINER_INSPECT_OP:

                flowFile = session.get();
                if (flowFile == null) {
                    getLogger().warn("Expecting incoming FlowFile and it was not present. Unable to continue");
                    return;
                }

                try {
                    FlowFile ff = session.write(flowFile, new StreamCallback() {
                        @Override
                        public void process(InputStream inputStream, OutputStream outputStream) throws IOException {
                            StringWriter writer = new StringWriter();
                            IOUtils.copy(inputStream, writer);
                            String cc = writer.toString();

                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                outputStream.write(mapper.writeValueAsBytes(docker.inspectContainer(cc)));
                            } catch (DockerException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            getLogger().info("Successfully started container: " + cc);
                        }
                    });

                    session.transfer(ff, REL_SUCCESS);

                } catch (Exception ex) {
                    getLogger().error(ex.getMessage());
                    session.transfer(flowFile, REL_FAILURE);
                }
                break;
            default:
                break;
        }

    }
}

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
package com.jeremydyer.reporting.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import org.apache.commons.lang.StringUtils;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.controller.status.ConnectionStatus;
import org.apache.nifi.controller.status.ProcessGroupStatus;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.AbstractReportingTask;
import org.apache.nifi.reporting.ReportingContext;

import java.io.IOException;
import java.util.*;


@Tags({"reporting", "docker", "metrics"})
@CapabilityDescription("Examines system load and deploys new Docker containers as needed")
public class DockerReportingTask extends AbstractReportingTask {

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
            .required(true)
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

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(DOCKER_DAEMON_URI);
        properties.add(DOCKER_BINDING_PORTS);
        properties.add(DOCKER_ENV_LIST);
        properties.add(DOCKER_IMAGE_TO_START);
        properties.add(DOCKER_MAX_ASSIST_CONTAINERS);
        return properties;
    }

    @OnScheduled
    public void setup(final ConfigurationContext context) throws IOException {
        final Map<String, ?> config = Collections.emptyMap();
    }


    private int containersStarted = 0;


    @Override
    public void onTrigger(final ReportingContext context) {

        final ProcessGroupStatus status = context.getEventAccess().getControllerStatus();

        getLogger().info("Running the DockerReportingTask");

        List<ConnectionStatus> busyConnections = searchForBusyConnections(status);

        getLogger().info(busyConnections.size() + " busy connections were found in the current Controller");

        for (ConnectionStatus cs : busyConnections) {
            getLogger().info("\tBusy Connection: " + cs.getName());
        }

        if (startNewDockerContainer(busyConnections)) {

            try {

                int maxAssistContainers = context.getProperty(DOCKER_MAX_ASSIST_CONTAINERS).evaluateAttributeExpressions().asInteger();
                if (containersStarted < maxAssistContainers) {
                    final DockerClient docker = DefaultDockerClient.builder()
                            .uri(context.getProperty(DOCKER_DAEMON_URI).evaluateAttributeExpressions().getValue())
                            .build();

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

                    final ContainerCreation container = docker.createContainer(cc);

                    docker.startContainer(container.id());
                    containersStarted++;
                    getLogger().info("Started Docker nginx container!");
                } else {
                    getLogger().warn("Maximum number of assist containers is set to " + maxAssistContainers + " and "
                            + containersStarted + " are already running so no more Docker containers will be created!");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                getLogger().error("Problem!!!!!: " + ex.getMessage());
            }

        }
    }

    private boolean startNewDockerContainer(List<ConnectionStatus> statuses) {
        return true;

    }

    private String[] commaDelimitedToStringArray(String commaValue) {
        return StringUtils.split(commaValue, ",");
    }

    /**
     * Recursively enumertes the ProcessGroupStatus looking for ConnectionStatus that are "busy" based on the user defined parameters.
     *
     * @param pg
     * @return
     */
    private List<ConnectionStatus> searchForBusyConnections(ProcessGroupStatus pg) {

        List<ConnectionStatus> bcs = new ArrayList<>();

        getLogger().info("Examining ProcessGroup: " + pg.getName());

        //Gets all of the Connections in this group
        Collection<ConnectionStatus> connectionStatuses = pg.getConnectionStatus();
        Iterator<ConnectionStatus> iterator = connectionStatuses.iterator();
        while (iterator.hasNext()) {
            ConnectionStatus cs = iterator.next();
            if (cs.getQueuedCount() > 10) {
                bcs.add(cs);
            }
        }

        //Recursively call of the ProcessGroups nested inside of this ProcessGroup
        Iterator<ProcessGroupStatus> pgs = pg.getProcessGroupStatus().iterator();
        while (pgs.hasNext()) {
            bcs.addAll(searchForBusyConnections(pgs.next()));
        }

        return bcs;
    }

}

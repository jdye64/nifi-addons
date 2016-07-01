package com.jeremydyer.reporting.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;

/**
 * Created by jdyer on 4/22/16.
 */
public class Main {

    public static void main(String[] args) throws DockerException, InterruptedException {
        System.out.println("Testing");

        // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
        try {
            final DockerClient docker = DefaultDockerClient.builder()
                    .uri("http://192.168.99.100:2375")
                    .build();

            //Create a container
            ContainerConfig cc = ContainerConfig.builder()
                    .image("nginx")
                    .build();
            final ContainerCreation container = docker.createContainer(cc);

            docker.startContainer(container.id());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

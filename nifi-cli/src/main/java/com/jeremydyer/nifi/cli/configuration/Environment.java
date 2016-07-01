package com.jeremydyer.nifi.cli.configuration;

import java.util.ArrayList;

/**
 * NiFi cluster environment
 *
 * Created by jdyer on 5/11/16.
 */
public class Environment {

    private String environmentName;
    private ArrayList<NiFiInstance> instances;
    private String hostname;
    private String port;

    public Environment() {}

    public Environment(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public ArrayList<NiFiInstance> getInstances() {
        return instances;
    }

    public void setInstances(ArrayList<NiFiInstance> instances) {
        this.instances = instances;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}

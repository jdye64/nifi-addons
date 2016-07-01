package com.jeremydyer.nifi.cli.configuration;

import java.util.ArrayList;

/**
 * Parent object holding all of the required configurations from the NiFi-CLI application
 * including environments, passwords, intervals, operations, users, etc
 *
 * Created by jdyer on 5/11/16.
 */
public class NiFiCLIConfiguration {

    private String configurationVersion;
    private ArrayList<Environment> environments;

    public NiFiCLIConfiguration() {
        //TODO: set these to something much more intelligent by pulling from the instances themselves.
        this.configurationVersion = "0.1";
        environments = new ArrayList<Environment>();
        environments.add(new Environment("dev"));
        environments.add(new Environment("qa"));
        environments.add(new Environment("prod"));
    }

    public boolean doesEnvironmentExist(String envName) {
        boolean exists = false;
        for (Environment env : environments) {
            if (env.getEnvironmentName().equals(envName)) {
                exists = true;
            }
        }
        return exists;
    }

    public Environment getEnvironmentByName(String envName) {
        for (Environment env : environments) {
            if (env.getEnvironmentName().equals(envName)) {
                return env;
            }
        }
        return null;
    }

    public String getConfigurationVersion() {
        return configurationVersion;
    }

    public void setConfigurationVersion(String configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    public ArrayList<Environment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(ArrayList<Environment> environments) {
        this.environments = environments;
    }
}

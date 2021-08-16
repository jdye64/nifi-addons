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
package com.jeremydyer.nifi.nvidia;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Tags({ "Nvidia", "Python", "Anaconda", "Conda"})
@CapabilityDescription("Manage Anaconda environments on the host. Expects a valid Anaconda/Miniconda installation to be present")
public class AnacondaEnvironmentService
        extends AbstractControllerService implements AnacondaEnvironment {

    private String currentCondaEnv = null;
    private String condaPrefix = null;

    public static final PropertyDescriptor CONDA_PREFIX = new PropertyDescriptor
            .Builder().name("Conda Prefix")
            .description("The base location of the conda installation. If this property is set it takes precedent over the environment variable $CONDA_PREFIX")
            .required(false)
            .addValidator(StandardValidators.FILE_EXISTS_VALIDATOR)
            .defaultValue("/home/nifi/miniconda3")
            .build();

    public static final PropertyDescriptor CONDA_ENV = new PropertyDescriptor
            .Builder().name("Conda Environment")
            .description("Name of the Conda environment that execution should occur in")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .defaultValue("nifi_cudf")
            .build();

    public static final PropertyDescriptor CREATE_MISSING_ENV = new PropertyDescriptor
            .Builder().name("Create Conda Environment")
            .description("Creates the missing anaconda environment if it doesn't exist locally")
            .required(true)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .defaultValue("true")
            .build();

    private static final List<PropertyDescriptor> properties;

    static {
        properties = List.of(CONDA_PREFIX, CONDA_ENV, CREATE_MISSING_ENV);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    /**
     * @param context
     *            the configuration context
     * @throws InitializationException
     *             if unable to create a database connection
     */
    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException, IOException {

        this.condaPrefix = context.getProperty(CONDA_PREFIX).getValue();
        if (!context.getProperty(CONDA_PREFIX).isSet()) {
            this.condaPrefix = System.getenv("CONDA_PREFIX");
        }

        if (this.condaPrefix == null) {
            throw new InitializationException("'CONDA_PREFIX' was unable to be detected. Ensure Anaconda is properly installed on this host.");
        }
        getLogger().warn("CONDA_PREFIX: " + this.condaPrefix);

        this.currentCondaEnv = context.getProperty(CONDA_ENV).getValue();
        File envDir = new File(this.condaPrefix + File.separator + "envs" + File.separator + this.currentCondaEnv);
        getLogger().warn("Conda Environment Dir: " + envDir.toString());
        if (!envDir.exists() && context.getProperty(CREATE_MISSING_ENV).asBoolean()) {
            getLogger().warn("Creating new Conda environment '" + this.currentCondaEnv + "'");
            createCondaEnv(this.currentCondaEnv);
        }
    }

    /**
     * Creates a new Conda environment if it is missing
     *
     * @param envName
     *  Name of the anaconda environment that should be created
     *
     * @throws ProcessException
     */
    private void createCondaEnv(String envName) throws ProcessException {

        List<String> processParameters = new ArrayList<>();
        processParameters.add(this.condaPrefix + File.separator + "bin" + File.separator + "conda");
        processParameters.add("create");
        processParameters.add("--name");
        processParameters.add(this.getCurrentEnvName());
        processParameters.add("python==3.8.0");
        processParameters.add("-q");
        processParameters.add("-y");

        ProcessBuilder pb = new ProcessBuilder(processParameters);
        try {
            Process process = pb.start();
            int exitValue = process.waitFor();
            getLogger().error("Process Exit Value: " + exitValue);
        } catch (IOException e) {
            getLogger().error(e.toString());
            e.printStackTrace();
        } catch (InterruptedException e) {
            getLogger().error(e.toString());
            e.printStackTrace();
        }
    }

    public ProcessResult runPython(String python) throws ProcessException {
        ProcessResult pr = new ProcessResult();

        List<String> processParameters = new ArrayList<>();
        processParameters.add(this.condaPrefix + File.separator + "envs" + File.separator + this.currentCondaEnv + File.separator + "bin" + File.separator + "python3");
        processParameters.add("-c");
        processParameters.add(python);

        ProcessBuilder pb = new ProcessBuilder(processParameters);
        try {
            Process process = pb.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            String result = builder.toString();
            int exitValue = process.waitFor();

            pr.setExitCode(exitValue);
            pr.setProcessOutput(result);
        } catch (IOException e) {
            getLogger().error(e.toString());
            e.printStackTrace();
        } catch (InterruptedException e) {
            getLogger().error(e.toString());
            e.printStackTrace();
        }

        return pr;
    }

    @Override
    public String getCurrentEnvName() throws ProcessException {
        return this.currentCondaEnv;
    }

    @Override
    public String getCondaPrefix() throws ProcessException {
        return this.condaPrefix;
    }

    @OnDisabled
    public void shutdown() {
        getLogger().warn("Shutting down Anaconda controller service");
    }
}

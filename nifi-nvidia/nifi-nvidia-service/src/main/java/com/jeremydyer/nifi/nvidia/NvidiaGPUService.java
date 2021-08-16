package com.jeremydyer.nifi.nvidia;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;

import java.io.IOException;
import java.util.List;

@Tags({ "Nvidia", "GPU"})
@CapabilityDescription("Manage GPU utilization in an environment")
public class NvidiaGPUService
        extends AbstractControllerService implements NvidiaGPU {

    public static final PropertyDescriptor CONDA_PREFIX = new PropertyDescriptor
            .Builder().name("Conda Prefix")
            .description("The base location of the conda installation. If this property is set it takes precedent over the environment variable $CONDA_PREFIX")
            .required(false)
            .addValidator(StandardValidators.FILE_EXISTS_VALIDATOR)
            .defaultValue("/home/nifi/miniconda3")
            .build();

    private static final List<PropertyDescriptor> properties;

    static {
        properties = List.of(CONDA_PREFIX);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException, IOException {

    }

    @OnDisabled
    public void shutdown() {
        getLogger().warn("Shutting down NvidiaGPUService");
    }
}

package com.jeremydyer.nifi.cli.service;

import com.jeremydyer.nifi.cli.configuration.Environment;
import org.apache.nifi.web.api.entity.ControllerStatusEntity;


/**
 * Created by jdyer on 5/11/16.
 */
public class EnvironmentServiceImpl
    implements EnvironmentService {

    private ControllerService controllerService = null;

    public EnvironmentServiceImpl(Environment environment) {
        controllerService = new ControllerServiceImplementation(environment.getHostname(), environment.getPort());
    }

    public ControllerStatusEntity getEnvironmentControllerStatus(Environment env) {
        return controllerService.getControllerStatus(null);
    }
}

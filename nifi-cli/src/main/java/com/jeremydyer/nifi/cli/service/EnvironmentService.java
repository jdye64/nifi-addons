package com.jeremydyer.nifi.cli.service;

import com.jeremydyer.nifi.cli.configuration.Environment;
import org.apache.nifi.web.api.entity.ControllerStatusEntity;

/**
 * Created by jdyer on 5/11/16.
 */
public interface EnvironmentService {

    ControllerStatusEntity getEnvironmentControllerStatus(Environment env);
}

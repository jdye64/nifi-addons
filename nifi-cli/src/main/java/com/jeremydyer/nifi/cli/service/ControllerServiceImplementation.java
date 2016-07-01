package com.jeremydyer.nifi.cli.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeremydyer.nifi.cli.client.NiFiAPIClient;
import org.apache.nifi.web.api.dto.status.ControllerStatusDTO;
import org.apache.nifi.web.api.entity.ControllerStatusEntity;

import java.io.IOException;

/**
 * Created by jdyer on 4/8/16.
 */
public class ControllerServiceImplementation
    implements ControllerService {

    private NiFiAPIClient client;
    private ObjectMapper mapper;

    public ControllerServiceImplementation(String server, String port) {
        client = new NiFiAPIClient(server, port);
        mapper = new ObjectMapper();
    }

    public String getController(String clientId) {
        return this.client.get("/controller");
    }

    public String getControllerAbout(String clientId) {
        return this.client.get("/controller/about");
    }

    public String postControllerArchieve(String version, String clientId) {
        return null;
    }

    public String getControllerAuthorties(String clientId) {
        return this.client.get("/controller/about");
    }

    public String getControllerBanners(String clientId) {
        return this.client.get("/controller/banners");
    }

    public String getControllerBulletinBoard(String clientId, String after, String sourceName, String message, String sourceId, String groupId, String limit) {
        return null;
    }

    public String getControllerConfiguration(String clientId) {
        return this.client.get("/controller/config");
    }

    public String putControllerConfiguration(String clientId) {
        return null;
    }

    public ControllerStatusEntity getControllerStatus(String clientId) {
        String jsonResponse = this.client.get("/controller/status");
        try {
            return mapper.readValue(jsonResponse, ControllerStatusEntity.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String postControllerTemplate(String clientId, String name, String description, String snippetId) {
        return null;
    }

    public String getControllerAllTemplates(String clientId) {
        return this.client.get("/controller/templates");
    }

    public String getControllerTemplate(String clientId, String templateId) {
        return null;
    }

    public String deleteControllertemplate(String clientId, String templateId) {
        return null;
    }

    public String getControllerSystemDiagnostics(String clientId) {
        return this.client.get("/controller/diagnostics");
    }
}

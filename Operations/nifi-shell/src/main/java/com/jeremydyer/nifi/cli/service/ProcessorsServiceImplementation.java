package com.jeremydyer.nifi.cli.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeremydyer.nifi.cli.client.NiFiAPIClient;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by jdyer on 4/8/16.
 */
public class ProcessorsServiceImplementation
    extends AbstractBaseService
    implements ProcessorsService {

    public ProcessorsServiceImplementation(String server, String port) {
        client = new NiFiAPIClient(server, port);
    }

    public ProcessorDTO getProcessors(String clientId, String processorGroupId) {
        String response = client.get("/controller/process-groups/" + processorGroupId + "/processors");
        System.out.println("Response: " + response);
        ObjectMapper mapper = new ObjectMapper();

        ProcessorDTO resource = null;
        try {
            JSONObject root = new JSONObject(response);
            resource = mapper.readValue(response, ProcessorDTO.class);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return resource;
    }
}

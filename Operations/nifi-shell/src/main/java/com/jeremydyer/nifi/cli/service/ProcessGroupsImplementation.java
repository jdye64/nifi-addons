package com.jeremydyer.nifi.cli.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeremydyer.nifi.cli.client.NiFiAPIClient;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by jdyer on 4/8/16.
 */
public class ProcessGroupsImplementation
    extends AbstractBaseService
    implements ProcessGroups {

    public ProcessGroupsImplementation(String server, String port) {
        client = new NiFiAPIClient(server, port);
    }

    public ProcessGroupDTO getProcessGroup(String clientId, boolean recursive, boolean verbose, String processGroupId) {
        String response = client.get("/controller/process-groups/" + processGroupId);
        System.out.println("Response: " + response);
        ObjectMapper mapper = new ObjectMapper();

        ProcessGroupDTO resource = null;
        try {
            JSONObject root = new JSONObject(response);
            resource = mapper.readValue(root.getJSONObject("processGroup").toString(), ProcessGroupDTO.class);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return resource;
    }
}

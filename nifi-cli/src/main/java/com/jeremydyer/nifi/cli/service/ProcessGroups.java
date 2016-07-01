package com.jeremydyer.nifi.cli.service;

import org.apache.nifi.web.api.dto.ProcessGroupDTO;

/**
 * Created by jdyer on 4/8/16.
 */
public interface ProcessGroups {

    /**
     * Gets a process group
     *
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response
     *
     * @param recursive
     *  Whether the response should contain all encapsulated components or just the immediate children.
     *
     * @param verbose
     *  Whether to include any encapulated components or just details about the process group.
     *
     * @param processGroupId
     *  The id of the process group that is the parent of the requested resource(s). If the desired process group is the root group an alias 'root' may be used as the process-group-id.
     *
     * @return
     */
    ProcessGroupDTO getProcessGroup(String clientId, boolean recursive, boolean verbose, String processGroupId);
}

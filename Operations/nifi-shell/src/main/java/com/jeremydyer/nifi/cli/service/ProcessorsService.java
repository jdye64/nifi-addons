package com.jeremydyer.nifi.cli.service;

import org.apache.nifi.web.api.dto.ProcessorDTO;

/**
 * Created by jdyer on 4/8/16.
 */
public interface ProcessorsService {

    /**
     * Gets all processors
     *
     * @param clientId
     *
     * @param processorGroupId
     *  The id of the process group that is the parent of the requested resource(s). If the desired process group is the root group an alias 'root' may be used as the process-group-id.
     *
     * @return
     */
    ProcessorDTO getProcessors(String clientId, String processorGroupId);
}

package com.jeremydyer.nifi.cli.service;

import org.apache.nifi.web.api.dto.status.ControllerStatusDTO;
import org.apache.nifi.web.api.entity.ControllerStatusEntity;

/**
 * Created by jdyer on 4/8/16.
 */
public interface ControllerService {

    /**
     * Returns the details about this NiFi necessary to communicate via site to site
     * @return
     */
    String getController(String clientId);

    /**
     * Retrieves details about this NiFi to put in the About dialog
     * @return
     */
    String getControllerAbout(String clientId);

    /**
     * Creates a new archive of this NiFi flow configuration
     * @return
     */
    String postControllerArchieve(String version, String clientId);

    /**
     * Retrieves the user details, including the authorities, about the user making the request
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     * @return
     */
    String getControllerAuthorties(String clientId);

    /**
     * Retrieves the banners for this NiFi
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     * @return
     */
    String getControllerBanners(String clientId);

    /**
     * Gets current bulletins
     *
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     * @param after
     *  Includes bulletins with an id after this value.
     * @param sourceName
     *  Includes bulletins originating from this sources whose name match this regular expression.
     * @param message
     *  Includes bulletins whose message that match this regular expression.
     * @param sourceId
     *  Includes bulletins originating from this sources whose id match this regular expression.
     * @param groupId
     *  Includes bulletins originating from this sources whose group id match this regular expression.
     * @param limit
     *  The number of bulletins to limit the response to.
     *
     * @return
     */
    String getControllerBulletinBoard(String clientId, String after, String sourceName, String message, String sourceId, String groupId, String limit);

    /**
     * Retrieves the configuration for this NiFi
     *
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     *
     * @return
     */
    String getControllerConfiguration(String clientId);

    String putControllerConfiguration(String clientId);

    /**
     * Gets the current status of this NiFi
     *
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     *
     * @return
     */
    ControllerStatusEntity getControllerStatus(String clientId);

    /**
     * Creates a template
     *
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     * @param name
     *  The template name.
     * @param description
     *  The template description.
     * @param snippetId
     *  The id of the snippet whose contents will comprise the template.
     *
     * @return
     */
    String postControllerTemplate(String clientId, String name, String description, String snippetId);

    /**
     * Gets all templates
     *
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     *
     * @return
     */
    String getControllerAllTemplates(String clientId);

    /**
     * Exports a template
     *
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     *
     * @param templateId
     *  The template id.
     *
     * @return
     */
    String getControllerTemplate(String clientId, String templateId);

    /**
     * Deletes a template
     *
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     *
     * @param templateId
     *  The template id.
     *
     * @return
     */
    String deleteControllertemplate(String clientId, String templateId);

    /**
     * Gets the diagnostics for the system NiFi is running on
     *
     * @param clientId
     *  If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.
     *
     * @return
     */
    String getControllerSystemDiagnostics(String clientId);
}

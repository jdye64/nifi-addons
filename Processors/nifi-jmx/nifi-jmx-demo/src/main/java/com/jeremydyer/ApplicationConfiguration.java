package com.jeremydyer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;


/**
 * Created by Jeremy Dyer on 11/19/14.
 */
public class ApplicationConfiguration
        extends Configuration {

    @NotEmpty
    private String applicationName;

    @JsonProperty
    public String getApplicationName() {
        return applicationName;
    }

    @JsonProperty
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

}

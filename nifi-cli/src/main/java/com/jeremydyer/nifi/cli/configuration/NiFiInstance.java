package com.jeremydyer.nifi.cli.configuration;

/**
 * Created by jdyer on 5/11/16.
 */
public class NiFiInstance {

    private String nifiVersion;
    private String hostname;

    public NiFiInstance() {}

    public String getNifiVersion() {
        return nifiVersion;
    }

    public void setNifiVersion(String nifiVersion) {
        this.nifiVersion = nifiVersion;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}

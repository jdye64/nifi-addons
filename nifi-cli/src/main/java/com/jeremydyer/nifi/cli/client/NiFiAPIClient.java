package com.jeremydyer.nifi.cli.client;

/**
 * Created by jdyer on 4/8/16.
 */
public class NiFiAPIClient
        extends AbstractNiFiAPIClient {

    public NiFiAPIClient(String server, String port) {
        super.setupClient(server, port);
    }
}

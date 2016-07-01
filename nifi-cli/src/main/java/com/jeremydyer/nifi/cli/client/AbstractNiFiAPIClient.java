package com.jeremydyer.nifi.cli.client;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by jdyer on 4/8/16.
 */
public abstract class AbstractNiFiAPIClient {

    private String server;
    private String port;
    private String baseUrl;
    private HttpClient client;

    protected void setupClient(String server, String port) {
        this.server = server;
        this.port = port;
        this.baseUrl = "http://" + this.server + ":" + this.port + "/nifi-api";
        this.client = HttpClientBuilder.create().build();
    }

    public String get(String nifiCommand) {

        HttpGet request = new HttpGet(this.baseUrl + nifiCommand);

        StringBuffer result = new StringBuffer();
        try {
            // add request header
            //request.addHeader("User-Agent", USER_AGENT);
            HttpResponse response = client.execute(request);

            System.out.println("Response Code : "
                    + response.getStatusLine().getStatusCode());

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result.toString();
    }

    public String post(String nifiCommand) {
        HttpGet request = new HttpGet(this.baseUrl + nifiCommand);

        StringBuffer result = new StringBuffer();
        try {
            // add request header
            //request.addHeader("User-Agent", USER_AGENT);
            HttpResponse response = client.execute(request);

            System.out.println("Response Code : "
                    + response.getStatusLine().getStatusCode());

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result.toString();
    }
}

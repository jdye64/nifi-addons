package com.jeremydyer.processors.salesforce.base;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import com.jeremydyer.nifi.salesforce.SalesforceUserPassAuthentication;

/**
 * Created by jdyer on 8/4/16.
 */
public class AbstractSalesforceRESTOperation
        extends AbstractProcessor {

    protected static final PropertyDescriptor SALESFORCE_AUTH_SERVICE = new PropertyDescriptor
            .Builder().name("Salesforce.com Authentication Controller Service")
            .description("Your Salesforce.com authentication service for authenticating against Salesforce.com")
            .required(true)
            .identifiesControllerService(SalesforceUserPassAuthentication.class)
            .build();

    protected static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Operation completed successfully")
            .build();

    protected static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Operation failed")
            .build();

    protected static final String SALESFORCE_VERSION = "v36.0";
    protected static final String SALESFORCE_URL_BASE = "https://test.salesforce.com/";
    protected static final String RESPONSE_JSON = "json";
    protected static final String RESPONSE_XML = "xml";

    @Override
    public void onTrigger(ProcessContext processContext, ProcessSession processSession) throws ProcessException {

    }

    // HTTP GET request
    protected String sendGet(String accessToken, String responseFormat, String url) throws Exception {

        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //Add headers
        con.setRequestProperty("Authorization: Bearer ", accessToken);
        con.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        int responseCode = con.getResponseCode();
        getLogger().info("\nSending 'GET' request to URL : " + url);
        getLogger().info("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        getLogger().info(response.toString());
        return response.toString();

    }

    // HTTP POST request
    protected String sendPost(String accessToken, String responseFormat, String url) throws Exception {

        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Authorization: Bearer ", accessToken);
        con.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + urlParameters);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());
        return response.toString();
    }


    protected String generateSalesforceURL(String apiEndpoint) {
        StringBuilder url = new StringBuilder();
        url.append(SALESFORCE_URL_BASE);
        url.append(SALESFORCE_VERSION);
        url.append("/");
        url.append(apiEndpoint);

        try {
            return URLEncoder.encode(url.toString(), "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}

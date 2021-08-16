/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeremydyer.nifi.salesforce;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.json.JSONObject;

@Tags({ "Salesforce.com", "username-password", "oauth", "authentication"})
@CapabilityDescription("Service to provide authentication services against Salesforce.com")
public class SalesforceUserPassAuthenticationService
        extends AbstractControllerService implements SalesforceUserPassAuthentication {

    //Salesforce.com Documentation around this authentication flow
    //https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/intro_understanding_username_password_oauth_flow.htm

    private final String GRANT_TYPE = "password";
    private String accessToken = null;

    //TODO: create a custom validator. Make sure the user is entering a URL and it is using HTTPS which is required by Salesforce.
    public static final PropertyDescriptor AUTH_ENDPOINT = new PropertyDescriptor
            .Builder().name("Salesforce REST Authentication Endpoint")
            .description("The URL for the authentication endpoint for Salesforce.com")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(true)
            .defaultValue("https://test.salesforce.com/services/oauth2/token")
            .build();

    public static final PropertyDescriptor CLIENT_ID = new PropertyDescriptor
            .Builder().name("Salesforce.com ClientID")
            .description("The 'Consumer Key' from the connected app definition.")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor CLIENT_SECRET = new PropertyDescriptor
            .Builder().name("Salesforce.com ClientSecret")
            .description("The 'Consumer Secret' from the connected app definition.")
            .required(true)
            .sensitive(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor USERNAME = new PropertyDescriptor
            .Builder().name("Salesforce.com Username")
            .description("End-user's username.")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PASSWORD = new PropertyDescriptor
            .Builder().name("Salesforce.com Password")
            .description("End-user's password.")
            .required(true)
            .sensitive(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private static final List<PropertyDescriptor> properties;

    static {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(AUTH_ENDPOINT);
        props.add(CLIENT_ID);
        props.add(CLIENT_SECRET);
        props.add(USERNAME);
        props.add(PASSWORD);
        properties = Collections.unmodifiableList(props);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    /**
     * @param context
     *            the configuration context
     * @throws InitializationException
     *             if unable to create a database connection
     */
    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException {

        StringBuilder requestBody = new StringBuilder();
        requestBody.append("grant_type=");
        requestBody.append(GRANT_TYPE);
        requestBody.append("&client_id=");
        requestBody.append(context.getProperty(CLIENT_ID).evaluateAttributeExpressions().getValue());
        requestBody.append("&client_secret=");
        requestBody.append(context.getProperty(CLIENT_SECRET).evaluateAttributeExpressions().getValue());
        requestBody.append("&username=");
        try {
            requestBody.append(URLEncoder.encode(context.getProperty(USERNAME).evaluateAttributeExpressions().getValue(), "UTF-8"));
        } catch (UnsupportedEncodingException use) {
            getLogger().error(use.getMessage());
            requestBody.append(context.getProperty(USERNAME).evaluateAttributeExpressions().getValue());
        }

        requestBody.append("&password=");
        requestBody.append(context.getProperty(PASSWORD).evaluateAttributeExpressions().getValue());
        //requestBody.append(context.getProperty(USER_SECURITY_TOKEN).evaluateAttributeExpressions().getValue());


        try {
            URL obj = new URL(context.getProperty(AUTH_ENDPOINT).evaluateAttributeExpressions().getValue());
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            //add request header
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(requestBody.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + context.getProperty(AUTH_ENDPOINT).evaluateAttributeExpressions().getValue());
            System.out.println("Post parameters : " + requestBody.toString());
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
            getLogger().info("Salesforce.com Auth Response: " + response.toString());

            //Parse the response and attempt to get the Salesforce.com access_token
            JSONObject sfResponse = new JSONObject(response.toString());

            if (sfResponse.get("access_token") != null) {
                accessToken = sfResponse.getString("access_token");
                getLogger().info("Salesforce.com Access Token received.");
            }

        } catch (Exception ex) {
            getLogger().error(ex.getMessage());
        }

    }

    @OnDisabled
    public void shutdown() {
        //TODO: Invalidate the access token here and "logout"
    }

    public String getSalesforceAccessToken() throws ProcessException {
        return this.accessToken;
    }

}

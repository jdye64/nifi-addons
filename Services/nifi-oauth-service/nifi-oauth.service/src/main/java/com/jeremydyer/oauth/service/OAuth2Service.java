package com.jeremydyer.oauth.service;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Tags({"salesforce.com", "authentication"})
@CapabilityDescription("Provides authentication services for Salesforce.com activity")
public class OAuth2Service
        extends AbstractControllerService {

    public static final PropertyDescriptor SF_CLIENT_ID = new PropertyDescriptor
            .Builder().name("Client ID")
            .description("The Salesforce ClientId used to perform the OAuth login")
            .defaultValue("3MVG9RHx1QGZ7OsgCq9CAYIb89iGjlOTn41p5QOFHR6N3l6NBQPQ9bZj2G0ij453L.MUGFSla3mAiYj7BCpiA")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_CLIENT_SECRET = new PropertyDescriptor
            .Builder().name("Client Secret")
            .description("The Salesforce ClientSecret used to perform the OAuth login")
            .defaultValue("7164985079946323147")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_USERNAME = new PropertyDescriptor
            .Builder().name("Salesforce Username")
            .description("The Salesforce username used to perform Salesforce.com login")
            .defaultValue("jdyer@hortonworks.com")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_PASSWORD = new PropertyDescriptor
            .Builder().name("Salesforce Password")
            .description("The Salesforce password used to perform Salesforce.com login")
            .defaultValue("Rascal18")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_SERVER_INSTANCE = new PropertyDescriptor
            .Builder().name("Salesforce Server Instance")
            .description("The Salesforce.com URL")
            .defaultValue("https://test.salesforce.com")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_PUSH_LONG_POLL_DURATION = new PropertyDescriptor
            .Builder().name("Salesforce Push Long Poll Duration")
            .description("Number of seconds for the Salesforce.com Push Long Poll Duration")
            .required(true)
            .defaultValue("30")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_PUSH_TOPIC = new PropertyDescriptor
            .Builder().name("Salesforce CometD Push Topic")
            .description("The Salesforce.com Push Topic to describe to")
            .defaultValue("AccountActivity")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_PUSH_ENDPOINT = new PropertyDescriptor
            .Builder().name("Salesforce CometD Push Server Endpoint")
            .description("The Salesforce.com URL that is appended to your Salesforce Server instance")
            .defaultValue("/cometd/36.0")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private static final List<PropertyDescriptor> properties;

    static {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(SF_PUSH_TOPIC);
        props.add(SF_CLIENT_ID);
        props.add(SF_CLIENT_SECRET);
        props.add(SF_USERNAME);
        props.add(SF_PASSWORD);
        props.add(SF_SERVER_INSTANCE);
        props.add(SF_PUSH_LONG_POLL_DURATION);
        props.add(SF_PUSH_ENDPOINT);
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

        try {
            String server = context.getProperty(SF_SERVER_INSTANCE).getValue();
            String clientId = context.getProperty(SF_CLIENT_ID).getValue();
            String clientSecret = context.getProperty(SF_CLIENT_SECRET).getValue();
            String username = context.getProperty(SF_USERNAME).getValue();
            String password = context.getProperty(SF_PASSWORD).getValue();

            getLogger().info("Server: " + server + " ClientID: " + clientId + " ClientSecret: " + clientSecret + " Username: " + username + " Password: " + password);

//            JSONObject response = oauthLogin(server, clientId, clientSecret, username, password);
//            getLogger().info("Authentication Response: " + response.toString());

        } catch (Exception ex) {
            getLogger().error(ex.getMessage());
        }
    }

    @OnDisabled
    public void shutdown() {

    }


//    private static JSONObject oauthLogin(String LOGIN_SERVER, String CLIENT_ID, String CLIENT_SECRET,
//                                         String USERNAME, String PASSWORD) throws Exception {
//        HttpClient httpClient = new HttpClient(new SslContextFactory(true));
//        httpClient.start();
//
//        String url = LOGIN_SERVER + "/services/oauth2/token";
//        String message = "grant_type=password&client_id=" + CLIENT_ID
//                + "&client_secret=" + CLIENT_SECRET + "&username=" + USERNAME
//                + "&password=" + PASSWORD;
//
//        BytesContentProvider body = new BytesContentProvider(message.getBytes());
//
//        ContentResponse response = httpClient
//                .POST(url)
//                .timeout(10, TimeUnit.SECONDS)
//                .header("Content-Type", "application/x-www-form-urlencoded")
//                .content(body)
//                .send();
//        //.get(5, TimeUnit.SECONDS);
//
//
////        ContentExchange exchange = new ContentExchange();
////        exchange.setMethod("POST");
////        exchange.setURL(url);
//
//
////        exchange.setRequestHeader("Content-Type",
////                "application/x-www-form-urlencoded");
////        exchange.setRequestContentSource(new ByteArrayInputStream(message
////                .getBytes("UTF-8")));
////
////        httpClient.send(exchange);
////        exchange.waitForDone();
//
//        return new JSONObject(new JSONTokener(response.getContentAsString()));
//
//    }

}

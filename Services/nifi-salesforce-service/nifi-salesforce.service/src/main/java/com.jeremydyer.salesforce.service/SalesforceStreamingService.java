package com.jeremydyer.salesforce.service;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Tags({"example"})
@CapabilityDescription("Example ControllerService implementation of MyService.")
public class SalesforceStreamingService
        extends AbstractControllerService implements SalesforceService {

    public static final PropertyDescriptor SF_CLIENT_ID = new PropertyDescriptor
            .Builder().name("Salesforce Client_Id")
            .description("The Salesforce ClientId used to perform the OAuth login")
            .defaultValue("3MVG9RHx1QGZ7OsgCq9CAYIb89iGjlOTn41p5QOFHR6N3l6NBQPQ9bZj2G0ij453L.MUGFSla3mAiYj7BCpiA")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_CLIENT_SECRET = new PropertyDescriptor
            .Builder().name("Salesforce Client_Secret")
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

    private BayeuxClient bayeuxClient = null;
    private final BlockingQueue<JSONObject> jsonQueue = new LinkedBlockingQueue<>();

    /**
     * @param context
     *            the configuration context
     * @throws InitializationException
     *             if unable to create a database connection
     */
    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException {

        try {
            int timeoutValue = context.getProperty(SF_PUSH_LONG_POLL_DURATION).asInteger() * 1000;
            String defaultPushEndpoint = context.getProperty(SF_PUSH_ENDPOINT).getValue();
            String topic = context.getProperty(SF_PUSH_TOPIC).getValue();

            String server = context.getProperty(SF_SERVER_INSTANCE).getValue();
            String clientId = context.getProperty(SF_CLIENT_ID).getValue();
            String clientSecret = context.getProperty(SF_CLIENT_SECRET).getValue();
            String username = context.getProperty(SF_USERNAME).getValue();
            String password = context.getProperty(SF_PASSWORD).getValue();

            getLogger().info("Server: " + server + " ClientID: " + clientId + " ClientSecret: " + clientSecret + " Username: " + username + " Password: " + password);

            this.bayeuxClient = getClient(timeoutValue, defaultPushEndpoint, server, clientId, clientSecret, username, password);
            this.bayeuxClient.handshake();

            getLogger().info("Waiting for Salesforce.com CometD handshake");
            waitForHandshake(this.bayeuxClient, 60 * 1000, 1000);

            getLogger().info("Subscribing to Topic: " + topic);

            this.bayeuxClient.getChannel("/topic/" + topic).subscribe(new ClientSessionChannel.MessageListener() {
                @Override
                public void onMessage(ClientSessionChannel channel, Message message) {
                    try {
                        JSONObject jsonObject = new JSONObject(new JSONTokener(message.getJSON()));
                        getLogger().info(jsonObject.toString(2));
                        jsonQueue.add(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            getLogger().info("Waiting for streamed data from Force.com...");

        } catch (Exception ex) {
            getLogger().error(ex.getMessage());
            //session.transfer(flowFile, REL_LOGIN_FAILURE);
        }
    }

    @OnDisabled
    public void shutdown() {

    }



    private static BayeuxClient getClient(int TIMEOUT, String DEFAULT_PUSH_ENDPOINT, String server, String ClientId, String clientSecret,
                                          String username, String password) throws Exception {
        // Authenticate via OAuth
        JSONObject response = oauthLogin(server, ClientId, clientSecret, username, password);
        System.out.println("Login response: " + response.toString(2));
        if (!response.has("access_token")) {
            throw new Exception("OAuth failed: " + response.toString());
        }

        // Get what we need from the OAuth response
        final String sid = response.getString("access_token");
        String instance_url = response.getString("instance_url");

        // Set up a Jetty HTTP client to use with CometD
        HttpClient httpClient = new HttpClient(new SslContextFactory(true));
        httpClient.setConnectTimeout(TIMEOUT);
        httpClient.start();

        Map<String, Object> options = new HashMap<String, Object>();
        //options.put(ClientTransport.TIMEOUT_OPTION, TIMEOUT);

        // Adds the OAuth header in LongPollingTransport
        LongPollingTransport transport = new LongPollingTransport(
                options, httpClient) {
            @Override
            protected void customize(Request request) {
                super.customize(request);
                request.header("Authorization", "OAuth " + sid);
            }
        };

        // Now set up the Bayeux client itself
        BayeuxClient client = new BayeuxClient(instance_url
                + DEFAULT_PUSH_ENDPOINT, transport);

        return client;
    }

    private static void waitForHandshake(BayeuxClient client,
                                         long timeoutInMilliseconds, long intervalInMilliseconds) {
        long start = System.currentTimeMillis();
        long end = start + timeoutInMilliseconds;
        while (System.currentTimeMillis() < end) {
            if (client.isHandshook())
                return;
            try {
                Thread.sleep(intervalInMilliseconds);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("Client did not handshake with server");
    }

    public static void setNoValidation() throws Exception {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs,
                                           String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs,
                                           String authType) {
            }
        } };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    private static JSONObject oauthLogin(String LOGIN_SERVER, String CLIENT_ID, String CLIENT_SECRET,
                                         String USERNAME, String PASSWORD) throws Exception {
        HttpClient httpClient = new HttpClient(new SslContextFactory(true));
        httpClient.start();

        String url = LOGIN_SERVER + "/services/oauth2/token";
        String message = "grant_type=password&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET + "&username=" + USERNAME
                + "&password=" + PASSWORD;

        BytesContentProvider body = new BytesContentProvider(message.getBytes());

        ContentResponse response = httpClient
                .POST(url)
                .timeout(10, TimeUnit.SECONDS)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .content(body)
                .send();
        //.get(5, TimeUnit.SECONDS);


//        ContentExchange exchange = new ContentExchange();
//        exchange.setMethod("POST");
//        exchange.setURL(url);


//        exchange.setRequestHeader("Content-Type",
//                "application/x-www-form-urlencoded");
//        exchange.setRequestContentSource(new ByteArrayInputStream(message
//                .getBytes("UTF-8")));
//
//        httpClient.send(exchange);
//        exchange.waitForDone();

        return new JSONObject(new JSONTokener(response.getContentAsString()));

    }

}
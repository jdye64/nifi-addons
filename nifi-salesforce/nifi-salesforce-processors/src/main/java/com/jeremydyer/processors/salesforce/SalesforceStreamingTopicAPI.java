package com.jeremydyer.processors.salesforce;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.TriggerWhenEmpty;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONObject;
import org.json.JSONTokener;


@Tags({"salesforce", "streaming", "api", "PushTopic", "channel"})
@TriggerWhenEmpty
@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
@CapabilityDescription("SalesforceStreamingAPI for subscribing to a Salesforce APEX PushTopic and receiving real time updates" +
        "based on the Salesforce defined SOQL setup in your Salesforce account.")
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class SalesforceStreamingTopicAPI
        extends AbstractProcessor {

    public static final PropertyDescriptor SF_CLIENT_ID = new PropertyDescriptor
            .Builder().name("Salesforce Client_Id")
            .description("The Salesforce ClientId used to perform the OAuth login")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_CLIENT_SECRET = new PropertyDescriptor
            .Builder().name("Salesforce Client_Secret")
            .description("The Salesforce ClientSecret used to perform the OAuth login")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_USERNAME = new PropertyDescriptor
            .Builder().name("Salesforce Username")
            .description("The Salesforce username used to perform Salesforce.com login")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SF_PASSWORD = new PropertyDescriptor
            .Builder().name("Salesforce Password")
            .description("The Salesforce password used to perform Salesforce.com login")
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

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("SOQL was successfully created")
            .build();

    public static final Relationship REL_LOGIN_FAILURE = new Relationship.Builder()
            .name("login_failure")
            .description("failed to login to salesforce.com")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("failed to perform Salesforce API Streaming")
            .build();


    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;
    private BayeuxClient bayeuxClient = null;

    private final BlockingQueue<JSONObject> jsonQueue = new LinkedBlockingQueue<>();

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(SF_PUSH_TOPIC);
        descriptors.add(SF_CLIENT_ID);
        descriptors.add(SF_CLIENT_SECRET);
        descriptors.add(SF_USERNAME);
        descriptors.add(SF_PASSWORD);
        descriptors.add(SF_SERVER_INSTANCE);
        descriptors.add(SF_PUSH_LONG_POLL_DURATION);
        descriptors.add(SF_PUSH_ENDPOINT);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_LOGIN_FAILURE);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

//    @OnScheduled
//    public void onScheduled(final ProcessContext context) {
//
//        try {
//            int timeoutValue = context.getProperty(SF_PUSH_LONG_POLL_DURATION).asInteger() * 1000;
//            String defaultPushEndpoint = context.getProperty(SF_PUSH_ENDPOINT).getValue();
//            String topic = context.getProperty(SF_PUSH_TOPIC).getValue();
//
//            String server = context.getProperty(SF_SERVER_INSTANCE).getValue();
//            String clientId = context.getProperty(SF_CLIENT_ID).getValue();
//            String clientSecret = context.getProperty(SF_CLIENT_SECRET).getValue();
//            String username = context.getProperty(SF_USERNAME).getValue();
//            String password = context.getProperty(SF_PASSWORD).getValue();
//
//            getLogger().info("Server: " + server + " ClientID: " + clientId + " ClientSecret: " + clientSecret + " Username: " + username + " Password: " + password);
//
//            setNoValidation();
//            this.bayeuxClient = getClient(timeoutValue, defaultPushEndpoint, server, clientId, clientSecret, username, password);
//            this.bayeuxClient.handshake();
//
//            getLogger().info("Waiting for Salesforce.com CometD handshake");
//            waitForHandshake(this.bayeuxClient, 60 * 1000, 1000);
//
//            getLogger().info("Subscribing to Topic: " + topic);
//
//            this.bayeuxClient.getChannel("/topic/" + topic).subscribe(new ClientSessionChannel.MessageListener() {
//                @Override
//                public void onMessage(ClientSessionChannel channel, Message message) {
//                    try {
//                        JSONObject jsonObject = new JSONObject(new JSONTokener(message.getJSON()));
//                        getLogger().info(jsonObject.toString(2));
//                        jsonQueue.add(jsonObject);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            getLogger().info("Waiting for streamed data from Force.com...");
//            while (true) {
//                // This infinite loop is for demo only, to receive streamed events
//                // on the specified topic from Salesforce.com
//                Thread.sleep(60000);
//            }
//
//        } catch (Exception ex) {
//            getLogger().error(ex.getMessage());
//            //session.transfer(flowFile, REL_LOGIN_FAILURE);
//        }
//
//    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }


    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {

        final FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        getLogger().info("Draining JSONObject queue");
        final List<JSONObject> objects = new ArrayList<>(10);
        jsonQueue.drainTo(objects, 10);
        if (objects.isEmpty()) {
            return;
        } else {
            //Process all of the files.
            for (final JSONObject obj : objects) {
                FlowFile ff = session.write(session.create(), new OutputStreamCallback() {
                    @Override
                    public void process(OutputStream outputStream) throws IOException {
                        outputStream.write(obj.toString().getBytes());
                    }
                });

                session.transfer(ff, REL_SUCCESS);
            }
        }
    }


    /**
     * Setup the Bayoux client.
     *
     * @return
     * @throws Exception
     */
    private BayeuxClient getClient(int TIMEOUT, String DEFAULT_PUSH_ENDPOINT, String server, String clientId,
                                          String clientSecret, String username, String password) throws Exception {
        // Authenticate via OAuth
        getLogger().info("Performing Salesforce OAuth2 Login");
        JSONObject response = oauthLogin(server, clientId, clientSecret, username, password);
        getLogger().info("Login response: " + response.toString(2));
        if (!response.has("access_token")) {
            throw new Exception("OAuth failed: " + response.toString());
        }

        // Get what we need from the OAuth response
        final String sid = response.getString("access_token");
        String instance_url = response.getString("instance_url");

        // Set up a Jetty HTTP client to use with CometD
        HttpClient httpClient = new HttpClient();
        httpClient.setConnectTimeout(TIMEOUT);
        httpClient.setTimeout(TIMEOUT);
        httpClient.start();

        Map<String, Object> options = new HashMap<String, Object>();
        options.put(ClientTransport.TIMEOUT_OPTION, TIMEOUT);

        // Adds the OAuth header in LongPollingTransport
        LongPollingTransport transport = new LongPollingTransport(
                options, httpClient) {
            @Override
            protected void customize(ContentExchange exchange) {
                super.customize(exchange);
                exchange.addRequestHeader("Authorization", "OAuth " + sid);
            }
        };

        // Now set up the Bayeux client itself
        BayeuxClient client = new BayeuxClient(instance_url
                + DEFAULT_PUSH_ENDPOINT, transport);

        return client;
    }


    /**
     * Waits for the Salesforce Bayoux client login handshake
     *
     * @param client
     * @param timeoutInMilliseconds
     * @param intervalInMilliseconds
     */
    private void waitForHandshake(BayeuxClient client,
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


    public void setNoValidation() throws Exception {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
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


    /**
     * Performs the Salesforce OAuth login
     *
     * @return
     * @throws Exception
     */
    private JSONObject oauthLogin(String LOGIN_SERVER, String CLIENT_ID, String CLIENT_SECRET,
                                         String USERNAME, String PASSWORD) throws Exception {

        getLogger().info("About to create HTTPClient");
        HttpClient httpClient = new HttpClient();
        httpClient.start();

        String url = LOGIN_SERVER + "/services/oauth2/token";

        ContentExchange exchange = new ContentExchange();
        exchange.setMethod("POST");
        exchange.setURL(url);

        String message = "grant_type=password&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET + "&username=" + USERNAME
                + "&password=" + PASSWORD;

        getLogger().info("Login POST Payload: " + message);

        exchange.setRequestHeader("Content-Type",
                "application/x-www-form-urlencoded");
        exchange.setRequestContentSource(new ByteArrayInputStream(message
                .getBytes("UTF-8")));

        httpClient.send(exchange);
        getLogger().info("Waiting for OAuth login response");
        exchange.waitForDone();

        return new JSONObject(new JSONTokener(exchange.getResponseContent()));

    }
}
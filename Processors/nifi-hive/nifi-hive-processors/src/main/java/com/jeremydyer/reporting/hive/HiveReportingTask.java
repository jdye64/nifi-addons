package com.jeremydyer.reporting.hive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.net.ssl.SSLContext;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.controller.status.PortStatus;
import org.apache.nifi.controller.status.ProcessGroupStatus;
import org.apache.nifi.controller.status.ProcessorStatus;
import org.apache.nifi.controller.status.RemoteProcessGroupStatus;
import org.apache.nifi.events.EventReporter;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.provenance.ProvenanceEventRecord;
import org.apache.nifi.reporting.AbstractReportingTask;
import org.apache.nifi.reporting.ReportingContext;
import org.apache.nifi.reporting.Severity;

@Tags({"hive", "chronicle", "provenance", "lineage", "tracking"})
@CapabilityDescription("Publishes Provenance events from NiFi to Onyara Chronicle")
public class HiveReportingTask extends AbstractReportingTask {
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    static final PropertyDescriptor CHRONICLE_URL = new PropertyDescriptor.Builder()
            .name("Chronicle URL")
            .description("The URL to post the Provenance Events to")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("http://chronicle.onyara.com")
            .addValidator(StandardValidators.URL_VALIDATOR)
            .build();
    static final PropertyDescriptor PORT_NAME = new PropertyDescriptor.Builder()
            .name("Input Port Name")
            .description("The name of Chronicle's Input Port to delivery Provenance Events to")
            .required(true)
            .expressionLanguageSupported(false)
            .defaultValue("Provenance Input")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    static final PropertyDescriptor SSL_CONTEXT = new PropertyDescriptor.Builder()
            .name("SSL Context Service")
            .description("The SSL Context Service to use when communicating with Chronicle. If not specified, communications will not be secure.")
            .required(false)
            .identifiesControllerService(SSLContextService.class)
            .build();
    static final PropertyDescriptor NIFI_URL = new PropertyDescriptor.Builder()
            .name("NiFi URL")
            .description("The URL of this NiFi instance. This is used to include the Content URI to send to Chronicle")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("https://${hostname(true)}:8443/nifi")
            .addValidator(new NiFiUrlValidator())
            .build();
    static final PropertyDescriptor COMPRESS = new PropertyDescriptor.Builder()
            .name("Compress Events")
            .description("Indicates whether or not to compress the events when sending to Chronicle.")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("true")
            .build();
    static final PropertyDescriptor STATE_FILE = new PropertyDescriptor.Builder()
            .name("State File")
            .description("Specifies where to store the Reporting Task's state so that on restart of NiFi the task knows where it left off")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    static final PropertyDescriptor TIMEOUT = new PropertyDescriptor.Builder()
            .name("Communications Timeout")
            .description("Specifies how long to wait to a response from Chronicle before deciding that an error has occurred and canceling the transaction")
            .required(true)
            .defaultValue("30 secs")
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .build();
    static final PropertyDescriptor BATCH_SIZE = new PropertyDescriptor.Builder()
            .name("Batch Size")
            .description("Specifies how many records to send in a single batch")
            .required(true)
            .defaultValue("1000")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();


    private volatile long firstEventId = -1L;
    private volatile SiteToSiteClient siteToSiteClient;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(CHRONICLE_URL);
        properties.add(PORT_NAME);
        properties.add(SSL_CONTEXT);
        properties.add(NIFI_URL);
        properties.add(COMPRESS);
        properties.add(new PropertyDescriptor.Builder().fromPropertyDescriptor(STATE_FILE).defaultValue("./conf/chronicle-" + getIdentifier() + ".state").build());
        properties.add(TIMEOUT);
        properties.add(BATCH_SIZE);
        return properties;
    }

    @OnScheduled
    public void setup(final ConfigurationContext context) throws IOException {
        if (firstEventId < 0) {
            final String stateFilename = context.getProperty(STATE_FILE).getValue();
            final File stateFile = new File(stateFilename);
            if (stateFile.exists()) {
                final byte[] bytes = Files.readAllBytes(stateFile.toPath());
                final String firstEventIdValue = new String(bytes, StandardCharsets.UTF_8);
                firstEventId = Long.parseLong(firstEventIdValue);
            }
        }

        final SSLContextService sslContextService = context.getProperty(SSL_CONTEXT).asControllerService(SSLContextService.class);
        final SSLContext sslContext = sslContextService == null ? null : sslContextService.createSSLContext(ClientAuth.REQUIRED);
        final EventReporter eventReporter = new EventReporter() {
            @Override
            public void reportEvent(final Severity severity, final String category, final String message) {
                switch (severity) {
                    case WARNING:
                        getLogger().warn(message);
                        break;
                    case ERROR:
                        getLogger().error(message);
                        break;
                    default:
                        break;
                }
            }
        };

        final String chronicleUrlPrefix = context.getProperty(CHRONICLE_URL).evaluateAttributeExpressions().getValue();
        final String chronicleUrl = chronicleUrlPrefix + (chronicleUrlPrefix.endsWith("/") ? "nifi" : "/nifi");

        siteToSiteClient = new SiteToSiteClient.Builder()
                .url(chronicleUrl)
                .portName(context.getProperty(PORT_NAME).getValue())
                .useCompression(context.getProperty(COMPRESS).asBoolean())
                .eventReporter(eventReporter)
                .sslContext(sslContext)
                .timeout(context.getProperty(TIMEOUT).asTimePeriod(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                .build();
    }

    @OnStopped
    public void shutdown() throws IOException {
        final SiteToSiteClient client = getClient();
        if (client != null) {
            client.close();
        }
    }

    // this getter is intended explicitly for testing purposes
    protected SiteToSiteClient getClient() {
        return this.siteToSiteClient;
    }

    private String getComponentName(final ProcessGroupStatus status, final ProvenanceEventRecord event) {
        if (status == null) {
            return null;
        }

        final String componentId = event.getComponentId();
        if (status.getId().equals(componentId)) {
            return status.getName();
        }

        for (final ProcessorStatus procStatus : status.getProcessorStatus()) {
            if (procStatus.getId().equals(componentId)) {
                return procStatus.getName();
            }
        }

        for (final PortStatus portStatus : status.getInputPortStatus()) {
            if (portStatus.getId().equals(componentId)) {
                return portStatus.getName();
            }
        }

        for (final PortStatus portStatus : status.getOutputPortStatus()) {
            if (portStatus.getId().equals(componentId)) {
                return portStatus.getName();
            }
        }

        for (final RemoteProcessGroupStatus rpgStatus : status.getRemoteProcessGroupStatus()) {
            if (rpgStatus.getId().equals(componentId)) {
                return rpgStatus.getName();
            }
        }

        for (final ProcessGroupStatus childGroup : status.getProcessGroupStatus()) {
            final String componentName = getComponentName(childGroup, event);
            if (componentName != null) {
                return componentName;
            }
        }

        return null;
    }

    @Override
    public void onTrigger(final ReportingContext context) {
        final ProcessGroupStatus procGroupStatus = context.getEventAccess().getControllerStatus();
        final String rootGroupName = procGroupStatus == null ? null : procGroupStatus.getName();

        final List<ProvenanceEventRecord> events;
        try {
            events = context.getEventAccess().getProvenanceEvents(firstEventId, context.getProperty(BATCH_SIZE).asInteger());
        } catch (final IOException ioe) {
            getLogger().error("Failed to retrieve Provenance Events from repository due to {}", ioe);
            return;
        }

        if (events == null || events.isEmpty()) {
            getLogger().debug("No events to send to Chronicle");
            return;
        }

        final long start = System.nanoTime();
        final Map<String, ?> config = Collections.emptyMap();
        final JsonBuilderFactory factory = Json.createBuilderFactory(config);
        final JsonObjectBuilder builder = factory.createObjectBuilder();

        final String nifiUrl = context.getProperty(NIFI_URL).evaluateAttributeExpressions().getValue();
        URL url;
        try {
            url = new URL(nifiUrl);
        } catch (final MalformedURLException e1) {
            // already validated
            throw new AssertionError();
        }

        final String hostname = url.getHost();

        final JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        for (final ProvenanceEventRecord event : events) {
            arrayBuilder.add(serialize(factory, builder, event, getComponentName(procGroupStatus, event), hostname, url, rootGroupName));
        }
        final JsonArray jsonArray = arrayBuilder.build();

        try {
            final Transaction transaction = getClient().createTransaction(TransferDirection.SEND);
            if (transaction == null) {
                getLogger().debug("All Chronicle nodes are penalized; will attempt to send data later");
                return;
            }

            final Map<String, String> attributes = new HashMap<>();
            final String transactionId = UUID.randomUUID().toString();
            attributes.put("chronicle.transaction.id", transactionId);

            final byte[] data = jsonArray.toString().getBytes(StandardCharsets.UTF_8);
            transaction.send(data, attributes);
            transaction.confirm();
            transaction.complete();

            final long transferMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            getLogger().info("Successfully sent {} Provenance Events to Chronicle in {} ms; Transaction ID = {}; First Event ID = {}",
                    new Object[] {events.size(), transferMillis, transactionId, events.get(0).getEventId()});
        } catch (final IOException e) {
            getLogger().error("Failed to send Provenance Events to Onyara due to {}", new Object[] {e});
        }

        final ProvenanceEventRecord lastEvent = events.get(events.size() - 1);
        final String lastEventId = String.valueOf(lastEvent.getEventId());
        final String stateFilename = context.getProperty(STATE_FILE).getValue();
        final File stateFile = new File(stateFilename);
        try (final OutputStream fos = new FileOutputStream(stateFile)) {
            fos.write(lastEventId.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException ioe) {
            getLogger().error("Failed to update state in state file {} to {} due to {}; this could result in events being re-sent after a restart of NiFi",
                    new Object[] {stateFile, lastEventId, ioe});
        }

        firstEventId = lastEvent.getEventId() + 1;
    }

    static JsonObject serialize(final JsonBuilderFactory factory, final JsonObjectBuilder builder, final ProvenanceEventRecord event,
                                final String componentName, final String hostname, final URL nifiUrl, final String applicationName) {
        addField(builder, "eventId", UUID.randomUUID().toString());
        addField(builder, "eventOrdinal", event.getEventId());
        addField(builder, "eventType", event.getEventType().name());
        addField(builder, "timestampMillis", event.getEventTime());

        final DateFormat df = new SimpleDateFormat(TIMESTAMP_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("Z"));
        addField(builder, "timestamp", df.format(event.getEventTime()));

        addField(builder, "durationMillis", event.getEventDuration());
        addField(builder, "lineageStart", event.getLineageStartDate());

        final Set<String> lineageIdentifiers = new HashSet<>();
        if (event.getLineageIdentifiers() != null) {
            lineageIdentifiers.addAll(event.getLineageIdentifiers());
        }
        lineageIdentifiers.add(event.getFlowFileUuid());
        addField(builder, factory, "lineageIdentifiers", lineageIdentifiers);
        addField(builder, "details", event.getDetails());
        addField(builder, "componentId", event.getComponentId());
        addField(builder, "componentType", event.getComponentType());
        addField(builder, "componentName", componentName);
        addField(builder, "entityId", event.getFlowFileUuid());
        addField(builder, "entityType", "org.apache.nifi.flowfile.FlowFile");
        addField(builder, "entitySize", event.getFileSize());
        addField(builder, "previousEntitySize", event.getPreviousFileSize());
        addField(builder, factory, "updatedAttributes", event.getUpdatedAttributes());
        addField(builder, factory, "previousAttributes", event.getPreviousAttributes());

        addField(builder, "actorHostname", hostname);
        if (nifiUrl != null) {
            final String urlPrefix = nifiUrl.toString().replace(nifiUrl.getPath(), "");
            final String contentUriBase = urlPrefix + "/nifi-api/controller/provenance/events/" + event.getEventId() + "/content/";
            addField(builder, "contentURI", contentUriBase + "output");
            addField(builder, "previousContentURI", contentUriBase + "input");
        }

        addField(builder, factory, "parentIds", event.getParentUuids());
        addField(builder, factory, "childIds", event.getChildUuids());
        addField(builder, "transitUri", event.getTransitUri());
        addField(builder, "remoteIdentifier", event.getSourceSystemFlowFileIdentifier());
        addField(builder, "alternateIdentifier", event.getAlternateIdentifierUri());
        addField(builder, "platform", "nifi");
        addField(builder, "application", applicationName);

        return builder.build();
    }

    private static void addField(final JsonObjectBuilder builder, final JsonBuilderFactory factory, final String key, final Map<String, String> values) {
        if (values == null) {
            return;
        }

        final JsonObjectBuilder mapBuilder = factory.createObjectBuilder();
        for (final Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            mapBuilder.add(entry.getKey(), entry.getValue());
        }

        builder.add(key, mapBuilder);
    }

    private static void addField(final JsonObjectBuilder builder, final String key, final Long value) {
        if (value != null) {
            builder.add(key, value.longValue());
        }
    }

    private static void addField(final JsonObjectBuilder builder, final JsonBuilderFactory factory, final String key, final Collection<String> values) {
        if (values == null) {
            return;
        }

        builder.add(key, createJsonArray(factory, values));
    }

    private static void addField(final JsonObjectBuilder builder, final String key, final String value) {
        if (value == null) {
            return;
        }

        builder.add(key, value);
    }

    private static JsonArrayBuilder createJsonArray(JsonBuilderFactory factory, final Collection<String> values) {
        final JsonArrayBuilder builder = factory.createArrayBuilder();
        for (final String value : values) {
            if (value != null) {
                builder.add(value);
            }
        }
        return builder;
    }


    private static class NiFiUrlValidator implements Validator {
        @Override
        public ValidationResult validate(final String subject, final String input, final ValidationContext context) {
            final String value = context.newPropertyValue(input).evaluateAttributeExpressions().getValue();
            try {
                new URL(value);
            } catch (final Exception e) {
                return new ValidationResult.Builder().input(input).subject(subject).valid(false).explanation("Not a valid URL").build();
            }

            return new ValidationResult.Builder().input(input).subject(subject).valid(true).build();
        }
    }
}
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
package org.apache.nifi.processors.websockets;

import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.glassfish.tyrus.client.ClientManager;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Tags({"websocket, listen"})
@CapabilityDescription("Provide a description")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class ListenWebSocket extends AbstractProcessor {

    public static final PropertyDescriptor ENDPOINT = new PropertyDescriptor
            .Builder().name("WebSocket Endpoint")
            .description("ws://localhost:8025/websockets/test")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MAX_MESSAGE_QUEUE_SIZE = new PropertyDescriptor.Builder()
            .name("Max Size of Message Queue")
            .description("The maximum size of the internal queue used to buffer messages being transferred from the underlying channel to the processor. " +
                    "Setting this value higher allows more messages to be buffered in memory during surges of incoming messages, but increases the total " +
                    "memory used by the processor.")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .defaultValue("10000")
            .required(true)
            .build();

    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name("success")
            .description("success")
            .build();

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;
    private Session session = null;
    private BlockingQueue messageEvents = null;
    //final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(ENDPOINT);
        descriptors.add(MAX_MESSAGE_QUEUE_SIZE);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS_RELATIONSHIP);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnStopped
    public void stopWebSocketListener() {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "NiFi ListenWebSocket stopped ..."));
            session.notify();
            //executor.shutdownNow();
        }
        catch (Exception ex) {
        }

    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

        ClientManager client = ClientManager.createClient();
        messageEvents = new LinkedBlockingQueue(context.getProperty(MAX_MESSAGE_QUEUE_SIZE).asInteger());

        try {
            final WsClientEndpoint ws_Client = new WsClientEndpoint(messageEvents);
            session = client.connectToServer(ws_Client, new URI(context.getProperty(ENDPOINT).getValue()));

            //Executor executor = Executors.newSingleThreadExecutor();
            //executor.execute(new Runnable() {
            //    @Override
            //    public void run() {
            //        ws_Client.Start();
            //    }
            //});

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {

        FlowFile flowFile = session.create();
        if ( flowFile == null ) {
            return;
        }
        // TODO implement

        flowFile = session.write(flowFile, new StreamCallback() {
            @Override
            public void process(InputStream inputStream, OutputStream outputStream) throws IOException {
                try {
                    outputStream.write(messageEvents.take().toString().getBytes());
                }
                catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        });
        session.transfer(flowFile, SUCCESS_RELATIONSHIP);
    }
}

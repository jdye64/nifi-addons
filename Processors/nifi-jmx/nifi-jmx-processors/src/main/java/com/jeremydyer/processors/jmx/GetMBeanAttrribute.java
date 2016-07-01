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
package com.jeremydyer.processors.jmx;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.json.JSONObject;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@Tags({"jmx", "mbean", "attribute"})
@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
@CapabilityDescription("Reads an MBean attribute value from an JMX MBean server connection")
public class GetMBeanAttrribute extends AbstractProcessor {

    public static final PropertyDescriptor JMX_SERVER_HOST = new PropertyDescriptor
            .Builder().name("JMX Server Host")
            .description("JMX Server Host")
            .defaultValue("localhost")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor JMX_SERVER_PORT = new PropertyDescriptor
            .Builder().name("JMX Server Port")
            .description("JMX Server Port")
            .defaultValue("5000")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    public static final PropertyDescriptor MBEAN_OBJECT_NAME = new PropertyDescriptor
            .Builder().name("MBean ObjectName")
            .description("JMX MBean ObjectName")
            .defaultValue("metrics:name=com.jeremydyer.resource.DummyResource.getSearchCache")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MBEAN_ATTRIBUTE_NAME = new PropertyDescriptor
            .Builder().name("MBean Attribute Name")
            .description("MBean Attribute Name")
            .defaultValue("Max")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("successfully captured jmx metrics")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("failure occured while capturing jmx metrics")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(JMX_SERVER_HOST);
        descriptors.add(JMX_SERVER_PORT);
        descriptors.add(MBEAN_OBJECT_NAME);
        descriptors.add(MBEAN_ATTRIBUTE_NAME);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
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

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {

        try {

            FlowFile ff = session.write(session.create(), new OutputStreamCallback() {
                @Override
                public void process(OutputStream outputStream) throws IOException {
                    try {
                        JMXServiceURL url =
                                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"
                                        + context.getProperty(JMX_SERVER_HOST).evaluateAttributeExpressions().getValue()
                                        + ":"
                                        + context.getProperty(JMX_SERVER_PORT).evaluateAttributeExpressions().getValue()
                                        + "/jmxrmi");

                        JMXConnector jmxConnector = JMXConnectorFactory.connect(url);
                        MBeanServerConnection mbeanServerConnection = jmxConnector.getMBeanServerConnection();

                        ObjectName mbeanName = new ObjectName(context.getProperty(MBEAN_OBJECT_NAME).getValue());
                        Object obj = mbeanServerConnection.getAttribute(mbeanName, context.getProperty(MBEAN_ATTRIBUTE_NAME).getValue());

                        JSONObject jsonObject = new JSONObject();

                        //Builds the JSON that will be emitted.
                        JSONObject mbean = new JSONObject();
                        mbean = mbean.put(context.getProperty(MBEAN_ATTRIBUTE_NAME).getValue(), obj.toString());

                        jsonObject = jsonObject.put(context.getProperty(MBEAN_OBJECT_NAME).getValue(), mbean);

                        outputStream.write(jsonObject.toString().getBytes());

                        //close the connection
                        jmxConnector.close();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        session.transfer(session.create(), REL_FAILURE);
                    }
                }
            });

            session.transfer(ff, REL_SUCCESS);

        } catch (Exception ex) {
            ex.printStackTrace();
            session.transfer(session.create(), REL_FAILURE);
        }
    }
}

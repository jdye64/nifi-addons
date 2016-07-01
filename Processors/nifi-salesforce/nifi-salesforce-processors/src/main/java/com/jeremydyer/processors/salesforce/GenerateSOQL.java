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
package com.jeremydyer.processors.salesforce;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


@Tags({"salesforce", "generate", "SOQL"})
@CapabilityDescription("Examines the contents of a Salesforce.com SObject describe and generates a REST API 'queryAll' " +
        "request to pull all changes for that SObject that have occured since the last time that the processer was ran.")
public class GenerateSOQL extends AbstractProcessor {

    public static final PropertyDescriptor SALESFORCE_SERVER_INSTANCE = new PropertyDescriptor
            .Builder().name("Salesforce.com Server Instance")
            .description("Your Salesforce.com server instance for using the REST API")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("https://cs20.salesforce.com")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor LAST_SYNC_TIME = new PropertyDescriptor
            .Builder().name("Salesforce.com SObject Last Sync Time")
            .description(("The value used for 'SYSTEMMODDATE' in the SOQL query. This value should be set to the last time that" +
                    " this process was ran. This value is used to get all Salesforce.com activity that has happened since" +
                    " the last time the ingestion was ran"))
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("2016-03-21T20:00:06.000Z")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("SOQL was successfully created")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(SALESFORCE_SERVER_INSTANCE);
        descriptors.add(LAST_SYNC_TIME);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
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

    private static String TABLE_NAME = "Account";

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        final AtomicReference<String> query_url = new AtomicReference<>();

        session.read(flowFile, new InputStreamCallback() {
            @Override
            public void process(InputStream inputStream) throws IOException {
                String jsonString = IOUtils.toString(inputStream);
                JSONObject json = new JSONObject(jsonString);

                JSONArray fields = json.getJSONArray("fields");

                StringBuffer buffer = new StringBuffer();
                buffer.append(context.getProperty(SALESFORCE_SERVER_INSTANCE).evaluateAttributeExpressions(flowFile).getValue());
                buffer.append("/services/data/v36.0/queryAll/?q=");
                buffer.append("SELECT ");

                //Loops through the fields and builds the SOQL
                for (int i = 0; i < fields.length() - 1; i++) {
                    buffer.append(fields.getJSONObject(i).getString("name"));
                    buffer.append(",");
                }

                //Append the last field name
                buffer.append(fields.getJSONObject(fields.length() - 1).getString("name"));

                buffer.append(" FROM " + TABLE_NAME);
                buffer.append(" WHERE SYSTEMMODSTAMP > ");
                buffer.append(context.getProperty(LAST_SYNC_TIME).evaluateAttributeExpressions(flowFile).getValue());
                buffer.append(" order by SYSTEMMODSTAMP asc");

                String soql = buffer.toString();
                //Replace all spaces with + as required by Salesforce
                soql = soql.replace(" ", "+");

                query_url.set(soql);
            }
        });

        FlowFile ff = session.putAttribute(flowFile, "SOQL_QUERY_URL", query_url.get());

        session.transfer(ff, REL_SUCCESS);
    }
}

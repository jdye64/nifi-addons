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

package com.jeremydyer.processors.salesforce.sobject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import com.jeremydyer.nifi.salesforce.SalesforceUserPassAuthentication;
import com.jeremydyer.processors.salesforce.base.AbstractSalesforceRESTOperation;

@Tags({"salesforce", "deleted", "sobject"})
@CapabilityDescription("Retrieves the list of individual records that have been deleted within the given timespan for the specified object. " +
        "SObject Get Deleted is available in API version 29.0 and later")
@SeeAlso({SObjectGetUpdatedProcessor.class})
public class SObjectGetDeletedProcessor
    extends AbstractSalesforceRESTOperation {

    //https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_getdeleted.htm

    private static final String SALESFORCE_OP = "sobject";

    public static final PropertyDescriptor SOBJECT_NAME = new PropertyDescriptor
            .Builder().name("SObject that will be interrogated for deleted records")
            .description("Salesforce SObject name that we are looking for deleted objects for.")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(true)
            .required(true)
            .build();

    public static final PropertyDescriptor START_DATE = new PropertyDescriptor
            .Builder().name("SObject deleted start date")
            .description("ISO 8601 formatted start date for looking for deleted records.")
            .required(true)
            .expressionLanguageSupported(true)
            .build();

    public static final PropertyDescriptor END_DATE = new PropertyDescriptor
            .Builder().name("SObject deleted end date")
            .description("ISO 8601 formatted end date for looking for deleted records.")
            .required(true)
            .expressionLanguageSupported(true)
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(SALESFORCE_AUTH_SERVICE);
        descriptors.add(SOBJECT_NAME);
        descriptors.add(START_DATE);
        descriptors.add(END_DATE);
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
        final FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        final SalesforceUserPassAuthentication sfAuthService = context.getProperty(SALESFORCE_AUTH_SERVICE)
                .asControllerService(SalesforceUserPassAuthentication.class);


        try {

            String endpoint = SALESFORCE_OP + "/" + context.getProperty(SOBJECT_NAME).evaluateAttributeExpressions().getValue() + "/deleted/?start="
                    + context.getProperty(START_DATE).evaluateAttributeExpressions().getValue() + "&end=" + context.getProperty(END_DATE).evaluateAttributeExpressions().getValue();


            final String responseJson = sendGet(sfAuthService.getSalesforceAccessToken(), RESPONSE_JSON, generateSalesforceURL(endpoint));

            FlowFile ff = session.write(flowFile, new OutputStreamCallback() {
                @Override
                public void process(OutputStream outputStream) throws IOException {
                    outputStream.write(responseJson.getBytes());
                }
            });
            session.transfer(ff, REL_SUCCESS);
        } catch (Exception ex) {
            getLogger().error(ex.getMessage());
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}

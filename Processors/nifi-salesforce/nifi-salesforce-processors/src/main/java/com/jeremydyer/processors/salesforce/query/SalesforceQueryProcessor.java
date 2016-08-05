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

package com.jeremydyer.processors.salesforce.query;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
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

@Tags({"salesforce", "soql", "sobject", "query"})
@CapabilityDescription("Executes the specified SOQL query")
public class SalesforceQueryProcessor
    extends AbstractSalesforceRESTOperation {

    //https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_query.htm

    private static final String SALESFORCE_OP = "query";

    public static final PropertyDescriptor SOQL = new PropertyDescriptor
            .Builder().name("SOQL Query")
            .description("Salesforce.com SOQL that will be perform on the Salesforce.com instance.")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(true)
            .required(true)
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(SALESFORCE_AUTH_SERVICE);
        descriptors.add(SOQL);
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

            String endpoint = SALESFORCE_OP + "/?q=" + context.getProperty(SOQL).evaluateAttributeExpressions().getValue();
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

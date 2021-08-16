package com.jeremydyer.processors.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
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
import org.apache.nifi.processor.util.StandardValidators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 12/6/17.
 */

@Tags({"delay", "wait", "flowfile"})
@CapabilityDescription("Delays a FlowFile from continuing along its execution path for a user defined period of time. Once" +
        " the period of time has elasped the FlowFile will continue on its path.")
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@WritesAttributes({@WritesAttribute(attribute="delay.timestamp", description="timestamp placed on a flowfile to understand its starting tick time" +
        " once the delay period has grown greater than this value the FlowFile will be allowed to continue along its normal flow.")})
@ReadsAttributes({@ReadsAttribute(attribute="delay.timestamp", description="timestamp placed on a flowfile to understand its starting tick time" +
        " once the delay period has grown greater than this value the FlowFile will be allowed to continue along its normal flow.")})
public class FlowFileDelayProcessor
    extends AbstractProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowFileDelayProcessor.class);

    private final String TIME_ATTRIBUTE = "delay.timestamp";

    public static final PropertyDescriptor DELAY_TIME_MS = new PropertyDescriptor
            .Builder().name("Delay Time")
            .description("Number of milliseconds that this processor should delay files before they are allowed to continue through")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.POSITIVE_LONG_VALIDATOR)
            .build();

    public static final Relationship REL_DELAY = new Relationship.Builder()
            .name("delay")
            .description("The FlowFile has not yet reached its delay period")
            .build();

    public static final Relationship REL_READY = new Relationship.Builder()
            .name("ready")
            .description("the Flow")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(DELAY_TIME_MS);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_DELAY);
        relationships.add(REL_READY);
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
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile ff = session.get();
        if (ff == null) {
            return;
        }

        String delay = ff.getAttribute(TIME_ATTRIBUTE);
        if (delay == null) {
            // This is the first time that the file has entered this processor so we need to create and place
            // the initial timestamp in the flowfile's attributes.
            ff = session.putAttribute(ff, TIME_ATTRIBUTE, String.valueOf(System.currentTimeMillis()));
            ff = session.penalize(ff);
            session.transfer(ff, REL_DELAY);
        } else {
            Long delayTimestamp = new Long(delay);
            if ((System.currentTimeMillis() - delayTimestamp)
                    > context.getProperty(DELAY_TIME_MS).evaluateAttributeExpressions().asLong()) {
                session.transfer(ff, REL_READY);
            } else {
                ff = session.penalize(ff);
                session.transfer(ff, REL_DELAY);
            }
        }
    }
}

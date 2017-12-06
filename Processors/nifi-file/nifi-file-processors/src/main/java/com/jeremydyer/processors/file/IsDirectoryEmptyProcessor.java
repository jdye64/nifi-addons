package com.jeremydyer.processors.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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



@Tags({"directory", "empty", "check"})
@CapabilityDescription("Checks a directory to determine if the directory is empty of not. Properties can be set to determine" +
        " if things such as hidden files will be considered when determining if the directory is empty or not.")
public class IsDirectoryEmptyProcessor
    extends AbstractProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(IsDirectoryEmptyProcessor.class);

    public static final PropertyDescriptor DIRECTORY = new PropertyDescriptor
            .Builder().name("Directory")
            .description("Directory to check")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_EMPTY = new Relationship.Builder()
            .name("empty")
            .description("")
            .build();

    public static final Relationship REL_NOT_EMPTY = new Relationship.Builder()
            .name("not empty")
            .description("Failure encountered while deleting file")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("A problem occured while attempting to check the directory for files")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(DIRECTORY);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_EMPTY);
        relationships.add(REL_NOT_EMPTY);
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
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile ff = session.get();

        if (ff == null) {
            // No incoming Flowfile. No big deal but lets create one.
            ff = session.create();
        }

        String dirName = context.getProperty(DIRECTORY).evaluateAttributeExpressions().getValue();
        File dir = new File(dirName);

        if (dir.exists()) {
            // Check for files in the directory.
            String[] files = dir.list();
            if (files != null && files.length > 0) {
                // Directory is NOT empty
                session.transfer(ff, REL_NOT_EMPTY);
            } else {
                // Directory is empty
                session.transfer(ff, REL_EMPTY);
            }
        } else {
            // The directory does not exist so we need to error here.
            session.transfer(ff, REL_FAILURE);
        }
    }
}

package com.jeremydyer.processors.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
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
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import com.jeremydyer.processors.file.parser.FilePartByRegEx;

@Tags({"zip", "entry", "reader"})
@CapabilityDescription("Reads the entries in a zip file")
@WritesAttributes({@WritesAttribute(attribute="file.part.value", description="")})
public class ZipEntryReader
    extends AbstractProcessor {

    public static final PropertyDescriptor OCCURRENCE = new PropertyDescriptor
            .Builder().name("Value Occurrence Index")
            .description("Value Occurrence Index")
            .required(true)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();

    public static final PropertyDescriptor REGEX = new PropertyDescriptor
            .Builder().name("RegEx")
            .description("RegEx")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor REGEX_GROUP_SUPPORT = new PropertyDescriptor
            .Builder().name("RegEx Group Support")
            .description("RegEx Group Support")
            .required(true)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Successfully deleted file")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Failure encountered while deleting file")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        this.descriptors = Collections.unmodifiableList(descriptors);
        descriptors.add(OCCURRENCE);
        descriptors.add(REGEX);
        descriptors.add(REGEX_GROUP_SUPPORT);

        final Set<Relationship> relationships = new HashSet<>();
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
        FlowFile flowFile = session.get();

        if ( flowFile == null ) {
            return;
        }

        final String regex = context.getProperty(REGEX).getValue();
        final Boolean regexGroupSupport = Boolean.parseBoolean(context.getProperty(REGEX_GROUP_SUPPORT).getValue());
        final Integer occurrence = Integer.parseInt(context.getProperty(OCCURRENCE).getValue());

        // Loops through all of the Zip entries
        try {
            final AtomicReference<String> filePartRef = new AtomicReference<>();

            session.read(flowFile, new InputStreamCallback() {

                @Override
                public void process(InputStream inputStream) throws IOException {

                    ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                    ZipEntry zipEntry = null;

                    String filePart = null;

                    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                        FilePartByRegEx fp = new FilePartByRegEx();
                        fp.setOccurrence(occurrence);
                        fp.setRegexGroupSupport(regexGroupSupport);
                        fp.setRegex(regex);

                        byte[] entry = IOUtils.toByteArray(zipInputStream);
                        ByteArrayInputStream bais = new ByteArrayInputStream(entry);
                        fp.setInputStream(bais);

                        //Check for a located value and if found update the global filename
                        if ((filePart = fp.getValue()) != null) {
                            //found a value
                            break;
                        }
                    }

                    if (filePart != null) {
                        //Found something ....
                        filePartRef.set(filePart);
                    }
                }
            });

            if (filePartRef.get() != null) {
                flowFile = session.putAttribute(flowFile, "found", "true");
                flowFile = session.putAttribute(flowFile, "file.part.value", filePartRef.get());
            } else {
                flowFile = session.putAttribute(flowFile, "found", "false");
            }

            session.transfer(flowFile, REL_SUCCESS);

        } catch (Exception ex) {
            getLogger().error(ex.getMessage());
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}

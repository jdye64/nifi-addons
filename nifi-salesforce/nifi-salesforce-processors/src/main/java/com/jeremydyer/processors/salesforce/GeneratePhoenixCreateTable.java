package com.jeremydyer.processors.salesforce;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;


@Tags({"salesforce", "phoenix"})
@CapabilityDescription("Provide a description")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class GeneratePhoenixCreateTable extends AbstractProcessor {

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("SOQL was successfully created")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
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

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        FlowFile ff = session.write(flowFile, new StreamCallback() {
            @Override
            public void process(InputStream inputStream, OutputStream outputStream) throws IOException {
                String jsonString = IOUtils.toString(inputStream);
                JSONArray fields = new JSONArray(jsonString);

                StringBuffer buffer = new StringBuffer();
                buffer.append("CREATE TABLE ");
                buffer.append("SALESFORCE.");
                buffer.append(flowFile.getAttribute("sobject_name"));
                buffer.append("(");

                //Loops through the fields
                for (int i = 0; i < fields.length() - 1; i++) {
                    JSONObject field = fields.getJSONObject(i);
                    buffer.append(field.getString("name") + " ");


                    //Inspects the type
                    String type = field.getString("type");
                    String TYPE = "VARCHAR";

                    if (type.equalsIgnoreCase("ID")) {
                        TYPE = TYPE + " PRIMARY KEY";
                    } else if (type.equalsIgnoreCase("string")) {
                        //Do nothing just keep it as varchar. Just putting here for reference.
                    } else if (type.equalsIgnoreCase("picklist")) {
                        //Make a phoenix array of VARCHAR to store all of the types
                        TYPE = TYPE + "[]";
                    } else if (type.equalsIgnoreCase("currency")) {
                        TYPE = "DOUBLE";
                    } else if (type.equalsIgnoreCase("double")) {
                        TYPE = "DOUBLE";
                    }

                    buffer.append(TYPE);
                    buffer.append(",");
                }

                //Appends the last value.
                buffer.append(fields.getJSONObject(fields.length() - 1).getString("name") + " VARCHAR");
                buffer.append(")");

                String cql = buffer.toString();
                outputStream.write(cql.getBytes());
            }
        });

        session.transfer(ff, REL_SUCCESS);
    }
}
package com.jeremydyer.processors.docker;

import org.apache.commons.lang.StringUtils;
import org.apache.nifi.processor.AbstractProcessor;

/**
 * Created by jdyer on 4/22/16.
 */
public abstract class AbstractDockerProcessor
    extends AbstractProcessor {

    protected String[] commaDelimitedToStringArray(String commaValue) {
        return StringUtils.split(commaValue, ",");
    }
}

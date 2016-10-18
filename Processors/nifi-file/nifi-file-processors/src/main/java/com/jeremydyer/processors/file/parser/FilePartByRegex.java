package com.jeremydyer.processors.file.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * This class is designed to pull the 'nth' occurrence of a RegEx check from a file.
 *
 * An example would be the need to extract a control date from a file.
 *
 * Created by dstreev on 2016-09-30.
 */
public class FilePartByRegEx {

    private int occurrence = 1;
    private int occurrenceCount = 1;

    private String regex = null;
    // Support for pulling a regex group element from the matched value.
    private boolean regexGroupSupport = false;
    private int groupItemOccurrence = 1;

    private InputStream inputStream = null;

    public int getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(int occurrence) {
        this.occurrence = occurrence;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public boolean isRegexGroupSupport() {
        return regexGroupSupport;
    }

    public void setRegexGroupSupport(boolean regexGroupSupport) {
        this.regexGroupSupport = regexGroupSupport;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public FilePartByRegEx() {
    }

    public FilePartByRegEx(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getValue() {
        String rtn = null;
        // Check that we have the minimum required elements
        if (inputStream == null || regex == null) {
            return null;
        }

        // Build the Compile RegEx Pattern
        Pattern regExPattern = Pattern.compile(regex);
        occurrenceCount = 1;

        // Wrap InputStream in a BufferedReader.
        BufferedReader inBuff = new BufferedReader(new InputStreamReader(inputStream));

        String line = null;
        try {
            // Iterate till the end of the buffer or when rtn isn't null.
            while ((line = inBuff.readLine()) != null && rtn == null) {
                rtn = getMatchedValue(regExPattern, line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return rtn;
    }

    private String getMatchedValue(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        String rtn = null;

        // If we have a match...
        while (matcher.find()) {
            if (occurrenceCount == occurrence) {
                // Found the occurrence we're looking for.
                // Now check if we're looking for RegEx Group Support and
                // return back the part, or the whole value.
                if (regexGroupSupport) {
                    rtn = new String(matcher.group(1));
                } else {
                    rtn = new String(value);
                }
            } else {
                // Increase Count and do it again.
                occurrenceCount++;
            }
        }
        return rtn;
    }
}
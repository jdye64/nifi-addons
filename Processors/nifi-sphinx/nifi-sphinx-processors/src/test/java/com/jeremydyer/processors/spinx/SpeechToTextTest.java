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
package com.jeremydyer.processors.spinx;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;


public class SpeechToTextTest {

    private TestRunner testRunner;

    @Before
    public void init() {
        testRunner = TestRunners.newTestRunner(SpeechToText.class);
    }

    @Test
    public void testProcessor() throws IOException {

        testRunner.enqueue(new File("src/test/resources/audio/test.wav").toPath());
        testRunner.run();

        List<MockFlowFile> ffs = testRunner.getFlowFilesForRelationship(SpeechToText.REL_SUCCESS);
        assertTrue(ffs.size() == 1);
        String data = new String(testRunner.getContentAsByteArray(ffs.get(0)));
        System.out.println("Data: " + data);
    }

}

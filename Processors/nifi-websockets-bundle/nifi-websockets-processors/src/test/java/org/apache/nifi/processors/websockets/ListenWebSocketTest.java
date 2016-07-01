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
package org.apache.nifi.processors.websockets;

import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;


public class ListenWebSocketTest {

    private TestRunner testRunner;

    @Before
    public void init() {
        testRunner = TestRunners.newTestRunner(ListenWebSocket.class);
    }

    @Test
    public void testProcessor() {
        final TestRunner runner = TestRunners.newTestRunner(new ListenWebSocket());
        //runner.setProperty(ConvertJSONtoCSV.DELIMITER, "|");
        runner.setProperty(ListenWebSocket.ENDPOINT,"ws://localhost:8025/websockets/test");
        //runner.setProperty(ConvertJSONtoCSV.EMPTY_FIELDS, "NULL");
        //runner.enqueue(Paths.get("src/test/resources/simple.json"));
        runner.run();
        try {
            wait(500000);
        }
        catch (Exception ex) {

        }
        //runner.assertAllFlowFilesTransferred(MyProcessor.MY_RELATIONSHIP);

    }

}

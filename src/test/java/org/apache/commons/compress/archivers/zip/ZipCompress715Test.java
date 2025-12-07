/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.archivers.zip;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.commons.io.function.IOConsumer;
import org.junit.jupiter.api.Test;

/**
 * Tests https://issues.apache.org/jira/browse/COMPRESS-715
 */
class ZipCompress715Test {

    private void assertNextEntry(final ZipArchiveInputStream inputStream, final String expectedName, final int expectedSize) throws IOException {
        final ZipArchiveEntry nextEntry = inputStream.getNextEntry();
        assertEquals(expectedName, nextEntry.getName());
        assertEquals(expectedSize, nextEntry.getSize());
    }

    /**
     * The test fixture unzips OK using {@code UnZip 6.00 of 20 April 2009, by Info-ZIP, with modifications by Apple Inc.}
     * <pre>
     * unzip -lv compress715.zip
     * </pre>
     * <p>
     * returns:
     * </p>
     * <pre>
     * Archive:  compress715.zip
     *  Length   Method    Size  Cmpr    Date    Time   CRC-32   Name
     * --------  ------  ------- ---- ---------- ----- --------  ----
     *   341552  Defl:N    14034  96% 03-18-2024 21:36 88cbd694  50700006_82901717_20240318�?162450_Speichergruppe-14_Monitoring_AMT_M5190M_12394_986197575850.mf4
     *   641560  Defl:N    49549  92% 03-18-2024 21:36 ef0dba37  50700006_82901717_20240318�?162450_Speichergruppe-11_Monitoring_CAN_M5190M_7651_986197575850.mf4
     *    37280  Defl:X    36887   1% 03-18-2024 21:36 ba89ba80  50700006_82901717_20240318�?162450_MEA_5190.dmp
     *   405692  Defl:X   371118   9% 03-18-2024 21:24 1e1402e5  50700006_82901717_20240318�?162450_CFG_5190.ilf
     *   847104  Defl:X   812041   4% 03-18-2024 21:24 d0b8c08d  50700006_82901717_20240318�?162450_CFG_5190.zip
     *    11107  Defl:X     2764  75% 03-18-2024 21:36 bb2f0f57  50700006_82901717_20240318�?162450_MEA_5190.LOG
     * --------          -------  ---                            -------
     *  2284295          1286393  44%                            6 files
     * </pre>
     *
     * @throws IOException Thrown if the test fails.
     */
    @Test
    void testCompress715() throws IOException {
        final String fixture = "src/test/resources/org/apache/commons/compress/COMPRESS-715/compress715.zip";
        try (ZipArchiveInputStream inputStream = ZipArchiveInputStream.builder().setPath(fixture).get()) {
            inputStream.forEach(IOConsumer.noop());
        }
        try (ZipArchiveInputStream inputStream = ZipArchiveInputStream.builder().setPath(fixture).get()) {
            assertNextEntry(inputStream, "50700006_82901717_20240318–162450_Speichergruppe-14_Monitoring_AMT_M5190M_12394_986197575850.mf4", 341_552);
            assertNextEntry(inputStream, "50700006_82901717_20240318–162450_Speichergruppe-11_Monitoring_CAN_M5190M_7651_986197575850.mf4", 641_560);
            assertNextEntry(inputStream, "50700006_82901717_20240318–162450_MEA_5190.dmp", 37_280);
            assertNextEntry(inputStream, "50700006_82901717_20240318–162450_CFG_5190.ilf", 405_692);
            assertNextEntry(inputStream, "50700006_82901717_20240318–162450_CFG_5190.zip", 847_104);
            assertNextEntry(inputStream, "50700006_82901717_20240318–162450_MEA_5190.LOG", 11_107);
            assertNull(inputStream.getNextEntry());
        }
    }
}

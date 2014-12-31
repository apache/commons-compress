/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.zip;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@SuppressWarnings("OctalInteger")
public class ParallelScatterZipCreatorTest {

    @Test
    public void concurrent()
            throws Exception {
        File result = File.createTempFile("parallelScatterGather1", "");
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(result);
        zos.setEncoding("UTF-8");
        ParallelScatterZipCreator zipCreator = new ParallelScatterZipCreator();

        Map<String, byte[]> entries = writeEntries(zipCreator);
        zipCreator.writeTo(zos);
        zos.close();

        removeEntriesFoundInZipFile(result, entries);
        assertTrue(entries.size() == 0);
        assertNotNull( zipCreator.getStatisticsMessage());
    }

    private void removeEntriesFoundInZipFile(File result, Map<String, byte[]> entries) throws IOException {
        ZipFile zf = new ZipFile(result);
        Enumeration<ZipArchiveEntry> entriesInPhysicalOrder = zf.getEntriesInPhysicalOrder();
        while (entriesInPhysicalOrder.hasMoreElements()){
            ZipArchiveEntry zipArchiveEntry = entriesInPhysicalOrder.nextElement();
            InputStream inputStream = zf.getInputStream(zipArchiveEntry);
            byte[] actual = IOUtils.toByteArray(inputStream);
            byte[] expected = entries.remove(zipArchiveEntry.getName());
            assertArrayEquals( expected, actual);
        }
        zf.close();
    }

    private Map<String, byte[]> writeEntries(ParallelScatterZipCreator zipCreator) {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        for (int i = 0; i < 10000; i++){
            ZipArchiveEntry za = new ZipArchiveEntry( "file" + i);
            final String payload = "content" + i;
            final byte[] payloadBytes = payload.getBytes();
            entries.put( za.getName(), payloadBytes);
            za.setMethod(ZipArchiveEntry.DEFLATED);
            za.setSize(payload.length());
            za.setUnixMode(UnixStat.FILE_FLAG | 0664);
            zipCreator.addArchiveEntry(za, new InputStreamSupplier() {
                public InputStream get() {
                    return new ByteArrayInputStream(payloadBytes);
                }
            });
        }
        return entries;
    }
}
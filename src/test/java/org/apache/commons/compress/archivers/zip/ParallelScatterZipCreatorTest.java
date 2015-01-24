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

import org.apache.commons.compress.parallel.FileBasedScatterGatherBackingStore;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.parallel.ScatterGatherBackingStore;
import org.apache.commons.compress.parallel.ScatterGatherBackingStoreSupplier;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.commons.compress.AbstractTestCase.tryHardToDelete;
import static org.junit.Assert.*;

@SuppressWarnings("OctalInteger")
public class ParallelScatterZipCreatorTest {

    private final int NUMITEMS = 5000;

    private File result;
    private File tmp;
    
    @After
    public void cleanup() {
        tryHardToDelete(result);
        tryHardToDelete(tmp);
    }

    @Test
    public void concurrent()
            throws Exception {
        result = File.createTempFile("parallelScatterGather1", "");
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

    @Test
    public void callableApi()
            throws Exception {
        result = File.createTempFile("parallelScatterGather2", "");
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(result);
        zos.setEncoding("UTF-8");
        ExecutorService es = Executors.newFixedThreadPool(1);

        ScatterGatherBackingStoreSupplier supp = new ScatterGatherBackingStoreSupplier() {
            public ScatterGatherBackingStore get() throws IOException {
                return new FileBasedScatterGatherBackingStore(tmp = File.createTempFile("parallelscatter", "n1"));
            }
        };

        ParallelScatterZipCreator zipCreator = new ParallelScatterZipCreator(es, supp);
        Map<String, byte[]> entries = writeEntriesAsCallable(zipCreator);
        zipCreator.writeTo(zos);
        zos.close();


        removeEntriesFoundInZipFile(result, entries);
        assertTrue(entries.size() == 0);
        assertNotNull(zipCreator.getStatisticsMessage());
    }

    private void removeEntriesFoundInZipFile(File result, Map<String, byte[]> entries) throws IOException {
        ZipFile zf = new ZipFile(result);
        Enumeration<ZipArchiveEntry> entriesInPhysicalOrder = zf.getEntriesInPhysicalOrder();
        while (entriesInPhysicalOrder.hasMoreElements()){
            ZipArchiveEntry zipArchiveEntry = entriesInPhysicalOrder.nextElement();
            InputStream inputStream = zf.getInputStream(zipArchiveEntry);
            byte[] actual = IOUtils.toByteArray(inputStream);
            byte[] expected = entries.remove(zipArchiveEntry.getName());
            assertArrayEquals( "For " + zipArchiveEntry.getName(),  expected, actual);
        }
        zf.close();
    }

    private Map<String, byte[]> writeEntries(ParallelScatterZipCreator zipCreator) {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        for (int i = 0; i < NUMITEMS; i++){
            final byte[] payloadBytes = ("content" + i).getBytes();
            ZipArchiveEntry za = createZipArchiveEntry(entries, i, payloadBytes);
            zipCreator.addArchiveEntry(za, new InputStreamSupplier() {
                public InputStream get() {
                    return new ByteArrayInputStream(payloadBytes);
                }
            });
        }
        return entries;
    }

    private Map<String, byte[]> writeEntriesAsCallable(ParallelScatterZipCreator zipCreator) {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        for (int i = 0; i < NUMITEMS; i++){
            final byte[] payloadBytes = ("content" + i).getBytes();
            ZipArchiveEntry za = createZipArchiveEntry(entries, i, payloadBytes);
            final Callable<Object> callable = zipCreator.createCallable(za, new InputStreamSupplier() {
                public InputStream get() {
                    return new ByteArrayInputStream(payloadBytes);
                }
            });
            zipCreator.submit(callable);
        }
        return entries;
    }

    private ZipArchiveEntry createZipArchiveEntry(Map<String, byte[]> entries, int i, byte[] payloadBytes) {
        ZipArchiveEntry za = new ZipArchiveEntry( "file" + i);
        entries.put( za.getName(), payloadBytes);
        za.setMethod(ZipArchiveEntry.DEFLATED);
        za.setSize(payloadBytes.length);
        za.setUnixMode(UnixStat.FILE_FLAG | 0664);
        return za;
    }
}

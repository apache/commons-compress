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
import org.apache.commons.compress.parallel.ScatterGatherBackingStoreSupplier;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.apache.commons.compress.AbstractTestCase.tryHardToDelete;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ParallelScatterZipCreatorTest {

    private final int NUMITEMS = 5000;
    private static final long EXPECTED_FILE_SIZE = 1024 * 1024; // 1MB
    private static final int EXPECTED_FILES_NUMBER = 50;

    private File result;
    private File tmp;

    @AfterEach
    public void cleanup() {
        tryHardToDelete(result);
        tryHardToDelete(tmp);
    }

    @Test
    public void concurrent()
            throws Exception {
        result = File.createTempFile("parallelScatterGather1", "");
        final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(result);
        zos.setEncoding("UTF-8");
        final ParallelScatterZipCreator zipCreator = new ParallelScatterZipCreator();

        final Map<String, byte[]> entries = writeEntries(zipCreator);
        zipCreator.writeTo(zos);
        zos.close();
        removeEntriesFoundInZipFile(result, entries);
        assertTrue(entries.isEmpty());
        assertNotNull( zipCreator.getStatisticsMessage());
    }

    @Test
    public void callableApiUsingSubmit() throws Exception {
        result = File.createTempFile("parallelScatterGather2", "");
        callableApi(zipCreator -> zipCreator::submit);
    }

    @Test
    public void callableApiUsingSubmitStreamAwareCallable() throws Exception {
        result = File.createTempFile("parallelScatterGather3", "");
        callableApi(zipCreator -> zipCreator::submitStreamAwareCallable);
    }

    @Test
    public void throwsExceptionWithCompressionLevelTooBig() {
        final int compressLevelTooBig = Deflater.BEST_COMPRESSION + 1;
        final ExecutorService es = Executors.newFixedThreadPool(1);
        final ScatterGatherBackingStoreSupplier supp = () -> new FileBasedScatterGatherBackingStore(tmp = File.createTempFile("parallelscatter", "n1"));

        new ParallelScatterZipCreator(es, supp, compressLevelTooBig);
    }

    @Test
    public void throwsExceptionWithCompressionLevelTooSmall() {
        final int compressLevelTooSmall = Deflater.DEFAULT_COMPRESSION - 1;
        final ExecutorService es = Executors.newFixedThreadPool(1);
        final ScatterGatherBackingStoreSupplier supp = () -> new FileBasedScatterGatherBackingStore(tmp = File.createTempFile("parallelscatter", "n1"));

        new ParallelScatterZipCreator(es, supp, compressLevelTooSmall);
    }

    @Test
    public void callableWithLowestLevelApiUsingSubmit() throws Exception {
        result = File.createTempFile("parallelScatterGather4", "");
        callableApiWithTestFiles(zipCreator -> zipCreator::submit, Deflater.NO_COMPRESSION);
    }

    @Test
    public void callableApiWithHighestLevelUsingSubmitStreamAwareCallable() throws Exception {
        result = File.createTempFile("parallelScatterGather5", "");
        callableApiWithTestFiles(zipCreator -> zipCreator::submitStreamAwareCallable, Deflater.BEST_COMPRESSION);
    }

    private void callableApi(final CallableConsumerSupplier consumerSupplier) throws Exception {
        callableApi(consumerSupplier, Deflater.DEFAULT_COMPRESSION);
    }

    private void callableApi(final CallableConsumerSupplier consumerSupplier, final int compressionLevel) throws Exception {
        final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(result);
        zos.setEncoding("UTF-8");
        final ExecutorService es = Executors.newFixedThreadPool(1);

        final ScatterGatherBackingStoreSupplier supp = () -> new FileBasedScatterGatherBackingStore(tmp = File.createTempFile("parallelscatter", "n1"));

        final ParallelScatterZipCreator zipCreator = new ParallelScatterZipCreator(es, supp, compressionLevel);
        final Map<String, byte[]> entries = writeEntriesAsCallable(zipCreator, consumerSupplier.apply(zipCreator));
        zipCreator.writeTo(zos);
        zos.close();


        removeEntriesFoundInZipFile(result, entries);
        assertTrue(entries.isEmpty());
        assertNotNull(zipCreator.getStatisticsMessage());
    }

    private void callableApiWithTestFiles(final CallableConsumerSupplier consumerSupplier, final int compressionLevel) throws Exception {
        final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(result);
        zos.setEncoding("UTF-8");
        final ExecutorService es = Executors.newFixedThreadPool(1);

        final ScatterGatherBackingStoreSupplier supp = () -> new FileBasedScatterGatherBackingStore(tmp = File.createTempFile("parallelscatter", "n1"));

        final ParallelScatterZipCreator zipCreator = new ParallelScatterZipCreator(es, supp, compressionLevel);
        final Map<String, byte[]> entries = writeTestFilesAsCallable(zipCreator, consumerSupplier.apply(zipCreator));
        zipCreator.writeTo(zos);
        zos.close();

        // validate the content of the compressed files
        try (final ZipFile zf = new ZipFile(result)) {
            final Enumeration<ZipArchiveEntry> entriesInPhysicalOrder = zf.getEntriesInPhysicalOrder();
            while (entriesInPhysicalOrder.hasMoreElements()) {
                final ZipArchiveEntry zipArchiveEntry = entriesInPhysicalOrder.nextElement();
                final InputStream inputStream = zf.getInputStream(zipArchiveEntry);
                final byte[] actual = IOUtils.toByteArray(inputStream);
                final byte[] expected = entries.remove(zipArchiveEntry.getName());
                assertArrayEquals("For " + zipArchiveEntry.getName(), expected, actual);
            }
        }
        assertNotNull(zipCreator.getStatisticsMessage());
    }

    private void removeEntriesFoundInZipFile(final File result, final Map<String, byte[]> entries) throws IOException {
        final ZipFile zf = new ZipFile(result);
        final Enumeration<ZipArchiveEntry> entriesInPhysicalOrder = zf.getEntriesInPhysicalOrder();
        int i = 0;
        while (entriesInPhysicalOrder.hasMoreElements()){
            final ZipArchiveEntry zipArchiveEntry = entriesInPhysicalOrder.nextElement();
            final InputStream inputStream = zf.getInputStream(zipArchiveEntry);
            final byte[] actual = IOUtils.toByteArray(inputStream);
            final byte[] expected = entries.remove(zipArchiveEntry.getName());
            assertArrayEquals( "For " + zipArchiveEntry.getName(),  expected, actual);
            // check order of zip entries vs order of order of addition to the parallel zip creator
            assertEquals( "For " + zipArchiveEntry.getName(),  "file" + i++, zipArchiveEntry.getName());
        }
        zf.close();
    }

    private Map<String, byte[]> writeEntries(final ParallelScatterZipCreator zipCreator) {
        final Map<String, byte[]> entries = new HashMap<>();
        for (int i = 0; i < NUMITEMS; i++){
            final byte[] payloadBytes = ("content" + i).getBytes();
            final ZipArchiveEntry za = createZipArchiveEntry(entries, i, payloadBytes);
            final InputStreamSupplier iss = () -> new ByteArrayInputStream(payloadBytes);
            if (i % 2 == 0) {
                zipCreator.addArchiveEntry(za, iss);
            } else {
                final ZipArchiveEntryRequestSupplier zaSupplier = () -> ZipArchiveEntryRequest.createZipArchiveEntryRequest(za, iss);
                zipCreator.addArchiveEntry(zaSupplier);
            }
        }
        return entries;
    }

    private Map<String, byte[]> writeEntriesAsCallable(final ParallelScatterZipCreator zipCreator,
                                                       final CallableConsumer consumer) {
        final Map<String, byte[]> entries = new HashMap<>();
        for (int i = 0; i < NUMITEMS; i++){
            final byte[] payloadBytes = ("content" + i).getBytes();
            final ZipArchiveEntry za = createZipArchiveEntry(entries, i, payloadBytes);
            final InputStreamSupplier iss = () -> new ByteArrayInputStream(payloadBytes);
            final Callable<ScatterZipOutputStream> callable;
            if (i % 2 == 0) {
                callable = zipCreator.createCallable(za, iss);
            } else {
                final ZipArchiveEntryRequestSupplier zaSupplier = () -> ZipArchiveEntryRequest.createZipArchiveEntryRequest(za, iss);
                callable = zipCreator.createCallable(zaSupplier);
            }

            consumer.accept(callable);
        }
        return entries;
    }

    /**
     * Try to compress the files in src/test/resources with size no bigger than
     * {@value EXPECTED_FILES_NUMBER} and with a mount of files no bigger than
     * {@value EXPECTED_FILES_NUMBER}
     *
     * @param zipCreator The ParallelScatterZipCreator
     * @param consumer   The parallel consumer
     * @return A map using file name as key and file content as value
     * @throws IOException if exceptions occur when opening files
     */
    private Map<String, byte[]> writeTestFilesAsCallable(final ParallelScatterZipCreator zipCreator,
                                                         final CallableConsumer consumer) throws IOException {
        final Map<String, byte[]> entries = new HashMap<>();
        final File baseDir = getFile("");
        int filesCount = 0;
        for (final File file : baseDir.listFiles()) {
            // do not compress too many files
            if (filesCount >= EXPECTED_FILES_NUMBER) {
                break;
            }

            // skip files that are too large
            if (file.isDirectory() || file.length() > EXPECTED_FILE_SIZE) {
                continue;
            }

            entries.put(file.getName(), IOUtils.toByteArray(Files.newInputStream(file.toPath())));

            final ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(file.getName());
            zipArchiveEntry.setMethod(ZipEntry.DEFLATED);
            zipArchiveEntry.setSize(file.length());
            zipArchiveEntry.setUnixMode(UnixStat.FILE_FLAG | 0664);

            final InputStreamSupplier iss = () -> {
                try {
                    return Files.newInputStream(file.toPath());
                } catch (final IOException e) {
                    return null;
                }
            };

            final Callable<ScatterZipOutputStream> callable;
            if (filesCount % 2 == 0) {
                callable = zipCreator.createCallable(zipArchiveEntry, iss);
            } else {
                final ZipArchiveEntryRequestSupplier zaSupplier = () -> ZipArchiveEntryRequest.createZipArchiveEntryRequest(zipArchiveEntry, iss);
                callable = zipCreator.createCallable(zaSupplier);
            }

            consumer.accept(callable);
            filesCount++;
        }
        return entries;
    }

    private ZipArchiveEntry createZipArchiveEntry(final Map<String, byte[]> entries, final int i, final byte[] payloadBytes) {
        final ZipArchiveEntry za = new ZipArchiveEntry( "file" + i);
        entries.put( za.getName(), payloadBytes);
        za.setMethod(ZipEntry.DEFLATED);
        za.setSize(payloadBytes.length);
        za.setUnixMode(UnixStat.FILE_FLAG | 0664);
        return za;
    }

    private interface CallableConsumer {
        void accept(Callable<? extends ScatterZipOutputStream> c);
    }
    private interface CallableConsumerSupplier {
        CallableConsumer apply(ParallelScatterZipCreator zipCreator);
    }
}

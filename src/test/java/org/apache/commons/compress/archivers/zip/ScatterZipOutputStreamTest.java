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
 */
package org.apache.commons.compress.archivers.zip;

import static org.apache.commons.compress.AbstractTestCase.forceDelete;
import static org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest.createZipArchiveEntryRequest;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ScatterZipOutputStreamTest {

    private File scatterFile;
    private File target;

    @AfterEach
    public void cleanup() {
        forceDelete(scatterFile);
        forceDelete(target);
    }

    private InputStreamSupplier createPayloadSupplier(final ByteArrayInputStream payload) {
        return () -> payload;
    }

    @Test
    public void testPutArchiveEntry() throws Exception {
        scatterFile = File.createTempFile("scattertest", ".notzip");
        final byte[] B_PAYLOAD = "RBBBBBBS".getBytes();
        final byte[] A_PAYLOAD = "XAAY".getBytes();
        try (ScatterZipOutputStream scatterZipOutputStream = ScatterZipOutputStream.fileBased(scatterFile)) {

            final ZipArchiveEntry zab = new ZipArchiveEntry("b.txt");
            zab.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload = new ByteArrayInputStream(B_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zab, createPayloadSupplier(payload)));

            final ZipArchiveEntry zae = new ZipArchiveEntry("a.txt");
            zae.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload1 = new ByteArrayInputStream(A_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zae, createPayloadSupplier(payload1)));

            target = File.createTempFile("scattertest", ".zip");
            try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(target)) {
                scatterZipOutputStream.writeTo(outputStream);
            }
        }

        try (ZipFile zf = new ZipFile(target)) {
            final ZipArchiveEntry bEntry = zf.getEntries("b.txt").iterator().next();
            assertEquals(8, bEntry.getSize());
            try (InputStream inputStream = zf.getInputStream(bEntry)) {
                assertArrayEquals(B_PAYLOAD, IOUtils.toByteArray(inputStream));
            }

            final ZipArchiveEntry aEntry = zf.getEntries("a.txt").iterator().next();
            assertEquals(4, aEntry.getSize());
            try (InputStream inputStream = zf.getInputStream(aEntry)) {
                assertArrayEquals(A_PAYLOAD, IOUtils.toByteArray(inputStream));
            }
        }
    }
}

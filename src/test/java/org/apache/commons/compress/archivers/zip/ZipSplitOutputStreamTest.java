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

import org.apache.commons.compress.AbstractTestCase;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ZipSplitOutputStreamTest extends AbstractTestCase {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void throwsExceptionIfSplitSizeIsTooSmall() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        new ZipSplitOutputStream(File.createTempFile("temp", "zip"), (64 * 1024 - 1));
    }

    @Test
    public void throwsExceptionIfSplitSizeIsTooLarge() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        new ZipSplitOutputStream(File.createTempFile("temp", "zip"), (4 * 1024 * 1024 * 1024L));
    }

    @Test
    public void throwsIfUnsplittableSizeLargerThanSplitSize() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        final long splitSize = 100 * 1024;
        final ZipSplitOutputStream output = new ZipSplitOutputStream(File.createTempFile("temp", "zip"), splitSize);
        output.prepareToWriteUnsplittableContent(splitSize + 1);
    }

    @Test
    public void splitZipBeginsWithZipSplitSignature() throws IOException {
        final File tempFile = File.createTempFile("temp", "zip");
        new ZipSplitOutputStream(tempFile, 100 * 1024L);

        final InputStream inputStream = Files.newInputStream(tempFile.toPath());
        final byte[] buffer = new byte[4];
        inputStream.read(buffer);

        Assert.assertEquals(ByteBuffer.wrap(ZipArchiveOutputStream.DD_SIG).getInt(), ByteBuffer.wrap(buffer).getInt());
    }

    @Test
    public void testCreateSplittedFiles() throws IOException {
        final File testOutputFile = new File(dir, "testCreateSplittedFiles.zip");
        final int splitSize = 100 * 1024; /* 100KB */
        final ZipSplitOutputStream zipSplitOutputStream = new ZipSplitOutputStream(testOutputFile, splitSize);

        final File fileToTest = getFile("COMPRESS-477/split_zip_created_by_zip/zip_to_compare_created_by_zip.zip");
        final InputStream inputStream = Files.newInputStream(fileToTest.toPath());
        final byte[] buffer = new byte[4096];
        int readLen;

        while ((readLen = inputStream.read(buffer)) > 0) {
            zipSplitOutputStream.write(buffer, 0, readLen);
        }

        inputStream.close();
        zipSplitOutputStream.close();

        File zipFile = new File(dir.getPath(), "testCreateSplittedFiles.z01");
        Assert.assertEquals(zipFile.length(), splitSize);

        zipFile = new File(dir.getPath(), "testCreateSplittedFiles.z02");
        Assert.assertEquals(zipFile.length(), splitSize);

        zipFile = new File(dir.getPath(), "testCreateSplittedFiles.z03");
        Assert.assertEquals(zipFile.length(), splitSize);

        zipFile = new File(dir.getPath(), "testCreateSplittedFiles.z04");
        Assert.assertEquals(zipFile.length(), splitSize);

        zipFile = new File(dir.getPath(), "testCreateSplittedFiles.z05");
        Assert.assertEquals(zipFile.length(), splitSize);

        zipFile = new File(dir.getPath(), "testCreateSplittedFiles.zip");
        Assert.assertEquals(zipFile.length(), (fileToTest.length() + 4 - splitSize * 5));
    }
}

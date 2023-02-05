/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.xz;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class XZCompressorInputStreamTest {
    private void multiByteReadConsistentlyReturnsMinusOneAtEof(final boolean decompressConcatenated) throws IOException {
        final File input = getFile("bla.tar.xz");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath());
                XZCompressorInputStream in = new XZCompressorInputStream(is, decompressConcatenated);) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofDecompressConcatenated() throws IOException {
        multiByteReadConsistentlyReturnsMinusOneAtEof(true);
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofNoDecompressConcatenated() throws IOException {
        multiByteReadConsistentlyReturnsMinusOneAtEof(false);
    }

    @Test
    public void redundantTestOfAlmostDeprecatedMatchesMethod() {
        final byte[] data = { (byte) 0xFD, '7', 'z', 'X', 'Z', '\0' };
        assertFalse(XZCompressorInputStream.matches(data, 5));
        assertTrue(XZCompressorInputStream.matches(data, 6));
        assertTrue(XZCompressorInputStream.matches(data, 7));
        data[5] = '0';
        assertFalse(XZCompressorInputStream.matches(data, 6));
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(final boolean decompressConcatenated) throws IOException {
        final File input = getFile("bla.tar.xz");
        try (InputStream is = Files.newInputStream(input.toPath());
                XZCompressorInputStream in = new XZCompressorInputStream(is, decompressConcatenated)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofDecompressConcatenated() throws IOException {
        singleByteReadConsistentlyReturnsMinusOneAtEof(true);
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofNoDecompressConcatenated() throws IOException {
        singleByteReadConsistentlyReturnsMinusOneAtEof(false);
    }

}

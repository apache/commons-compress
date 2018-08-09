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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class XZCompressorInputStreamTest {
    @Test
    public void redundantTestOfAlmostDeprecatedMatchesMethod() {
        final byte[] data = {
            (byte) 0xFD, '7', 'z', 'X', 'Z', '\0'
        };
        Assert.assertFalse(XZCompressorInputStream.matches(data, 5));
        Assert.assertTrue(XZCompressorInputStream.matches(data, 6));
        Assert.assertTrue(XZCompressorInputStream.matches(data, 7));
        data[5] = '0';
        Assert.assertFalse(XZCompressorInputStream.matches(data, 6));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofNoDecompressConcatenated() throws IOException {
        singleByteReadConsistentlyReturnsMinusOneAtEof(false);
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofDecompressConcatenated() throws IOException {
        singleByteReadConsistentlyReturnsMinusOneAtEof(true);
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(boolean decompressConcatenated) throws IOException {
        final File input = getFile("bla.tar.xz");
        try (InputStream is = new FileInputStream(input)) {
            final XZCompressorInputStream in =
                new XZCompressorInputStream(is, decompressConcatenated);
            IOUtils.toByteArray(in);
            Assert.assertEquals(-1, in.read());
            Assert.assertEquals(-1, in.read());
            in.close();
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofNoDecompressConcatenated() throws IOException {
        multiByteReadConsistentlyReturnsMinusOneAtEof(false);
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofDecompressConcatenated() throws IOException {
        multiByteReadConsistentlyReturnsMinusOneAtEof(true);
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(boolean decompressConcatenated) throws IOException {
        final File input = getFile("bla.tar.xz");
        byte[] buf = new byte[2];
        try (InputStream is = new FileInputStream(input)) {
            final XZCompressorInputStream in =
                new XZCompressorInputStream(is, decompressConcatenated);
            IOUtils.toByteArray(in);
            Assert.assertEquals(-1, in.read(buf));
            Assert.assertEquals(-1, in.read(buf));
            in.close();
        }
    }

}

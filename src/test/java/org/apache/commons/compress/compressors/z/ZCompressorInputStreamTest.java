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
package org.apache.commons.compress.compressors.z;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.util.Collections;
import org.apache.commons.compress.utils.IOUtils;

import static org.apache.commons.compress.AbstractTestCase.getFile;

/**
 * Unit tests for class {@link ZCompressorInputStream}.
 *
 * @date 16.06.2017
 * @see ZCompressorInputStream
 **/
public class ZCompressorInputStreamTest {


    @Test
    public void testFailsToCreateZCompressorInputStreamAndThrowsIOException() throws IOException {
        final SequenceInputStream sequenceInputStream = new SequenceInputStream(Collections.emptyEnumeration());
        final ZCompressorInputStream zCompressorInputStream = new ZCompressorInputStream(sequenceInputStream);
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.Z");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final ZCompressorInputStream in =
                    new ZCompressorInputStream(is);
            IOUtils.toByteArray(in);
            Assert.assertEquals(-1, in.read());
            Assert.assertEquals(-1, in.read());
            in.close();
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.Z");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final ZCompressorInputStream in =
                    new ZCompressorInputStream(is);
            IOUtils.toByteArray(in);
            Assert.assertEquals(-1, in.read(buf));
            Assert.assertEquals(-1, in.read(buf));
            in.close();
        }
    }

}

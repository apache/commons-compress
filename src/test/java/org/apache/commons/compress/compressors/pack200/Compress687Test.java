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

package org.apache.commons.compress.compressors.pack200;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Test;

/**
 * Tests <a href="https://issues.apache.org/jira/browse/COMPRESS-687">COMPRESS-687</a>.
 */
class Compress687Test {

    private static final String FIXTURE = "org/apache/commons/compress/COMPRESS-687/test-issue.7z";
    private static final int BUFFER_SIZE = 16_384;

    @Test
    void testTransferTo() throws Exception {
        try (InputStream inputStream = Compress687Test.class.getClassLoader().getResourceAsStream(FIXTURE);
                Pack200CompressorInputStream compressInputStream = new Pack200CompressorInputStream(inputStream)) {
            transferTo(compressInputStream, NullOutputStream.INSTANCE);
        }
        try (InputStream inputStream = Compress687Test.class.getClassLoader().getResourceAsStream(FIXTURE);
                Pack200CompressorInputStream compressInputStream = new Pack200CompressorInputStream(inputStream)) {
            IOUtils.copy(compressInputStream, NullOutputStream.INSTANCE);
        }
        if (Boolean.getBoolean("Compress687Test.sysout")) {
            System.out.println("Done.");
        }
    }

    private long transferTo(final InputStream in, final OutputStream out) throws IOException {
        long transferred = 0;
        final byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
            if (transferred < Long.MAX_VALUE) {
                try {
                    transferred = Math.addExact(transferred, read);
                } catch (final ArithmeticException ignore) {
                    transferred = Long.MAX_VALUE;
                }
            }
        }
        return transferred;
    }
}

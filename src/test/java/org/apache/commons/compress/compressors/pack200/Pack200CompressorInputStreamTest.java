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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/**
 * Test {@link Pack200CompressorInputStream}.
 */
public class Pack200CompressorInputStreamTest {

    /*
     * Bad input detected in org.apache.commons.compress.harmony.unpack200.CpBands.parseCpUtf8(InputStream).
     *
     * An {@link IOException} wraps an {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    public void testBandSet_decodeBandInt() throws IOException {
        final byte[] input = Base64.getDecoder().decode("yv7QDQeWZxAEDXNJEBAuEBAQAQAAAABDAIAQEC8Q7RAQEPYAAAD/////ARAQCgoKCgo0CgoKCgoKCgoKCgoKJwAAAAoKLf4"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQEAcQQQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAPD+/v7+/v7+/v7+"
                + "/v7+/v7+/////wEAAAAAAAAAAAAAAAAQ//7+JZAoDQc=");
        assertThrows(IOException.class, () -> new Pack200CompressorInputStream(new ByteArrayInputStream(input)));
    }

    /*
     * Bad input detected in org.apache.commons.compress.harmony.unpack200.CpBands.parseCpUtf8(InputStream).
     *
     * An {@link IOException} wraps an {@link StringIndexOutOfBoundsException}.
     */
    @Test
    public void testCpBands_parseCpUtf8() throws IOException {
        final byte[] input = Base64.getDecoder().decode("yv7QDQeWEAMDAwMDAxAAAAAQKhAQEBAQKAYGBgYGBgYAECoQEBAQECj//5j/");
        assertThrows(IOException.class, () -> new Pack200CompressorInputStream(new ByteArrayInputStream(input)));
    }
}

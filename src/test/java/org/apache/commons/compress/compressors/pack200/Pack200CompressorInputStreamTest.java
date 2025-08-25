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
 * Tests {@link Pack200CompressorInputStream}.
 */
public class Pack200CompressorInputStreamTest {

    private void assertThrowsIOException(final String inputBase64) {
        assertThrows(IOException.class, () -> new Pack200CompressorInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(inputBase64))));
    }

    /**
     * Tests bad input detected in org.apache.commons.compress.harmony.unpack200.CpBands.parseCpUtf8(InputStream).
     *
     * An {@link IOException} wraps an {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    public void testBandSet_decodeBandInt() {
        assertThrowsIOException("yv7QDQeWZxAEDXNJEBAuEBAQAQAAAABDAIAQEC8Q7RAQEPYAAAD/////ARAQCgoKCgo0CgoKCgoKCgoKCgoKJwAAAAoKLf4"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAQEAcQQQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAPD+/v7+/v7+/v7+"
                + "/v7+/v7+/////wEAAAAAAAAAAAAAAAAQ//7+JZAoDQc=");
    }

    /**
     * Tests bad input detected in {@code org.apache.commons.compress.harmony.unpack200.BandSet.getReferences(int[], String[])}.
     *
     * An {@link IOException} wraps an {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    public void testBandSet_getReferences() {
        assertThrowsIOException("yv7QDQeWAgICAgMCAgICAv////8CAgAAAAINAAAAAAAAAAAAAAAAAAAAAAAAAAAAgBAC");
    }

    /**
     * Tests bad input detected in {@code org.apache.commons.compress.harmony.unpack200.CpBands.cpUTF8Value(int)}.
     *
     * An {@link IOException} wraps an {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    public void testBandSet_parseCPUTF8References() {
        assertThrowsIOException("yv7QDQeWEBAQEBAQEBAQEBAAAQAABhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAJSxkAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACUsZAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAEAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABgAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD///8WAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAASwAAAIAA"
                + "AAAAAAAQAQAAAAAAAAAAAAAAAAAAAAAAAAAABQAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAO3t7e3t7e3t7e3tAAAAAAAQ5xAQBhAQEBAQEBAQECgNBxcXFxc=");
    }

    /**
     * Tests bad input detected in {@code org.apache.commons.compress.harmony.unpack200.ClassBands.getCallCount(int[][], long[][], int)}.
     *
     * An {@link IOException} wraps an {@link NullPointerException}.
     */
    @Test
    public void testClassBands_getCallCount() {
        assertThrowsIOException("yv7QDQeWEBAQEBAQEBAQEBAAAAAAEAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA////////////////////////////////////////EBAQEBAQKA0H");
    }

    /**
     * Tests bad input detected in {@code org.apache.commons.compress.harmony.unpack200.CpBands.cpUTF8Value(int)}.
     *
     * An {@link IOException} wraps an {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    public void testCpBands_cpUTF8Value() {
        assertThrowsIOException(
                "yv7QDQeWEBgQEBAQEBAQEBAQEBAQEAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABwAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMsAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAQAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAMcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAlLGQAAAAAAAAAAAAAAADj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+Pj4+PjAAAAAACRAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACUsAA"
                        + "AAAAAAAAAAAAAAAGVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWU0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0NDQ0RzQ0NDQ0NDQ0"
                        + "NDQ0NAAAAAAAAAAAAAAAAAAAoKCgoKCgoKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAEAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAZS5jb21tb25zLmNvbXByZXNzLmhhcm1vbnk+dW5wYWNrMzcwLlBhY2syMDBVbnBhY2tlckFkYQAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAKCgoKCgoKCgAAAAAAAlLGQAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAvAAAAEOcQEBAQEBAQEBAQECgNBw==");
    }
    /**
     * Tests bad input detected in {@code org.apache.commons.compress.harmony.unpack200.CpBands.parseCpUtf8(InputStream)}.
     *
     * An {@link IOException} wraps an {@link StringIndexOutOfBoundsException}.
     */
    @Test
    public void testCpBands_parseCpUtf8() {
        assertThrowsIOException("yv7QDQeWEAMDAwMDAxAAAAAQKhAQEBAQKAYGBgYGBgYAECoQEBAQECj//5j/");
    }

    /**
     * Tests bad input detected in {@code org.apache.commons.compress.harmony.unpack200.MetadataBandGroup.getNextValue(int)}.
     *
     * An {@link IOException} wraps an {@link NegativeArraySizeException}.
     */
    @Test
    public void testMetadataBandGroup_getNextValue() {
        assertThrowsIOException("yv7QDQeWEBAQEBAQEBAQEBAQEBAQEAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAEAAAYACQAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAxwAAAAAAAAAAAAAAAAAAAAAAAAAA4+Pj4+Pj4+Pj4+NbW1tbW1tbW1tbW1tbW1tbW1tbW1tbqltbW1tb"
                + "W1tbW1tbW1tbW1tbW1tbW1tbBQUFBQUKBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQ"
                + "UFBQUFBQUFBQUFBQUFBVtbW1tbWwBbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1tbW1vj4+Pj4wAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAIAAAAAAAAAAAAAAAD5////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABJUVlZWVldZQAAAAAAAAAAAAA=");
    }

    /**
     * Tests bad input detected in {@code org.apache.commons.compress.harmony.pack200.PopulationCodec.decodeInts(int, InputStream)}.
     *
     * An {@link IOException} wraps an {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    public void testPopulationCodec_decodeInts() {
        assertThrowsIOException("yv7QDQeWgYGBgYGBgYGBgYGBhYUwhYWFhYV6enp6enp6hYWFhYWFhYWFhYWFhYWFhY2FhYWFhYWFh4WFhYWFhYVAhYWFhX2F"
                + "hYWFhYWFjoWFhY2FhYWFhYWFhYWFhYqFhYUHlmcQEBArEBAQLhAQEBAQEBCTATeTk5OTk5OTk5N1fJMrEBAQEBD2AAAAzQAAAAEAAgAA+wAHlmcQEBAAACsAJQAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAP7///+AAAAAAAAAAAAAAAAAAAAAAAAAAAAA/v///xAQLhAQEBAQEAAAsw==");
    }

    /**
     * Tests bad input detected in {@code org.apache.commons.compress.harmony.pack200.PopulationCodec.decodeInts(int, InputStream)}.
     *
     * An {@link IOException} wraps an {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    public void testRunCodec_decodeInts() {
        assertThrowsIOException("yv7QDQeWgYGBgYGBgYGBgYGBhYUwhYWFhYV6enp6enp6hYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFh4WFhYWFhYWFhYWFhYWF"
                + "hYWFhYWFjoWFjYWFhYWFhYWFhYWFhYWFhYUHlmcQEBArEBAQLhAQEBAQEBCTATeTk5OTk5OTk5N1fJMrEBAQEBD2AAAA/////wEAAgAA+wBaB9ANB5ZnEBAQKxAQEC4QEBAQEB"
                + "AQkwE3k5OTk5OTk5OTdXyTk3qKbP0AAAAAALM=");
    }
}

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.pack200.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.stream.Stream;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PopulationCodec;
import org.apache.commons.compress.harmony.pack200.RunCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for RunCodec
 */
public class RunCodecTest {

    static Stream<Arguments> runCodec() {
        return Stream.of(
                Arguments.of(0, Codec.SIGNED5, Codec.UDELTA5, "Should not allow a k value of 0"),
                Arguments.of(10, null, Codec.UDELTA5, "Should not allow a null codec"),
                Arguments.of(10, Codec.UDELTA5, null, "Should not allow a null codec"),
                Arguments.of(10, null, null, "Should not allow a null codec")
        );
    }

    @Test
    public void testDecode() throws Exception {
        RunCodec runCodec = new RunCodec(1, Codec.UNSIGNED5, Codec.BYTE1);
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] {
                (byte) 192, 0, (byte) 192, 0 });
        assertEquals(192, runCodec.decode(bais));
        assertEquals(192, runCodec.decode(bais));
        assertEquals(0, runCodec.decode(bais));
        assertEquals(0, bais.available());
        runCodec = new RunCodec(1, Codec.BYTE1, Codec.UNSIGNED5);
        bais = new ByteArrayInputStream(new byte[] { (byte) 192, 0, (byte) 192,
                0 });
        assertEquals(192, runCodec.decode(bais));
        assertEquals(0, runCodec.decode(bais));
        assertEquals(192, runCodec.decode(bais));
        assertEquals(0, bais.available());
    }

    @Test
    public void testDecodeInts() throws Exception {
        final int[] band = { 1, -2, -3, 1000, 55, 5, 10, 20 };
        // first 5 of band to be encoded with DELTA5
        final byte[] bytes1 = Codec.DELTA5.encode(new int[] { 1, -2, -3, 1000, 55 });
        // rest of band to be encoded with UNSIGNED5
        final byte[] bytes2 = Codec.UNSIGNED5.encode(new int[] { 5, 10, 20 });
        final byte[] bandEncoded = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, bandEncoded, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bandEncoded, bytes1.length, bytes2.length);
        final RunCodec runCodec = new RunCodec(5, Codec.DELTA5, Codec.UNSIGNED5);
        final int[] bandDecoded = runCodec.decodeInts(8, new ByteArrayInputStream(
                bandEncoded));
        assertEquals(band.length, bandDecoded.length);
        for (int i = 0; i < band.length; i++) {
            assertEquals(band[i], bandDecoded[i]);
        }
    }

    @Test
    public void testEncodeSingleValue() {
        assertThrows(Pack200Exception.class, () -> new RunCodec(10, Codec.SIGNED5, Codec.UDELTA5).encode(5),
                "Should not allow a single value to be encoded as we don't know which codec to use");
        assertThrows(Pack200Exception.class, () -> new RunCodec(10, Codec.SIGNED5, Codec.UDELTA5).encode(5, 8),
                "Should not allow a single value to be encoded as we don't know which codec to use");
    }

    @Test
    public void testNestedPopulationCodec() throws Exception {
        final int[] band = { 11, 12, 33, 4000, -555, 5, 10, 20, 10, 3, 20,
                20, 20, 10, 10, 999, 20, 789, 10, 10, 355, 12345 };
        // first 5 of band to be encoded with DELTA5
        final byte[] bytes1 = Codec.DELTA5
                .encode(new int[] { 11, 12, 33, 4000, -555 });
        // rest of band to be encoded with a PopulationCodec
        final PopulationCodec popCodec = new PopulationCodec(Codec.UNSIGNED5,
                Codec.BYTE1, Codec.UNSIGNED5);
        final byte[] bytes2 = popCodec.encode(new int[] { 10, 20 }, new int[] { 0, 1,
                2, 1, 0, 2, 2, 2, 1, 1, 0, 2, 0, 1, 1, 0, 0 }, new int[] { 5,
                3, 999, 789, 355, 12345 });
        final byte[] bandEncoded = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, bandEncoded, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bandEncoded, bytes1.length, bytes2.length);
        final RunCodec runCodec = new RunCodec(5, Codec.DELTA5, new PopulationCodec(
                Codec.UNSIGNED5, Codec.BYTE1, Codec.UNSIGNED5));
        final int[] bandDecoded = runCodec.decodeInts(band.length,
                new ByteArrayInputStream(bandEncoded));
        assertEquals(band.length, bandDecoded.length);
        for (int i = 0; i < band.length; i++) {
            assertEquals(band[i], bandDecoded[i]);
        }
    }

    @Test
    public void testNestedRunCodec() throws Exception {
        final int[] band = { 1, 2, 3, 10, 20, 30, 100, 200, 300 };
        // first 3 of band to be encoded with UDELTA5
        final byte[] bytes1 = Codec.UDELTA5.encode(new int[] { 1, 2, 3 });
        // rest of band to be encoded with a RunCodec
        final byte[] bytes2 = Codec.BYTE1.encode(new int[] { 10, 20, 30 });
        final byte[] bytes3 = Codec.UNSIGNED5.encode(new int[] { 100, 200, 300 });
        final byte[] bandEncoded = new byte[bytes1.length + bytes2.length
                + bytes3.length];
        System.arraycopy(bytes1, 0, bandEncoded, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bandEncoded, bytes1.length, bytes2.length);
        System.arraycopy(bytes3, 0, bandEncoded, bytes1.length + bytes2.length,
                bytes3.length);
        final RunCodec runCodec = new RunCodec(3, Codec.UDELTA5, new RunCodec(3,
                Codec.BYTE1, Codec.UNSIGNED5));
        final int[] bandDecoded = runCodec.decodeInts(9, new ByteArrayInputStream(
                bandEncoded));
        assertEquals(band.length, bandDecoded.length);
        for (int i = 0; i < band.length; i++) {
            assertEquals(band[i], bandDecoded[i]);
        }
    }

    @ParameterizedTest
    @MethodSource("runCodec")
    public void testRunCodec(final int k, final Codec aCodec, final Codec bCodec, final String failureMessage) {
        assertThrows(Pack200Exception.class, () -> new RunCodec(k, aCodec, bCodec), failureMessage);
    }

    @Test
    public void testToString() throws Pack200Exception {
        final RunCodec runCodec = new RunCodec(3, Codec.UNSIGNED5, Codec.BYTE1);
        assertEquals(
                "RunCodec[k=" + 3 + ";aCodec=" + Codec.UNSIGNED5 + "bCodec=" + Codec.BYTE1 + "]",
                runCodec.toString());
    }
}

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.CanonicalCodecFamilies;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 */
public class CodecTest {

    static Stream<Arguments> bCodings(){
        return IntStream.rangeClosed(1, 5).mapToObj(Arguments::of);
    }

    static Stream<Arguments> codecFamily() {
        return Stream.of(
                Arguments.of((Object) CanonicalCodecFamilies.nonDeltaUnsignedCodecs1),
                Arguments.of((Object) CanonicalCodecFamilies.nonDeltaUnsignedCodecs2),
                Arguments.of((Object) CanonicalCodecFamilies.nonDeltaUnsignedCodecs3),
                Arguments.of((Object) CanonicalCodecFamilies.nonDeltaUnsignedCodecs4),
                Arguments.of((Object) CanonicalCodecFamilies.nonDeltaUnsignedCodecs5),
                Arguments.of((Object) CanonicalCodecFamilies.deltaUnsignedCodecs1),
                Arguments.of((Object) CanonicalCodecFamilies.deltaUnsignedCodecs2),
                Arguments.of((Object) CanonicalCodecFamilies.deltaUnsignedCodecs3),
                Arguments.of((Object) CanonicalCodecFamilies.deltaUnsignedCodecs4),
                Arguments.of((Object) CanonicalCodecFamilies.deltaUnsignedCodecs5),
                Arguments.of((Object) CanonicalCodecFamilies.nonDeltaSignedCodecs1),
                Arguments.of((Object) CanonicalCodecFamilies.nonDeltaSignedCodecs2),
                Arguments.of((Object) CanonicalCodecFamilies.nonDeltaDoubleSignedCodecs1),
                Arguments.of((Object) CanonicalCodecFamilies.deltaSignedCodecs1),
                Arguments.of((Object) CanonicalCodecFamilies.deltaSignedCodecs2),
                Arguments.of((Object) CanonicalCodecFamilies.deltaSignedCodecs3),
                Arguments.of((Object) CanonicalCodecFamilies.deltaSignedCodecs4),
                Arguments.of((Object) CanonicalCodecFamilies.deltaSignedCodecs5),
                Arguments.of((Object) CanonicalCodecFamilies.deltaDoubleSignedCodecs1)
        );
    }

    static Stream<Arguments> hCodings() {
        return IntStream.range(0, 256).mapToObj(Arguments::of);
    }

    private long decode(final Codec codec, final byte[] data, final long value,
            final long last) throws IOException, Pack200Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream(data);
        assertEquals(value, codec.decode(in, last));
        assertEquals(-1, in.read());
        return (value);
    }

    private void decodeFail(final Codec codec, final byte[] data)
            throws IOException, Pack200Exception {
        assertThrowsExactly(EOFException.class, () -> decode(codec, data, 0, 0), "Should have detected an EOFException");
    }

    @ParameterizedTest
    @MethodSource("bCodings")
    public void testBCodings(final int i) {
        if (i == 5) {
            assertThrows(IllegalArgumentException.class, () -> new BHSDCodec(i, 256));
        } else {
            assertDoesNotThrow(() -> new BHSDCodec(i, 256));
        }
    }

    @Test
    public void testByte1() throws Exception {
        for (int i = 0; i < 255; i++) {
            decode(Codec.BYTE1, new byte[] { (byte) i }, i, 0);
        }
    }

    @Test
    public void testByte1Delta() throws Exception {
        final Codec BYTE1D = new BHSDCodec(1, 256, 0, 1);
        long last = 0;
        for (int i = 1; i < 255; i++) {
            last = decode(BYTE1D, new byte[] { (byte) 1 }, i, last);
        }
    }

    @Test
    public void testByte1DeltaException() throws Exception {
        final Codec BYTE1D = new BHSDCodec(1, 256, 0, 1);
        assertThrows(Pack200Exception.class, () -> BYTE1D.decode(new ByteArrayInputStream(new byte[]{(byte) 1})),
                "Decoding with a delta stream and not passing a last value should throw an exception");
    }

    @Test
    public void testByte1Signed() throws Exception {
        final Codec BYTE1S2 = new BHSDCodec(1, 256, 2);
        decode(BYTE1S2, new byte[] { 0 }, 0, 0);
        decode(BYTE1S2, new byte[] { 1 }, 1, 0);
        decode(BYTE1S2, new byte[] { 2 }, 2, 0);
        decode(BYTE1S2, new byte[] { 3 }, -1, 0);
        decode(BYTE1S2, new byte[] { 4 }, 3, 0);
        decode(BYTE1S2, new byte[] { 5 }, 4, 0);
        decode(BYTE1S2, new byte[] { 6 }, 5, 0);
        decode(BYTE1S2, new byte[] { 7 }, -2, 0);
        decode(BYTE1S2, new byte[] { 8 }, 6, 0);
        decode(BYTE1S2, new byte[] { 9 }, 7, 0);
        decode(BYTE1S2, new byte[] { 10 }, 8, 0);
        decode(BYTE1S2, new byte[] { 11 }, -3, 0);
    }

    @Test
    public void testCardinality() {
        final BHSDCodec byte1 = Codec.BYTE1;
        assertEquals(256, byte1.cardinality());
        assertEquals(0, byte1.smallest());
        assertEquals(255, byte1.largest());
        assertFalse(byte1.encodes(-257));
        assertFalse(byte1.encodes(-256));
        assertFalse(byte1.encodes(-255));
        assertFalse(byte1.encodes(-129));
        assertFalse(byte1.encodes(-128));
        assertFalse(byte1.encodes(-127));
        assertFalse(byte1.encodes(-1));
        assertTrue(byte1.encodes(0));
        assertTrue(byte1.encodes(1));
        assertTrue(byte1.encodes(255));
        assertFalse(byte1.encodes(256));
        final BHSDCodec byte1s = new BHSDCodec(1, 256, 1);
        assertEquals(256, byte1s.cardinality());
        assertEquals(-128, byte1s.smallest());
        assertEquals(127, byte1s.largest());
        assertFalse(byte1s.encodes(-257));
        assertFalse(byte1s.encodes(-256));
        assertFalse(byte1s.encodes(-255));
        assertFalse(byte1s.encodes(-129));
        assertTrue(byte1s.encodes(-128));
        assertTrue(byte1s.encodes(-127));
        assertTrue(byte1s.encodes(-1));
        assertTrue(byte1s.encodes(0));
        assertTrue(byte1s.encodes(1));
        assertTrue(byte1s.encodes(127));
        assertFalse(byte1s.encodes(128));
        assertFalse(byte1s.encodes(129));
        assertFalse(byte1s.encodes(255));
        assertFalse(byte1s.encodes(256));
        final BHSDCodec byte2s = new BHSDCodec(1, 256, 2);
        assertEquals(256, byte2s.cardinality());
        assertEquals(-64, byte2s.smallest());
        assertEquals(191, byte2s.largest());
        assertFalse(byte2s.encodes(-257));
        assertFalse(byte2s.encodes(-256));
        assertFalse(byte2s.encodes(-255));
        assertFalse(byte2s.encodes(-129));
        assertFalse(byte2s.encodes(-128));
        assertFalse(byte2s.encodes(-127));
        assertFalse(byte2s.encodes(-65));
        assertTrue(byte2s.encodes(-64));
        assertTrue(byte2s.encodes(-64));
        assertTrue(byte2s.encodes(-1));
        assertTrue(byte2s.encodes(0));
        assertTrue(byte2s.encodes(1));
        assertTrue(byte2s.encodes(127));
        assertTrue(byte2s.encodes(128));
        assertTrue(byte2s.encodes(191));
        assertFalse(byte2s.encodes(192));
        assertFalse(byte2s.encodes(256));
    }

    @ParameterizedTest
    @MethodSource("codecFamily")
    public void testCodecFamilies(final BHSDCodec[] family) {
        for (int i = 1; i < family.length; i++) {
            final BHSDCodec previous = family[i-1];
            final BHSDCodec codec = family[i];
            assertTrue(codec.largest() >= previous.largest());
            assertTrue(codec.smallest() <= previous.smallest());
        }
    }

    @Test
    public void testCodecToString() {
        assertEquals("(1,256)", Codec.BYTE1.toString());
        assertEquals("(3,128)", Codec.CHAR3.toString());
        assertEquals("(5,4)", Codec.BCI5.toString());
        assertEquals("(5,4,2)", Codec.BRANCH5.toString());
        assertEquals("(5,64)", Codec.UNSIGNED5.toString());
        assertEquals("(5,64,1)", Codec.SIGNED5.toString());
        assertEquals("(5,64,0,1)", Codec.UDELTA5.toString());
        assertEquals("(5,64,1,1)", Codec.DELTA5.toString());
        assertEquals("(5,64,2,1)", Codec.MDELTA5.toString());
        assertEquals("(5,64)", Codec.UNSIGNED5.toString());
        assertEquals("(5,64,1)", Codec.SIGNED5.toString());
        assertEquals("(5,64,1,1)", Codec.DELTA5.toString());
        assertEquals("(5,64,2,1)", Codec.MDELTA5.toString());
    }

    @ParameterizedTest
    @MethodSource("hCodings")
    public void testInvalidHCodings(final int i) {
        assertThrows(IllegalArgumentException.class, () -> new BHSDCodec(1, i), "b=1 -> h=256");
    }

    @Test
    public void testUnsigned5() throws Exception {
        decode(Codec.UNSIGNED5, new byte[] { 1 }, 1, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 191 }, 191, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 192, 0 }, 192, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 193, 0 }, 193, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 255, 0 }, 255, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 192, 1 }, 256, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 192, 5 }, 512, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 192, 13 }, 1024, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 192, 29 }, 2048, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 255, (byte) 191 }, 12479, 0);

        decode(Codec.UNSIGNED5, new byte[] { (byte) 192, (byte) 192, 0 },
                12480, 0);
        decode(Codec.UNSIGNED5,
                new byte[] { (byte) 255, (byte) 255, (byte) 191 }, 798911, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 192, (byte) 192,
                (byte) 192, 0 }, 798912, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 255, (byte) 255,
                (byte) 255, (byte) 191 }, 51130559, 0);
        decode(Codec.UNSIGNED5, new byte[] { (byte) 192, (byte) 192,
                (byte) 192, (byte) 192, 0 }, 51130560, 0);
        decodeFail(Codec.UNSIGNED5, new byte[] { (byte) 192 });
        decodeFail(Codec.UNSIGNED5, new byte[] { (byte) 192, (byte) 192 });
        decodeFail(Codec.UNSIGNED5, new byte[] { (byte) 192, (byte) 192,
                (byte) 192 });
        decodeFail(Codec.UNSIGNED5, new byte[] { (byte) 192, (byte) 192,
                (byte) 192, (byte) 192 });
    }
}

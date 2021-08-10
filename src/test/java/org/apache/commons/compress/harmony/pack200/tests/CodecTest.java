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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.CanonicalCodecFamilies;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 *
 */
public class CodecTest extends TestCase {

    public void testInvalidCodings() {
        for (int i = 0; i < 256; i++) {
            try {
                new BHSDCodec(1, i);
                fail("b=1 -> h=256");
            } catch (IllegalArgumentException e) {
                assertTrue(true);
            }
        }
        for (int i = 1; i <= 5; i++) {
            try {
                new BHSDCodec(i, 256);
                if (i == 5) {
                    fail("h=256 -> b!=5");
                }
            } catch (IllegalArgumentException e) {
                assertTrue(true);
            }
        }

    }

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

    public void testByte1() throws Exception {
        for (int i = 0; i < 255; i++) {
            decode(Codec.BYTE1, new byte[] { (byte) i }, i, 0);
        }
    }

    public void testByte1Delta() throws Exception {
        Codec BYTE1D = new BHSDCodec(1, 256, 0, 1);
        long last = 0;
        for (int i = 1; i < 255; i++) {
            last = decode(BYTE1D, new byte[] { (byte) 1 }, i, last);
        }
    }

    public void testByte1DeltaException() throws Exception {
        Codec BYTE1D = new BHSDCodec(1, 256, 0, 1);
        try {
            BYTE1D.decode(new ByteArrayInputStream(new byte[] { (byte) 1 }));
            fail("Decoding with a delta stream and not passing a last value should throw exception");
        } catch (Pack200Exception e) {
            assertTrue(true);
        }
    }

    public void testByte1Signed() throws Exception {
        Codec BYTE1S2 = new BHSDCodec(1, 256, 2);
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

    public void testCardinality() {
        BHSDCodec byte1 = Codec.BYTE1;
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
        BHSDCodec byte1s = new BHSDCodec(1, 256, 1);
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
        BHSDCodec byte2s = new BHSDCodec(1, 256, 2);
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
    public void testCodecFamilies() {
        checkAscendingCardinalities(CanonicalCodecFamilies.nonDeltaUnsignedCodecs1);
        checkAscendingCardinalities(CanonicalCodecFamilies.nonDeltaUnsignedCodecs2);
        checkAscendingCardinalities(CanonicalCodecFamilies.nonDeltaUnsignedCodecs3);
        checkAscendingCardinalities(CanonicalCodecFamilies.nonDeltaUnsignedCodecs4);
        checkAscendingCardinalities(CanonicalCodecFamilies.nonDeltaUnsignedCodecs5);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaUnsignedCodecs1);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaUnsignedCodecs2);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaUnsignedCodecs3);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaUnsignedCodecs4);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaUnsignedCodecs5);
        checkAscendingCardinalities(CanonicalCodecFamilies.nonDeltaSignedCodecs1);
        checkAscendingCardinalities(CanonicalCodecFamilies.nonDeltaSignedCodecs2);
        checkAscendingCardinalities(CanonicalCodecFamilies.nonDeltaDoubleSignedCodecs1);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaSignedCodecs1);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaSignedCodecs2);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaSignedCodecs3);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaSignedCodecs4);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaSignedCodecs5);
        checkAscendingCardinalities(CanonicalCodecFamilies.deltaDoubleSignedCodecs1);
    }

    private void checkAscendingCardinalities(BHSDCodec[] family) {
        for (int i = 1; i < family.length; i++) {
            BHSDCodec previous = family[i-1];
            BHSDCodec codec = family[i];
            assertTrue(codec.largest() >= previous.largest());
            assertTrue(codec.smallest() <= previous.smallest());
        }
    }

    private void decodeFail(final Codec codec, final byte[] data)
            throws IOException, Pack200Exception {
        try {
            decode(codec, data, 0, 0);
            fail("Should have detected an EOFException");
        } catch (EOFException e) {
            assertTrue(true);
        }
    }

    private long decode(final Codec codec, final byte[] data, final long value,
            final long last) throws IOException, Pack200Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream(data);
        assertEquals(value, codec.decode(in, last));
        assertEquals(-1, in.read());
        return (value);
    }
}

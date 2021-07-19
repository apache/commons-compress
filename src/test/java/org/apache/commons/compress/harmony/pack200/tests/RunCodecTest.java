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

import junit.framework.TestCase;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PopulationCodec;
import org.apache.commons.compress.harmony.pack200.RunCodec;

/**
 * Test for RunCodec
 */
public class RunCodecTest extends TestCase {

    public void testRunCodec() {
        try {
            new RunCodec(0, BHSDCodec.SIGNED5, BHSDCodec.UDELTA5);
            fail("Should not allow a k value of 0");
        } catch (Pack200Exception e) {
            // pass
        }
        try {
            new RunCodec(10, null, BHSDCodec.UDELTA5);
            fail("Should not allow a null codec");
        } catch (Pack200Exception e) {
            // pass
        }
        try {
            new RunCodec(10, BHSDCodec.UDELTA5, null);
            fail("Should not allow a null codec");
        } catch (Pack200Exception e) {
            // pass
        }
        try {
            new RunCodec(10, null, null);
            fail("Should not allow a null codec");
        } catch (Pack200Exception e) {
            // pass
        }
    }

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

    public void testDecodeInts() throws Exception {
        int[] band = new int[] { 1, -2, -3, 1000, 55, 5, 10, 20 };
        // first 5 of band to be encoded with DELTA5
        byte[] bytes1 = Codec.DELTA5.encode(new int[] { 1, -2, -3, 1000, 55 });
        // rest of band to be encoded with UNSIGNED5
        byte[] bytes2 = Codec.UNSIGNED5.encode(new int[] { 5, 10, 20 });
        byte[] bandEncoded = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, bandEncoded, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bandEncoded, bytes1.length, bytes2.length);
        RunCodec runCodec = new RunCodec(5, Codec.DELTA5, Codec.UNSIGNED5);
        int[] bandDecoded = runCodec.decodeInts(8, new ByteArrayInputStream(
                bandEncoded));
        assertEquals(band.length, bandDecoded.length);
        for (int i = 0; i < band.length; i++) {
            assertEquals(band[i], bandDecoded[i]);
        }
    }

    public void testNestedPopulationCodec() throws Exception {
        int[] band = new int[] { 11, 12, 33, 4000, -555, 5, 10, 20, 10, 3, 20,
                20, 20, 10, 10, 999, 20, 789, 10, 10, 355, 12345 };
        // first 5 of band to be encoded with DELTA5
        byte[] bytes1 = Codec.DELTA5
                .encode(new int[] { 11, 12, 33, 4000, -555 });
        // rest of band to be encoded with a PopulationCodec
        PopulationCodec popCodec = new PopulationCodec(Codec.UNSIGNED5,
                Codec.BYTE1, Codec.UNSIGNED5);
        byte[] bytes2 = popCodec.encode(new int[] { 10, 20 }, new int[] { 0, 1,
                2, 1, 0, 2, 2, 2, 1, 1, 0, 2, 0, 1, 1, 0, 0 }, new int[] { 5,
                3, 999, 789, 355, 12345 });
        byte[] bandEncoded = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, bandEncoded, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bandEncoded, bytes1.length, bytes2.length);
        RunCodec runCodec = new RunCodec(5, Codec.DELTA5, new PopulationCodec(
                Codec.UNSIGNED5, Codec.BYTE1, Codec.UNSIGNED5));
        int[] bandDecoded = runCodec.decodeInts(band.length,
                new ByteArrayInputStream(bandEncoded));
        assertEquals(band.length, bandDecoded.length);
        for (int i = 0; i < band.length; i++) {
            assertEquals(band[i], bandDecoded[i]);
        }
    }

    public void testNestedRunCodec() throws Exception {
        int[] band = new int[] { 1, 2, 3, 10, 20, 30, 100, 200, 300 };
        // first 3 of band to be encoded with UDELTA5
        byte[] bytes1 = Codec.UDELTA5.encode(new int[] { 1, 2, 3 });
        // rest of band to be encoded with a RunCodec
        byte[] bytes2 = Codec.BYTE1.encode(new int[] { 10, 20, 30 });
        byte[] bytes3 = Codec.UNSIGNED5.encode(new int[] { 100, 200, 300 });
        byte[] bandEncoded = new byte[bytes1.length + bytes2.length
                + bytes3.length];
        System.arraycopy(bytes1, 0, bandEncoded, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bandEncoded, bytes1.length, bytes2.length);
        System.arraycopy(bytes3, 0, bandEncoded, bytes1.length + bytes2.length,
                bytes3.length);
        RunCodec runCodec = new RunCodec(3, Codec.UDELTA5, new RunCodec(3,
                Codec.BYTE1, Codec.UNSIGNED5));
        int[] bandDecoded = runCodec.decodeInts(9, new ByteArrayInputStream(
                bandEncoded));
        assertEquals(band.length, bandDecoded.length);
        for (int i = 0; i < band.length; i++) {
            assertEquals(band[i], bandDecoded[i]);
        }
    }

    public void testToString() throws Pack200Exception {
        RunCodec runCodec = new RunCodec(3, Codec.UNSIGNED5, Codec.BYTE1);
        assertEquals(
                "RunCodec[k=" + 3 + ";aCodec=" + Codec.UNSIGNED5 + "bCodec=" + Codec.BYTE1 + "]",
                runCodec.toString());
    }

    public void testEncodeSingleValue() {
        try {
            new RunCodec(10, BHSDCodec.SIGNED5, BHSDCodec.UDELTA5).encode(5);
            fail("Should not allow a single value to be encoded as we don't know which codec to use");
        } catch (Pack200Exception e) {
            // pass
        }
        try {
            new RunCodec(10, BHSDCodec.SIGNED5, BHSDCodec.UDELTA5).encode(5, 8);
            fail("Should not allow a single value to be encoded as we don't know which codec to use");
        } catch (Pack200Exception e) {
            // pass
        }
    }
}

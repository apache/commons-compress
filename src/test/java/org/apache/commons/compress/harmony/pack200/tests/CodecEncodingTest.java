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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.CodecEncoding;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PopulationCodec;
import org.apache.commons.compress.harmony.pack200.RunCodec;

/**
 *
 */
public class CodecEncodingTest extends TestCase {

    public void testCanonicalEncodings() throws IOException, Pack200Exception {
        Codec defaultCodec = new BHSDCodec(2, 16, 0, 0);
        assertEquals(defaultCodec, CodecEncoding
                .getCodec(0, null, defaultCodec));
        Map map = new HashMap();
        // These are the canonical encodings specified by the Pack200 spec
        map.put(new Integer(1), "(1,256)");
        map.put(new Integer(2), "(1,256,1)");
        map.put(new Integer(3), "(1,256,0,1)");
        map.put(new Integer(4), "(1,256,1,1)");
        map.put(new Integer(5), "(2,256)");
        map.put(new Integer(6), "(2,256,1)");
        map.put(new Integer(7), "(2,256,0,1)");
        map.put(new Integer(8), "(2,256,1,1)");
        map.put(new Integer(9), "(3,256)");
        map.put(new Integer(10), "(3,256,1)");
        map.put(new Integer(11), "(3,256,0,1)");
        map.put(new Integer(12), "(3,256,1,1)");
        map.put(new Integer(13), "(4,256)");
        map.put(new Integer(14), "(4,256,1)");
        map.put(new Integer(15), "(4,256,0,1)");
        map.put(new Integer(16), "(4,256,1,1)");
        map.put(new Integer(17), "(5,4)");
        map.put(new Integer(18), "(5,4,1)");
        map.put(new Integer(19), "(5,4,2)");
        map.put(new Integer(20), "(5,16)");
        map.put(new Integer(21), "(5,16,1)");
        map.put(new Integer(22), "(5,16,2)");
        map.put(new Integer(23), "(5,32)");
        map.put(new Integer(24), "(5,32,1)");
        map.put(new Integer(25), "(5,32,2)");
        map.put(new Integer(26), "(5,64)");
        map.put(new Integer(27), "(5,64,1)");
        map.put(new Integer(28), "(5,64,2)");
        map.put(new Integer(29), "(5,128)");
        map.put(new Integer(30), "(5,128,1)");
        map.put(new Integer(31), "(5,128,2)");
        map.put(new Integer(32), "(5,4,0,1)");
        map.put(new Integer(33), "(5,4,1,1)");
        map.put(new Integer(34), "(5,4,2,1)");
        map.put(new Integer(35), "(5,16,0,1)");
        map.put(new Integer(36), "(5,16,1,1)");
        map.put(new Integer(37), "(5,16,2,1)");
        map.put(new Integer(38), "(5,32,0,1)");
        map.put(new Integer(39), "(5,32,1,1)");
        map.put(new Integer(40), "(5,32,2,1)");
        map.put(new Integer(41), "(5,64,0,1)");
        map.put(new Integer(42), "(5,64,1,1)");
        map.put(new Integer(43), "(5,64,2,1)");
        map.put(new Integer(44), "(5,128,0,1)");
        map.put(new Integer(45), "(5,128,1,1)");
        map.put(new Integer(46), "(5,128,2,1)");
        map.put(new Integer(47), "(2,192)");
        map.put(new Integer(48), "(2,224)");
        map.put(new Integer(49), "(2,240)");
        map.put(new Integer(50), "(2,248)");
        map.put(new Integer(51), "(2,252)");
        map.put(new Integer(52), "(2,8,0,1)");
        map.put(new Integer(53), "(2,8,1,1)");
        map.put(new Integer(54), "(2,16,0,1)");
        map.put(new Integer(55), "(2,16,1,1)");
        map.put(new Integer(56), "(2,32,0,1)");
        map.put(new Integer(57), "(2,32,1,1)");
        map.put(new Integer(58), "(2,64,0,1)");
        map.put(new Integer(59), "(2,64,1,1)");
        map.put(new Integer(60), "(2,128,0,1)");
        map.put(new Integer(61), "(2,128,1,1)");
        map.put(new Integer(62), "(2,192,0,1)");
        map.put(new Integer(63), "(2,192,1,1)");
        map.put(new Integer(64), "(2,224,0,1)");
        map.put(new Integer(65), "(2,224,1,1)");
        map.put(new Integer(66), "(2,240,0,1)");
        map.put(new Integer(67), "(2,240,1,1)");
        map.put(new Integer(68), "(2,248,0,1)");
        map.put(new Integer(69), "(2,248,1,1)");
        map.put(new Integer(70), "(3,192)");
        map.put(new Integer(71), "(3,224)");
        map.put(new Integer(72), "(3,240)");
        map.put(new Integer(73), "(3,248)");
        map.put(new Integer(74), "(3,252)");
        map.put(new Integer(75), "(3,8,0,1)");
        map.put(new Integer(76), "(3,8,1,1)");
        map.put(new Integer(77), "(3,16,0,1)");
        map.put(new Integer(78), "(3,16,1,1)");
        map.put(new Integer(79), "(3,32,0,1)");
        map.put(new Integer(80), "(3,32,1,1)");
        map.put(new Integer(81), "(3,64,0,1)");
        map.put(new Integer(82), "(3,64,1,1)");
        map.put(new Integer(83), "(3,128,0,1)");
        map.put(new Integer(84), "(3,128,1,1)");
        map.put(new Integer(85), "(3,192,0,1)");
        map.put(new Integer(86), "(3,192,1,1)");
        map.put(new Integer(87), "(3,224,0,1)");
        map.put(new Integer(88), "(3,224,1,1)");
        map.put(new Integer(89), "(3,240,0,1)");
        map.put(new Integer(90), "(3,240,1,1)");
        map.put(new Integer(91), "(3,248,0,1)");
        map.put(new Integer(92), "(3,248,1,1)");
        map.put(new Integer(93), "(4,192)");
        map.put(new Integer(94), "(4,224)");
        map.put(new Integer(95), "(4,240)");
        map.put(new Integer(96), "(4,248)");
        map.put(new Integer(97), "(4,252)");
        map.put(new Integer(98), "(4,8,0,1)");
        map.put(new Integer(99), "(4,8,1,1)");
        map.put(new Integer(100), "(4,16,0,1)");
        map.put(new Integer(101), "(4,16,1,1)");
        map.put(new Integer(102), "(4,32,0,1)");
        map.put(new Integer(103), "(4,32,1,1)");
        map.put(new Integer(104), "(4,64,0,1)");
        map.put(new Integer(105), "(4,64,1,1)");
        map.put(new Integer(106), "(4,128,0,1)");
        map.put(new Integer(107), "(4,128,1,1)");
        map.put(new Integer(108), "(4,192,0,1)");
        map.put(new Integer(109), "(4,192,1,1)");
        map.put(new Integer(110), "(4,224,0,1)");
        map.put(new Integer(111), "(4,224,1,1)");
        map.put(new Integer(112), "(4,240,0,1)");
        map.put(new Integer(113), "(4,240,1,1)");
        map.put(new Integer(114), "(4,248,0,1)");
        map.put(new Integer(115), "(4,248,1,1)");
        for (int i = 1; i <= 115; i++) {
            assertEquals(map.get(new Integer(i)), CodecEncoding.getCodec(i,
                    null, null).toString());
        }
    }

    public void testArbitraryCodec() throws IOException, Pack200Exception {
        assertEquals("(1,256)", CodecEncoding.getCodec(116,
                new ByteArrayInputStream(new byte[] { 0x00, (byte) 0xFF }),
                null).toString());
        assertEquals("(5,128,2,1)", CodecEncoding.getCodec(116,
                new ByteArrayInputStream(new byte[] { 0x25, (byte) 0x7F }),
                null).toString());
        assertEquals("(2,128,1,1)", CodecEncoding.getCodec(116,
                new ByteArrayInputStream(new byte[] { 0x0B, (byte) 0x7F }),
                null).toString());
    }

    public void testGetSpecifier() throws IOException, Pack200Exception {
        // Test canonical codecs
        for (int i = 1; i <= 115; i++) {
            assertEquals(i, CodecEncoding.getSpecifier(CodecEncoding.getCodec(i, null, null), null)[0]);
        }

        // Test a range of non-canonical codecs
        Codec c1 = new BHSDCodec(2, 125, 0, 1);
        int[] specifiers = CodecEncoding.getSpecifier(c1, null);
        assertEquals(3, specifiers.length);
        assertEquals(116, specifiers[0]);
        byte[] bytes = new byte[] {(byte) specifiers[1], (byte) specifiers[2]};
        InputStream in = new ByteArrayInputStream(bytes);
        assertEquals(c1, CodecEncoding.getCodec(116, in, null));

        c1 = new BHSDCodec(3, 125, 2, 1);
        specifiers = CodecEncoding.getSpecifier(c1, null);
        assertEquals(3, specifiers.length);
        assertEquals(116, specifiers[0]);
        bytes = new byte[] {(byte) specifiers[1], (byte) specifiers[2]};
        in = new ByteArrayInputStream(bytes);
        assertEquals(c1, CodecEncoding.getCodec(116, in, null));

        c1 = new BHSDCodec(4, 125);
        specifiers = CodecEncoding.getSpecifier(c1, null);
        assertEquals(3, specifiers.length);
        assertEquals(116, specifiers[0]);
        bytes = new byte[] {(byte) specifiers[1], (byte) specifiers[2]};
        in = new ByteArrayInputStream(bytes);
        assertEquals(c1, CodecEncoding.getCodec(116, in, null));

        c1 = new BHSDCodec(5, 125, 2, 0);
        specifiers = CodecEncoding.getSpecifier(c1, null);
        assertEquals(3, specifiers.length);
        assertEquals(116, specifiers[0]);
        bytes = new byte[] {(byte) specifiers[1], (byte) specifiers[2]};
        in = new ByteArrayInputStream(bytes);
        assertEquals(c1, CodecEncoding.getCodec(116, in, null));

        c1 = new BHSDCodec(3, 5, 2, 1);
        specifiers = CodecEncoding.getSpecifier(c1, null);
        assertEquals(3, specifiers.length);
        assertEquals(116, specifiers[0]);
        bytes = new byte[] {(byte) specifiers[1], (byte) specifiers[2]};
        in = new ByteArrayInputStream(bytes);
        assertEquals(c1, CodecEncoding.getCodec(116, in, null));
    }

    public void testGetSpeciferForRunCodec() throws Pack200Exception, IOException {
        RunCodec runCodec = new RunCodec(25, Codec.DELTA5, Codec.BYTE1);
        int[] specifiers = CodecEncoding.getSpecifier(runCodec, null);
        assertTrue(specifiers[0] > 116);
        assertTrue(specifiers[0] < 141);
        byte[] bytes = new byte[specifiers.length - 1];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) specifiers[i+1];
        }
        InputStream in = new ByteArrayInputStream(bytes);
        RunCodec runCodec2 = (RunCodec) CodecEncoding.getCodec(specifiers[0], in, null);
        assertEquals(runCodec.getK(), runCodec2.getK());
        assertEquals(runCodec.getACodec(), runCodec2.getACodec());
        assertEquals(runCodec.getBCodec(), runCodec2.getBCodec());

        // One codec is the same as the default
        runCodec = new RunCodec(4096, Codec.DELTA5, Codec.BYTE1);
        specifiers = CodecEncoding.getSpecifier(runCodec, Codec.DELTA5);
        assertTrue(specifiers[0] > 116);
        assertTrue(specifiers[0] < 141);
        bytes = new byte[specifiers.length - 1];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) specifiers[i+1];
        }
        in = new ByteArrayInputStream(bytes);
        runCodec2 = (RunCodec) CodecEncoding.getCodec(specifiers[0], in, Codec.DELTA5);
        assertEquals(runCodec.getK(), runCodec2.getK());
        assertEquals(runCodec.getACodec(), runCodec2.getACodec());
        assertEquals(runCodec.getBCodec(), runCodec2.getBCodec());

        // Nested run codecs
        runCodec = new RunCodec(64, Codec.SIGNED5, new RunCodec(25, Codec.UDELTA5, Codec.DELTA5));
        specifiers = CodecEncoding.getSpecifier(runCodec, null);
        assertTrue(specifiers[0] > 116);
        assertTrue(specifiers[0] < 141);
        bytes = new byte[specifiers.length - 1];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) specifiers[i+1];
        }
        in = new ByteArrayInputStream(bytes);
        runCodec2 = (RunCodec) CodecEncoding.getCodec(specifiers[0], in, null);
        assertEquals(runCodec.getK(), runCodec2.getK());
        assertEquals(runCodec.getACodec(), runCodec2.getACodec());
        RunCodec bCodec = (RunCodec) runCodec.getBCodec();
        RunCodec bCodec2 = (RunCodec) runCodec2.getBCodec();
        assertEquals(bCodec.getK(), bCodec2.getK());
        assertEquals(bCodec.getACodec(), bCodec2.getACodec());
        assertEquals(bCodec.getBCodec(), bCodec2.getBCodec());

        // Nested with one the same as the default
        runCodec = new RunCodec(64, Codec.SIGNED5, new RunCodec(25, Codec.UDELTA5, Codec.DELTA5));
        specifiers = CodecEncoding.getSpecifier(runCodec, Codec.UDELTA5);
        assertTrue(specifiers[0] > 116);
        assertTrue(specifiers[0] < 141);
        bytes = new byte[specifiers.length - 1];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) specifiers[i+1];
        }
        in = new ByteArrayInputStream(bytes);
        runCodec2 = (RunCodec) CodecEncoding.getCodec(specifiers[0], in, Codec.UDELTA5);
        assertEquals(runCodec.getK(), runCodec2.getK());
        assertEquals(runCodec.getACodec(), runCodec2.getACodec());
        bCodec = (RunCodec) runCodec.getBCodec();
        bCodec2 = (RunCodec) runCodec2.getBCodec();
        assertEquals(bCodec.getK(), bCodec2.getK());
        assertEquals(bCodec.getACodec(), bCodec2.getACodec());
        assertEquals(bCodec.getBCodec(), bCodec2.getBCodec());
    }

    public void testGetSpeciferForPopulationCodec() throws IOException, Pack200Exception {
        PopulationCodec pCodec = new PopulationCodec(Codec.BYTE1, Codec.CHAR3, Codec.UNSIGNED5);
        int[] specifiers = CodecEncoding.getSpecifier(pCodec, null);
        assertTrue(specifiers[0] > 140);
        assertTrue(specifiers[0] < 189);
        byte[] bytes = new byte[specifiers.length - 1];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) specifiers[i+1];
        }
        InputStream in = new ByteArrayInputStream(bytes);
        PopulationCodec pCodec2 = (PopulationCodec) CodecEncoding.getCodec(specifiers[0], in, null);
        assertEquals(pCodec.getFavouredCodec(), pCodec2.getFavouredCodec());
        assertEquals(pCodec.getTokenCodec(), pCodec2.getTokenCodec());
        assertEquals(pCodec.getUnfavouredCodec(), pCodec2.getUnfavouredCodec());
    }

}

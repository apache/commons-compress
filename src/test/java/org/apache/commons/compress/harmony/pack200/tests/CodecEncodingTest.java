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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.CodecEncoding;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PopulationCodec;
import org.apache.commons.compress.harmony.pack200.RunCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 */
public class CodecEncodingTest {

    static Stream<Arguments> arbitraryCodec() {
        return Stream.of(
                Arguments.of("(1,256)", new byte[] { 0x00, (byte) 0xFF }),
                Arguments.of("(5,128,2,1)", new byte[] { 0x25, (byte) 0x7F }),
                Arguments.of("(2,128,1,1)", new byte[] { 0x0B, (byte) 0x7F })
        );
    }

    // These are the canonical encodings specified by the Pack200 spec
    static Stream<Arguments> canonicalEncodings() {
        return Stream.of(
                Arguments.of(1, "(1,256)"),
                Arguments.of(2, "(1,256,1)"),
                Arguments.of(3, "(1,256,0,1)"),
                Arguments.of(4, "(1,256,1,1)"),
                Arguments.of(5, "(2,256)"),
                Arguments.of(6, "(2,256,1)"),
                Arguments.of(7, "(2,256,0,1)"),
                Arguments.of(8, "(2,256,1,1)"),
                Arguments.of(9, "(3,256)"),
                Arguments.of(10, "(3,256,1)"),
                Arguments.of(11, "(3,256,0,1)"),
                Arguments.of(12, "(3,256,1,1)"),
                Arguments.of(13, "(4,256)"),
                Arguments.of(14, "(4,256,1)"),
                Arguments.of(15, "(4,256,0,1)"),
                Arguments.of(16, "(4,256,1,1)"),
                Arguments.of(17, "(5,4)"),
                Arguments.of(18, "(5,4,1)"),
                Arguments.of(19, "(5,4,2)"),
                Arguments.of(20, "(5,16)"),
                Arguments.of(21, "(5,16,1)"),
                Arguments.of(22, "(5,16,2)"),
                Arguments.of(23, "(5,32)"),
                Arguments.of(24, "(5,32,1)"),
                Arguments.of(25, "(5,32,2)"),
                Arguments.of(26, "(5,64)"),
                Arguments.of(27, "(5,64,1)"),
                Arguments.of(28, "(5,64,2)"),
                Arguments.of(29, "(5,128)"),
                Arguments.of(30, "(5,128,1)"),
                Arguments.of(31, "(5,128,2)"),
                Arguments.of(32, "(5,4,0,1)"),
                Arguments.of(33, "(5,4,1,1)"),
                Arguments.of(34, "(5,4,2,1)"),
                Arguments.of(35, "(5,16,0,1)"),
                Arguments.of(36, "(5,16,1,1)"),
                Arguments.of(37, "(5,16,2,1)"),
                Arguments.of(38, "(5,32,0,1)"),
                Arguments.of(39, "(5,32,1,1)"),
                Arguments.of(40, "(5,32,2,1)"),
                Arguments.of(41, "(5,64,0,1)"),
                Arguments.of(42, "(5,64,1,1)"),
                Arguments.of(43, "(5,64,2,1)"),
                Arguments.of(44, "(5,128,0,1)"),
                Arguments.of(45, "(5,128,1,1)"),
                Arguments.of(46, "(5,128,2,1)"),
                Arguments.of(47, "(2,192)"),
                Arguments.of(48, "(2,224)"),
                Arguments.of(49, "(2,240)"),
                Arguments.of(50, "(2,248)"),
                Arguments.of(51, "(2,252)"),
                Arguments.of(52, "(2,8,0,1)"),
                Arguments.of(53, "(2,8,1,1)"),
                Arguments.of(54, "(2,16,0,1)"),
                Arguments.of(55, "(2,16,1,1)"),
                Arguments.of(56, "(2,32,0,1)"),
                Arguments.of(57, "(2,32,1,1)"),
                Arguments.of(58, "(2,64,0,1)"),
                Arguments.of(59, "(2,64,1,1)"),
                Arguments.of(60, "(2,128,0,1)"),
                Arguments.of(61, "(2,128,1,1)"),
                Arguments.of(62, "(2,192,0,1)"),
                Arguments.of(63, "(2,192,1,1)"),
                Arguments.of(64, "(2,224,0,1)"),
                Arguments.of(65, "(2,224,1,1)"),
                Arguments.of(66, "(2,240,0,1)"),
                Arguments.of(67, "(2,240,1,1)"),
                Arguments.of(68, "(2,248,0,1)"),
                Arguments.of(69, "(2,248,1,1)"),
                Arguments.of(70, "(3,192)"),
                Arguments.of(71, "(3,224)"),
                Arguments.of(72, "(3,240)"),
                Arguments.of(73, "(3,248)"),
                Arguments.of(74, "(3,252)"),
                Arguments.of(75, "(3,8,0,1)"),
                Arguments.of(76, "(3,8,1,1)"),
                Arguments.of(77, "(3,16,0,1)"),
                Arguments.of(78, "(3,16,1,1)"),
                Arguments.of(79, "(3,32,0,1)"),
                Arguments.of(80, "(3,32,1,1)"),
                Arguments.of(81, "(3,64,0,1)"),
                Arguments.of(82, "(3,64,1,1)"),
                Arguments.of(83, "(3,128,0,1)"),
                Arguments.of(84, "(3,128,1,1)"),
                Arguments.of(85, "(3,192,0,1)"),
                Arguments.of(86, "(3,192,1,1)"),
                Arguments.of(87, "(3,224,0,1)"),
                Arguments.of(88, "(3,224,1,1)"),
                Arguments.of(89, "(3,240,0,1)"),
                Arguments.of(90, "(3,240,1,1)"),
                Arguments.of(91, "(3,248,0,1)"),
                Arguments.of(92, "(3,248,1,1)"),
                Arguments.of(93, "(4,192)"),
                Arguments.of(94, "(4,224)"),
                Arguments.of(95, "(4,240)"),
                Arguments.of(96, "(4,248)"),
                Arguments.of(97, "(4,252)"),
                Arguments.of(98, "(4,8,0,1)"),
                Arguments.of(99, "(4,8,1,1)"),
                Arguments.of(100, "(4,16,0,1)"),
                Arguments.of(101, "(4,16,1,1)"),
                Arguments.of(102, "(4,32,0,1)"),
                Arguments.of(103, "(4,32,1,1)"),
                Arguments.of(104, "(4,64,0,1)"),
                Arguments.of(105, "(4,64,1,1)"),
                Arguments.of(106, "(4,128,0,1)"),
                Arguments.of(107, "(4,128,1,1)"),
                Arguments.of(108, "(4,192,0,1)"),
                Arguments.of(109, "(4,192,1,1)"),
                Arguments.of(110, "(4,224,0,1)"),
                Arguments.of(111, "(4,224,1,1)"),
                Arguments.of(112, "(4,240,0,1)"),
                Arguments.of(113, "(4,240,1,1)"),
                Arguments.of(114, "(4,248,0,1)"),
                Arguments.of(115, "(4,248,1,1)")
        );
    }

    // Test canonical codecs
    static Stream<Arguments> canonicalGetSpecifier() {
        return IntStream.range(1, 115).mapToObj(Arguments::of);
    }

    static Stream<Arguments> specifier() {
        return Stream.of(
                Arguments.of(new BHSDCodec(2, 125, 0, 1)),
                Arguments.of(new BHSDCodec(3, 125, 2, 1)),
                Arguments.of(new BHSDCodec(4, 125)),
                Arguments.of(new BHSDCodec(5, 125, 2, 0)),
                Arguments.of(new BHSDCodec(3, 5, 2, 1))
        );
    }

    @ParameterizedTest
    @MethodSource("arbitraryCodec")
    public void testArbitraryCodec(final String expected, final byte[] bytes) throws IOException, Pack200Exception {
        assertEquals(expected, CodecEncoding.getCodec(116, new ByteArrayInputStream(bytes), null).toString());
    }

    @ParameterizedTest
    @MethodSource("canonicalEncodings")
    void testCanonicalEncodings(final int i, final String expectedCodec) throws IOException, Pack200Exception {
        assertEquals(expectedCodec, CodecEncoding.getCodec(i, null, null).toString());
    }

    @ParameterizedTest
    @MethodSource("canonicalGetSpecifier")
    public void testCanonicalGetSpecifier(final int i) throws Pack200Exception, IOException {
        assertEquals(i, CodecEncoding.getSpecifier(CodecEncoding.getCodec(i, null, null), null)[0]);
    }

    @Test
    public void testDefaultCodec() throws Pack200Exception, IOException {
        final Codec defaultCodec = new BHSDCodec(2, 16, 0, 0);
        assertEquals(defaultCodec, CodecEncoding.getCodec(0, null, defaultCodec));
    }

    @Test
    public void testGetSpeciferForPopulationCodec() throws IOException, Pack200Exception {
        final PopulationCodec pCodec = new PopulationCodec(Codec.BYTE1, Codec.CHAR3, Codec.UNSIGNED5);
        final int[] specifiers = CodecEncoding.getSpecifier(pCodec, null);
        assertTrue(specifiers[0] > 140);
        assertTrue(specifiers[0] < 189);
        final byte[] bytes = new byte[specifiers.length - 1];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) specifiers[i+1];
        }
        final InputStream in = new ByteArrayInputStream(bytes);
        final PopulationCodec pCodec2 = (PopulationCodec) CodecEncoding.getCodec(specifiers[0], in, null);
        assertEquals(pCodec.getFavouredCodec(), pCodec2.getFavouredCodec());
        assertEquals(pCodec.getTokenCodec(), pCodec2.getTokenCodec());
        assertEquals(pCodec.getUnfavouredCodec(), pCodec2.getUnfavouredCodec());
    }

    @Test
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

    @ParameterizedTest
    @MethodSource("specifier")
    void testGetSpecifier(final Codec c1) throws IOException, Pack200Exception {
        final int[] specifiers = CodecEncoding.getSpecifier(c1, null);
        assertEquals(3, specifiers.length);
        assertEquals(116, specifiers[0]);
        final byte[] bytes = {(byte) specifiers[1], (byte) specifiers[2]};
        final InputStream in = new ByteArrayInputStream(bytes);
        assertEquals(c1, CodecEncoding.getCodec(116, in, null));
    }

}

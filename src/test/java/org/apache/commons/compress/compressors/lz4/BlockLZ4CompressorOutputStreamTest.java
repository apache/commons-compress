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
package org.apache.commons.compress.compressors.lz4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.compress.compressors.lz77support.LZ77Compressor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class BlockLZ4CompressorOutputStreamTest {

    @Test
    public void pairSeesBackReferenceWhenSet() {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        Assert.assertFalse(p.hasBackReference());
        p.setBackReference(new LZ77Compressor.BackReference(1, 4));
        Assert.assertTrue(p.hasBackReference());
    }

    @Test
    public void canWriteBackReferenceFollowedByLongLiteral() {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(1, 4));
        // a length of 11 would be enough according to the spec, but
        // the algorithm we use for rewriting the last block requires
        // 16 bytes
        Assert.assertTrue(p.canBeWritten(16));
    }

    @Test
    @Ignore("would pass if the algorithm used for rewriting the final pairs was smarter")
    public void canWriteBackReferenceFollowedByShortLiteralIfOffsetIsBigEnough() {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(10, 4));
        Assert.assertTrue(p.canBeWritten(5));
    }

    @Test
    @Ignore("would pass if the algorithm used for rewriting the final pairs was smarter")
    public void canWriteBackReferenceFollowedByShortLiteralIfLengthIsBigEnough() {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(1, 10));
        Assert.assertTrue(p.canBeWritten(5));
    }

    @Test
    public void cantWriteBackReferenceFollowedByLiteralThatIsTooShort() {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(10, 14));
        Assert.assertFalse(p.canBeWritten(4));
    }

    @Test
    public void cantWriteBackReferenceIfAccumulatedOffsetIsTooShort() {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(1, 4));
        Assert.assertFalse(p.canBeWritten(5));
    }

    @Test
    public void pairAccumulatesLengths() {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(1, 4));
        byte[] b = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        p.addLiteral(new LZ77Compressor.LiteralBlock(b, 1, 4));
        p.addLiteral(new LZ77Compressor.LiteralBlock(b, 2, 5));
        Assert.assertEquals(13, p.length());
    }

    @Test
    public void canWritePairWithoutLiterals() throws IOException {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(1, 4));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(bos);
        Assert.assertArrayEquals(new byte[] { 0, 1, 0 }, bos.toByteArray());
    }

    @Test
    public void writesCorrectSizeFor19ByteLengthBackReference() throws IOException {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(1, 19));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(bos);
        Assert.assertArrayEquals(new byte[] { 15, 1, 0, 0 }, bos.toByteArray());
    }

    @Test
    public void writesCorrectSizeFor273ByteLengthBackReference() throws IOException {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(1, 273));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(bos);
        Assert.assertArrayEquals(new byte[] { 15, 1, 0, (byte) 254 }, bos.toByteArray());
    }

    @Test
    public void writesCorrectSizeFor274ByteLengthBackReference() throws IOException {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        p.setBackReference(new LZ77Compressor.BackReference(1, 274));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(bos);
        Assert.assertArrayEquals(new byte[] { 15, 1, 0, (byte) 255, 0 }, bos.toByteArray());
    }

    @Test
    public void canWritePairWithoutBackReference() throws IOException {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        byte[] b = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        p.addLiteral(new LZ77Compressor.LiteralBlock(b, 1, 4));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(bos);
        Assert.assertArrayEquals(new byte[] { 4<<4, 2, 3, 4, 5 }, bos.toByteArray());
    }

    @Test
    public void writesCorrectSizeFor15ByteLengthLiteral() throws IOException {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        byte[] b = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        p.addLiteral(new LZ77Compressor.LiteralBlock(b, 0, 9));
        p.addLiteral(new LZ77Compressor.LiteralBlock(b, 0, 6));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(bos);
        Assert.assertArrayEquals(new byte[] { (byte) (15<<4), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4, 5, 6 },
            bos.toByteArray());
    }

    @Test
    public void writesCorrectSizeFor269ByteLengthLiteral() throws IOException {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        byte[] b = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        for (int i = 0; i < 26; i++) {
            p.addLiteral(new LZ77Compressor.LiteralBlock(b, 0, 10));
        }
        p.addLiteral(new LZ77Compressor.LiteralBlock(b, 0, 9));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(bos);
        Assert.assertArrayEquals(new byte[] { (byte) (15<<4), (byte) 254, 1 },
            Arrays.copyOfRange(bos.toByteArray(), 0, 3));
    }

    @Test
    public void writesCorrectSizeFor270ByteLengthLiteral() throws IOException {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        byte[] b = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        for (int i = 0; i < 27; i++) {
            p.addLiteral(new LZ77Compressor.LiteralBlock(b, 0, 10));
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(bos);
        Assert.assertArrayEquals(new byte[] { (byte) (15<<4), (byte) 255, 0, 1 },
            Arrays.copyOfRange(bos.toByteArray(), 0, 4));
    }

    @Test
    public void writesCompletePair() throws IOException {
        BlockLZ4CompressorOutputStream.Pair p = new BlockLZ4CompressorOutputStream.Pair();
        byte[] b = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        p.addLiteral(new LZ77Compressor.LiteralBlock(b, 1, 4));
        b[2] = 19;
        p.setBackReference(new LZ77Compressor.BackReference(1, 5));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(bos);
        Assert.assertArrayEquals(new byte[] { (4<<4) + 1, 2, 3, 4, 5, 1, 0 },
            bos.toByteArray());
    }

    @Test
    public void rewritingOfFinalBlockWithoutTrailingLZ77Literals() throws IOException {
        for (int i = 1; i < 13; i++) {
            // according to the spec these are all too short be compressed
            // LZ77Compressor will create a single byte literal
            // followed by a back-reference starting with i = 5,
            // though. (4 is the minimum length for a back-reference
            // in LZ4
            byte[] compressed = compress(i);
            byte[] expected = prepareExpected(i + 1);
            expected[0] = (byte) (i<<4);
            Assert.assertArrayEquals("input length is " + i, expected, compressed);
        }

        for (int i = 13; i < 17; i++) {
            // LZ77Compressor will still create a single byte literal
            // followed by a back-reference
            // according to the spec the back-reference could be split
            // as we can cut out a five byte literal and the offset
            // would be big enough, but our algorithm insists on a
            // twelve byte literal trailer and the back-reference
            // would fall below the minimal size
            byte[] compressed = compress(i);
            byte[] expected = prepareExpected(i < 15 ? i + 1 : i + 2);
            if (i < 15) {
                expected[0] = (byte) (i<<4);
            } else {
                expected[0] = (byte) (15<<4);
                expected[1] = (byte) (i - 15);
            }
            Assert.assertArrayEquals("input length is " + i, expected, compressed);
        }

        for (int i = 17; i < 20; i++) {
            // LZ77Compressor will still create a single byte literal
            // followed by a back-reference
            // this time even our algorithm is willing to break up the
            // back-reference
            byte[] compressed = compress(i);
            byte[] expected = prepareExpected(17);
            expected[0] = (byte) ((1<<4) | i - 17);
            // two-byte offset
            expected[2] = 1;
            expected[3] = 0;
            expected[4] = (byte) (12<<4);
            Assert.assertArrayEquals("input length is " + i, expected, compressed);
        }
    }

    @Test
    public void rewritingOfFinalBlockWithTrailingLZ77Literals() throws IOException {
        for (int i = 1; i < 5; i++) {
            // LZ77Compressor will create a single byte literal
            // followed by a back-reference of length 15 followed by a
            // literal of length i
            // we can split the back-reference and merge it with the literal
            byte[] compressed = compress(16, i);
            byte[] expected = prepareExpected(17);
            expected[0] = (byte) ((1<<4) | i - 1);
            // two-byte offset
            expected[2] = 1;
            expected[3] = 0;
            expected[4] = (byte) (12<<4);
            for (int j = 0; j < i; j++) {
                expected[expected.length - 1 - j] = 1;
            }
            Assert.assertArrayEquals("trailer length is " + i, expected, compressed);
        }
        for (int i = 5; i < 12; i++) {
            // LZ77Compressor will create a single byte literal
            // followed by a back-reference of length 15 followed by
            // another single byte literal and another back-reference
            // of length i-1
            // according to the spec we could completely satisfy the
            // requirements by just rewriting the last Pair, but our
            // algorithm will chip off a few bytes from the first Pair
            byte[] compressed = compress(16, i);
            byte[] expected = prepareExpected(17);
            expected[0] = (byte) ((1<<4) | i - 1);
            // two-byte offset
            expected[2] = 1;
            expected[3] = 0;
            expected[4] = (byte) (12<<4);
            for (int j = 0; j < i; j++) {
                expected[expected.length - 1 - j] = 1;
            }
            Assert.assertArrayEquals("trailer length is " + i, expected, compressed);
        }
        for (int i = 12; i < 15; i++) {
            // LZ77Compressor will create a single byte literal
            // followed by a back-reference of length 15 followed by
            // another single byte literal and another back-reference
            // of length i-1
            // this shouldn't affect the first pair at all as
            // rewriting the second one is sufficient
            byte[] compressed = compress(16, i);
            byte[] expected = prepareExpected(i + 5);
            expected[0] = (byte) ((1<<4) | 11);
            // two-byte offset
            expected[2] = 1;
            expected[3] = 0;
            expected[4] = (byte) (i<<4);
            for (int j = 0; j < i; j++) {
                expected[expected.length - 1 - j] = 1;
            }
            Assert.assertArrayEquals("trailer length is " + i, expected, compressed);
        }
    }

    @Test
    public void rewritingOfFourPairs() throws IOException {
        // LZ77Compressor creates three times a literal block followed
        // by a back-reference (once 5 bytes long and twice four bytes
        // long and a final literal block of length 1
        // in the result the three last pairs are merged into a single
        // literal and one byte is chopped off of the first pair's
        // back-reference
        byte[] compressed = compress(6, 5, 5, 1);
        byte[] expected = prepareExpected(17);
        expected[0] = (byte) (1<<4);
        // two-byte offset
        expected[2] = 1;
        expected[3] = 0;
        expected[4] = (byte) (12<<4);
        for (int i = 6; i < 11; i++) {
            expected[i] = 1;
        }
        for (int i = 11; i < 16; i++) {
            expected[i] = 2;
        }
        expected[16] = 3;
        Assert.assertArrayEquals(expected, compressed);
    }

    @Test
    public void rewritingWithFinalBackreferenceAndOffsetBiggerThan1() throws IOException {
        // this caused trouble when expandFromList() fell into the "offsetRemaining is negative" self-copy case as the
        // calculation of copyOffset was wrong
        byte[] toCompress = prepareExpected(25);
        for (int i = 0; i < toCompress.length; i += 4) {
            toCompress[i] = 1;
        }
        // LZ77Compressor creates a four byte literal and a back-reference with offset 4 and length 21
        // we'll need to split the back-reference and chop off the last 12 bytes
        byte[] compressed = compress(toCompress);
        byte[] expected = prepareExpected(1 + 4 + 2 + 1 + 12);
        expected[0] = (byte) ((4<<4) | 5);
        expected[1] = 1;
        expected[5] = 4;
        expected[6] = 0;
        expected[7] = (byte) (12<<4);
        for (int i = 11; i < expected.length; i += 4) {
            expected[i] = 1;
        }
        Assert.assertArrayEquals(expected, compressed);
    }

    private byte[] compress(int length) throws IOException {
        return compress(length, 0);
    }

    private byte[] compress(int lengthBeforeTrailer, int... lengthOfTrailers) throws IOException {
        byte[] b = prepareExpected(lengthBeforeTrailer);
        return compress(b, lengthOfTrailers);
    }

    private byte[] compress(byte[] input, int... lengthOfTrailers) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BlockLZ4CompressorOutputStream lo = new BlockLZ4CompressorOutputStream(baos)) {
            lo.write(input);
            for (int i = 0; i < lengthOfTrailers.length; i++) {
                int lengthOfTrailer = lengthOfTrailers[i];
                for (int j = 0; j < lengthOfTrailer; j++) {
                    lo.write(i + 1);
                }
            }
            lo.close();
            return baos.toByteArray();
        }
    }

    private byte[] prepareExpected(int length) {
        byte[] b = new byte[length];
        Arrays.fill(b, (byte) -1);
        return b;
    }
}

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
}

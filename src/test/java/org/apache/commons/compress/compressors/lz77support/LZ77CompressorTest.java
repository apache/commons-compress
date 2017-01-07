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
package org.apache.commons.compress.compressors.lz77support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LZ77CompressorTest {

    private List<LZ77Compressor.Block> compress(Parameters params, byte[]... chunks) {
        final List<LZ77Compressor.Block> blocks = new ArrayList<>();
        LZ77Compressor c = new LZ77Compressor(params, new LZ77Compressor.Callback() {
                @Override
                public void accept(LZ77Compressor.Block block) {
                    if (block instanceof LZ77Compressor.LiteralBlock) {
                        // replace with a real copy of data so tests
                        // can see the results as they've been when
                        // the callback has been called
                        LZ77Compressor.LiteralBlock b = (LZ77Compressor.LiteralBlock) block;
                        int len = b.getLength();
                        block = new LZ77Compressor.LiteralBlock(
                            Arrays.copyOfRange(b.getData(), b.getOffset(), b.getOffset() + len),
                            0, len);
                    }
                    blocks.add(block);
                }
            });
        for (byte[] chunk : chunks) {
            c.compress(chunk);
        }
        c.finish();
        return blocks;
    }

    @Test
    public void nonCompressableWithLengthSmallerThanLiteralMax() {
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        List<LZ77Compressor.Block> blocks = compress(new Parameters(128), data);
        assertEquals(2, blocks.size());
        assertEquals(LZ77Compressor.LiteralBlock.class, blocks.get(0).getClass());
        assertArrayEquals(data, ((LZ77Compressor.LiteralBlock) blocks.get(0)).getData());
        assertEquals(LZ77Compressor.EOD.class, blocks.get(1).getClass());
    }

    @Test
    public void nonCompressableWithLengthGreaterThanLiteralMaxButLessThanTwiceWindowSize() {
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        List<LZ77Compressor.Block> blocks = compress(new Parameters(8), data);
        assertEquals(3, blocks.size());
        assertEquals(LZ77Compressor.LiteralBlock.class, blocks.get(0).getClass());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 },
            ((LZ77Compressor.LiteralBlock) blocks.get(0)).getData());
        assertEquals(LZ77Compressor.LiteralBlock.class, blocks.get(1).getClass());
        assertArrayEquals(new byte[] { 9, 10 },
            ((LZ77Compressor.LiteralBlock) blocks.get(1)).getData());
        assertEquals(LZ77Compressor.EOD.class, blocks.get(2).getClass());
    }

    @Test
    public void nonCompressableWithLengthThatForcesWindowSlide() {
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        List<LZ77Compressor.Block> blocks = compress(new Parameters(4), data);
        assertEquals(4, blocks.size());
        assertEquals(LZ77Compressor.LiteralBlock.class, blocks.get(0).getClass());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 },
            ((LZ77Compressor.LiteralBlock) blocks.get(0)).getData());
        assertEquals(LZ77Compressor.LiteralBlock.class, blocks.get(1).getClass());
        assertArrayEquals(new byte[] { 5, 6, 7, 8 },
            ((LZ77Compressor.LiteralBlock) blocks.get(1)).getData());
        assertEquals(LZ77Compressor.LiteralBlock.class, blocks.get(2).getClass());
        assertArrayEquals(new byte[] { 9, 10 },
            ((LZ77Compressor.LiteralBlock) blocks.get(2)).getData());
        assertEquals(LZ77Compressor.EOD.class, blocks.get(3).getClass());
    }

    @Test
    public void nonCompressableSentAsSingleBytes() {
        List<LZ77Compressor.Block> blocks = compress(new Parameters(8), new byte[][] {
                { 1 }, { 2 }, { 3 } , { 4 }, { 5 },
                { 6 }, { 7 }, { 8 } , { 9 }, { 10 },
            });
        assertEquals(3, blocks.size());
        assertEquals(LZ77Compressor.LiteralBlock.class, blocks.get(0).getClass());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 },
            ((LZ77Compressor.LiteralBlock) blocks.get(0)).getData());
        assertEquals(LZ77Compressor.LiteralBlock.class, blocks.get(1).getClass());
        assertArrayEquals(new byte[] { 9, 10 },
            ((LZ77Compressor.LiteralBlock) blocks.get(1)).getData());
        assertEquals(LZ77Compressor.EOD.class, blocks.get(2).getClass());
    }

}

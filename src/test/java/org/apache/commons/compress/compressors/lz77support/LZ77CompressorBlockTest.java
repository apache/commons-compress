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
package org.apache.commons.compress.compressors.lz77support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.compress.compressors.lz77support.LZ77Compressor.BackReference;
import org.apache.commons.compress.compressors.lz77support.LZ77Compressor.Block;
import org.apache.commons.compress.compressors.lz77support.LZ77Compressor.Block.BlockType;
import org.apache.commons.compress.compressors.lz77support.LZ77Compressor.EOD;
import org.apache.commons.compress.compressors.lz77support.LZ77Compressor.LiteralBlock;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link LZ77Compressor.Block}.
 */
class LZ77CompressorBlockTest {

    @SuppressWarnings("deprecation")
    static final class DeprecatedBlock extends Block {
    }

    @Test
    void testBackReferenceBlockToString() {
        assertTrue(new BackReference(1, 2).toString().contains(BlockType.BACK_REFERENCE.name()));
    }

    @Test
    void testDeprecatedBlock() {
        assertNotNull(new DeprecatedBlock().toString().contains(DeprecatedBlock.class.getSimpleName()));
    }

    @Test
    void testEodBlockToString() {
        assertTrue(new EOD().toString().contains(BlockType.EOD.name()));
    }

    @Test
    void testLiteralBlockToString() {
        assertTrue(new LiteralBlock(new byte[10], 1, 2).toString().contains(BlockType.LITERAL.name()));
    }

}

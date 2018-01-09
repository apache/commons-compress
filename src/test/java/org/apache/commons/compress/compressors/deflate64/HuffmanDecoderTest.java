/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.compressors.deflate64;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HuffmanDecoderTest {
    @Test
    public void decodeUncompressedBlock() throws Exception {
        byte[] data = {
                0b1, // end of block + no compression mode
                11, 0, -12, -1, // len & ~len
                'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'
        };

        HuffmanDecoder decoder = new HuffmanDecoder(new ByteArrayInputStream(data));
        byte[] result = new byte[100];
        int len = decoder.decode(result);

        assertEquals(11, len);
        assertEquals("Hello World", new String(result, 0, len));
    }

    @Test
    public void decodeUncompressedBlockWithInvalidLenNLenValue() throws Exception {
        byte[] data = {
                0b1, // end of block + no compression mode
                11, 0, -12, -2, // len & ~len
                'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'
        };

        HuffmanDecoder decoder = new HuffmanDecoder(new ByteArrayInputStream(data));
        byte[] result = new byte[100];
        try {
            int len = decoder.decode(result);
            fail("Should have failed but returned " + len + " entries: " + Arrays.toString(Arrays.copyOf(result, len)));
        } catch (IllegalStateException e) {
            assertEquals("Illegal LEN / NLEN values", e.getMessage());
        }
    }

    @Test
    public void decodeSimpleFixedHuffmanBlock() throws Exception {
        byte[] data = {
                //|--- binary filling ---|76543210
                0b11111111111111111111111111110011, // final block + fixed huffman + H
                0b00000000000000000000000001001000, // H + e
                0b11111111111111111111111111001101, // e + l
                0b11111111111111111111111111001001, // l + l
                0b11111111111111111111111111001001, // l + o
                0b00000000000000000000000001010111, // o + ' '
                0b00000000000000000000000000001000, // ' ' + W
                0b11111111111111111111111111001111, // W + o
                0b00000000000000000000000000101111, // o + r
                0b11111111111111111111111111001010, // r + l
                0b00000000000000000000000001001001, // l + d
                0b00000000000000000000000000000001, // d + end of block
                0b11111111111111111111111111111100 // end of block (00) + garbage
        };

        HuffmanDecoder decoder = new HuffmanDecoder(new ByteArrayInputStream(data));
        byte[] result = new byte[100];
        int len = decoder.decode(result);

        assertEquals(11, len);
        assertEquals("Hello World", new String(result, 0, len));
    }

    @Test
    public void decodeSimpleFixedHuffmanBlockToSmallBuffer() throws Exception {
        byte[] data = {
                //|--- binary filling ---|76543210
                0b11111111111111111111111111110011, // final block + fixed huffman + H
                0b00000000000000000000000001001000, // H + e
                0b11111111111111111111111111001101, // e + l
                0b11111111111111111111111111001001, // l + l
                0b11111111111111111111111111001001, // l + o
                0b00000000000000000000000001010111, // o + ' '
                0b00000000000000000000000000001000, // ' ' + W
                0b11111111111111111111111111001111, // W + o
                0b00000000000000000000000000101111, // o + r
                0b11111111111111111111111111001010, // r + l
                0b00000000000000000000000001001001, // l + d
                0b00000000000000000000000000000001, // d + end of block
                0b11111111111111111111111111111100 // end of block (00) + garbage
        };

        HuffmanDecoder decoder = new HuffmanDecoder(new ByteArrayInputStream(data));
        byte[] result = new byte[10];
        int len;
        len = decoder.decode(result);
        assertEquals(10, len);
        assertEquals("Hello Worl", new String(result, 0, len));
        len = decoder.decode(result);
        assertEquals(1, len);
        assertEquals("d", new String(result, 0, len));
    }


    @Test
    public void decodeFixedHuffmanBlockWithMemoryLookup() throws Exception {
        byte[] data = {
                //|--- binary filling ---|76543210
                0b11111111111111111111111111110011, // final block + fixed huffman + H
                0b00000000000000000000000001001000, // H + e
                0b11111111111111111111111111001101, // e + l
                0b11111111111111111111111111001001, // l + l
                0b11111111111111111111111111001001, // l + o
                0b00000000000000000000000001010111, // o + ' '
                0b00000000000000000000000000001000, // ' ' + W
                0b11111111111111111111111111001111, // W + o
                0b00000000000000000000000000101111, // o + r
                0b11111111111111111111111111001010, // r + l
                0b00000000000000000000000001001001, // l + d
                0b11111111111111111111111111100001, // d + '\n'
                0b00000000000000000000000000100010, // '\n' + <len>
                0b11111111111111111111111110000110, // <len> + offset <001> + dist6
                0b00000000000000000000000000001101, // dist6 + offset <11> + end of block (000000)
                0b11111111111111111111111111111000 // end of block (0000) + garbage
        };

        HuffmanDecoder decoder = new HuffmanDecoder(new ByteArrayInputStream(data));
        byte[] result = new byte[100];
        int len = decoder.decode(result);

        assertEquals(48, len);
        assertEquals("Hello World\nHello World\nHello World\nHello World\n", new String(result, 0, len));
    }

    @Test
    public void decodeFixedHuffmanBlockWithMemoryLookupInSmallBuffer() throws Exception {
        byte[] data = {
                //|--- binary filling ---|76543210
                0b11111111111111111111111111110011, // final block + fixed huffman + H
                0b00000000000000000000000001001000, // H + e
                0b11111111111111111111111111001101, // e + l
                0b11111111111111111111111111001001, // l + l
                0b11111111111111111111111111001001, // l + o
                0b00000000000000000000000001010111, // o + ' '
                0b00000000000000000000000000001000, // ' ' + W
                0b11111111111111111111111111001111, // W + o
                0b00000000000000000000000000101111, // o + r
                0b11111111111111111111111111001010, // r + l
                0b00000000000000000000000001001001, // l + d
                0b11111111111111111111111111100001, // d + '\n'
                0b00000000000000000000000000100010, // '\n' + <len>
                0b11111111111111111111111110000110, // <len> + offset <001> + dist6
                0b00000000000000000000000000001101, // dist6 + offset <11> + end of block (000000)
                0b11111111111111111111111111111000 // end of block (0000) + garbage
        };

        HuffmanDecoder decoder = new HuffmanDecoder(new ByteArrayInputStream(data));
        byte[] result = new byte[30];
        int len;

        len = decoder.decode(result);
        assertEquals(30, len);
        assertEquals("Hello World\nHello World\nHello ", new String(result, 0, len));

        len = decoder.decode(result);
        assertEquals(18, len);
        assertEquals("World\nHello World\n", new String(result, 0, len));
    }

    @Test
    public void decodeFixedHuffmanBlockWithMemoryLookupInExactBuffer() throws Exception {
        byte[] data = {
                //|--- binary filling ---|76543210
                0b11111111111111111111111111110011, // final block + fixed huffman + H
                0b00000000000000000000000001001000, // H + e
                0b11111111111111111111111111001101, // e + l
                0b11111111111111111111111111001001, // l + l
                0b11111111111111111111111111001001, // l + o
                0b00000000000000000000000001010111, // o + ' '
                0b00000000000000000000000000001000, // ' ' + W
                0b11111111111111111111111111001111, // W + o
                0b00000000000000000000000000101111, // o + r
                0b11111111111111111111111111001010, // r + l
                0b00000000000000000000000001001001, // l + d
                0b11111111111111111111111111100001, // d + '\n'
                0b00000000000000000000000000100010, // '\n' + <len>
                0b11111111111111111111111110000110, // <len> + offset <001> + dist6
                0b00000000000000000000000000001101, // dist6 + offset <11> + end of block (000000)
                0b11111111111111111111111111111000 // end of block (0000) + garbage
        };

        HuffmanDecoder decoder = new HuffmanDecoder(new ByteArrayInputStream(data));
        byte[] result = new byte[48];
        int len;

        len = decoder.decode(result);
        assertEquals(48, len);
        assertEquals("Hello World\nHello World\nHello World\nHello World\n", new String(result, 0, len));

        len = decoder.decode(result);
        assertEquals(0, len);

        len = decoder.decode(result);
        assertEquals(-1, len);
    }
}

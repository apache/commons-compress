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

package org.apache.commons.compress.utils;

import static org.apache.commons.compress.utils.ByteUtils.fromLittleEndian;
import static org.apache.commons.compress.utils.ByteUtils.toLittleEndian;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.compress.utils.ByteUtils.InputStreamByteSupplier;
import org.apache.commons.compress.utils.ByteUtils.OutputStreamByteConsumer;
import org.junit.jupiter.api.Test;

class ByteUtilsTest {

    @Test
    void testFromLittleEndianFromArray() {
        final byte[] b = { 1, 2, 3, 4, 5 };
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(b, 1, 3));
    }

    @Test
    void testFromLittleEndianFromArrayOneArg() {
        final byte[] b = { 2, 3, 4 };
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(b));
    }

    @Test
    void testFromLittleEndianFromArrayOneArgThrowsForLengthTooBig() {
        assertThrows(IllegalArgumentException.class, () -> fromLittleEndian(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
    }

    @Test
    void testFromLittleEndianFromArrayOneArgUnsignedInt32() {
        final byte[] b = { 2, 3, 4, (byte) 128 };
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, fromLittleEndian(b));
    }

    @Test
    void testFromLittleEndianFromArrayThrowsForLengthTooBig() {
        assertThrows(IllegalArgumentException.class, () -> fromLittleEndian(ByteUtils.EMPTY_BYTE_ARRAY, 0, 9));
    }

    @Test
    void testFromLittleEndianFromArrayUnsignedInt32() {
        final byte[] b = { 1, 2, 3, 4, (byte) 128 };
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, fromLittleEndian(b, 1, 4));
    }

    @Test
    void testFromLittleEndianFromDataInput() throws IOException {
        final DataInput din = new DataInputStream(new ByteArrayInputStream(new byte[] { 2, 3, 4, 5 }));
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(din, 3));
    }

    @Test
    void testFromLittleEndianFromDataInputThrowsForLengthTooBig() {
        final DataInput din = new DataInputStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY));
        assertThrows(IllegalArgumentException.class, () -> fromLittleEndian(din, 9));
    }

    @Test
    void testFromLittleEndianFromDataInputThrowsForPrematureEnd() {
        final DataInput din = new DataInputStream(new ByteArrayInputStream(new byte[] { 2, 3 }));
        assertThrows(EOFException.class, () -> fromLittleEndian(din, 3));
    }

    @Test
    void testFromLittleEndianFromDataInputUnsignedInt32() throws IOException {
        final DataInput din = new DataInputStream(new ByteArrayInputStream(new byte[] { 2, 3, 4, (byte) 128 }));
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, fromLittleEndian(din, 4));
    }

    @Test
    void testFromLittleEndianFromStream() throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3, 4, 5 });
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(bin, 3));
    }

    @Test
    void testFromLittleEndianFromStreamThrowsForLengthTooBig() {
        assertThrows(IllegalArgumentException.class, () -> fromLittleEndian(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY), 9));
    }

    @Test
    void testFromLittleEndianFromStreamThrowsForPrematureEnd() {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3 });
        assertThrows(IOException.class, () -> fromLittleEndian(bin, 3));
    }

    @Test
    void testFromLittleEndianFromStreamUnsignedInt32() throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3, 4, (byte) 128 });
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, fromLittleEndian(bin, 4));
    }

    @Test
    void testFromLittleEndianFromSupplier() throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3, 4, 5 });
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(new InputStreamByteSupplier(bin), 3));
    }

    @Test
    void testFromLittleEndianFromSupplierThrowsForLengthTooBig() {
        assertThrows(IllegalArgumentException.class,
                () -> fromLittleEndian(new InputStreamByteSupplier(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY)), 9));
    }

    @Test
    void testFromLittleEndianFromSupplierThrowsForPrematureEnd() {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3 });
        assertThrows(IOException.class, () -> fromLittleEndian(new InputStreamByteSupplier(bin), 3));
    }

    @Test
    void testFromLittleEndianFromSupplierUnsignedInt32() throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3, 4, (byte) 128 });
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, fromLittleEndian(new InputStreamByteSupplier(bin), 4));
    }

    @Test
    void testToLittleEndianToByteArray() {
        final byte[] b = new byte[4];
        toLittleEndian(b, 2 + 3 * 256 + 4 * 256 * 256, 1, 3);
        assertArrayEquals(new byte[] { 2, 3, 4 }, Arrays.copyOfRange(b, 1, 4));
    }

    @Test
    void testToLittleEndianToByteArrayUnsignedInt32() {
        final byte[] b = new byte[4];
        toLittleEndian(b, 2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, 0, 4);
        assertArrayEquals(new byte[] { 2, 3, 4, (byte) 128 }, b);
    }

    @Test
    void testToLittleEndianToConsumer() throws IOException {
        final byte[] byteArray;
        final byte[] expected = { 2, 3, 4 };
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            toLittleEndian(new OutputStreamByteConsumer(bos), 2 + 3 * 256 + 4 * 256 * 256, 3);
            byteArray = bos.toByteArray();
            assertArrayEquals(expected, byteArray);
        }
        assertArrayEquals(expected, byteArray);
    }

    @Test
    void testToLittleEndianToConsumerUnsignedInt32() throws IOException {
        final byte[] byteArray;
        final byte[] expected = { 2, 3, 4, (byte) 128 };
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            toLittleEndian(new OutputStreamByteConsumer(bos), 2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, 4);
            byteArray = bos.toByteArray();
            assertArrayEquals(expected, byteArray);
        }
        assertArrayEquals(expected, byteArray);
    }

    @Test
    void testToLittleEndianToDataOutput() throws IOException {
        final byte[] byteArray;
        final byte[] expected = { 2, 3, 4 };
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final DataOutput dos = new DataOutputStream(bos);
            toLittleEndian(dos, 2 + 3 * 256 + 4 * 256 * 256, 3);
            byteArray = bos.toByteArray();
            assertArrayEquals(expected, byteArray);
        }
        assertArrayEquals(expected, byteArray);
    }

    @Test
    void testToLittleEndianToDataOutputUnsignedInt32() throws IOException {
        final byte[] byteArray;
        final byte[] expected = { 2, 3, 4, (byte) 128 };
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final DataOutput dos = new DataOutputStream(bos);
            toLittleEndian(dos, 2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, 4);
            byteArray = bos.toByteArray();
            assertArrayEquals(expected, byteArray);
        }
        assertArrayEquals(expected, byteArray);
    }

    @Test
    void testToLittleEndianToStream() throws IOException {
        final byte[] byteArray;
        final byte[] expected = { 2, 3, 4 };
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            toLittleEndian(bos, 2 + 3 * 256 + 4 * 256 * 256, 3);
            byteArray = bos.toByteArray();
            assertArrayEquals(expected, byteArray);
        }
        assertArrayEquals(expected, byteArray);
    }

    @Test
    void testToLittleEndianToStreamUnsignedInt32() throws IOException {
        final byte[] byteArray;
        final byte[] expected = { 2, 3, 4, (byte) 128 };
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            toLittleEndian(bos, 2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, 4);
            byteArray = bos.toByteArray();
            assertArrayEquals(expected, byteArray);
        }
        assertArrayEquals(expected, byteArray);
    }
}

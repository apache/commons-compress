/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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

public class ByteUtilsTest {

    @Test
    public void fromLittleEndianFromArray() {
        final byte[] b = { 1, 2, 3, 4, 5 };
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(b, 1, 3));
    }

    @Test
    public void fromLittleEndianFromArrayOneArg() {
        final byte[] b = { 2, 3, 4 };
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(b));
    }

    @Test
    public void fromLittleEndianFromArrayOneArgThrowsForLengthTooBig() {
        assertThrows(IllegalArgumentException.class, () -> fromLittleEndian(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
    }

    @Test
    public void fromLittleEndianFromArrayOneArgUnsignedInt32() {
        final byte[] b = { 2, 3, 4, (byte) 128 };
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, fromLittleEndian(b));
    }

    @Test
    public void fromLittleEndianFromArrayThrowsForLengthTooBig() {
        assertThrows(IllegalArgumentException.class, () -> fromLittleEndian(ByteUtils.EMPTY_BYTE_ARRAY, 0, 9));
    }

    @Test
    public void fromLittleEndianFromArrayUnsignedInt32() {
        final byte[] b = { 1, 2, 3, 4, (byte) 128 };
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, fromLittleEndian(b, 1, 4));
    }

    @Test
    public void fromLittleEndianFromDataInput() throws IOException {
        final DataInput din = new DataInputStream(new ByteArrayInputStream(new byte[] { 2, 3, 4, 5 }));
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(din, 3));
    }

    @Test
    public void fromLittleEndianFromDataInputThrowsForLengthTooBig() {
        final DataInput din = new DataInputStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY));
        assertThrows(IllegalArgumentException.class, () -> fromLittleEndian(din, 9));
    }

    @Test
    public void fromLittleEndianFromDataInputThrowsForPrematureEnd() {
        final DataInput din = new DataInputStream(new ByteArrayInputStream(new byte[] { 2, 3 }));
        assertThrows(EOFException.class, () -> fromLittleEndian(din, 3));
    }

    @Test
    public void fromLittleEndianFromDataInputUnsignedInt32() throws IOException {
        final DataInput din = new DataInputStream(new ByteArrayInputStream(new byte[] { 2, 3, 4, (byte) 128 }));
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, fromLittleEndian(din, 4));
    }

    @Test
    public void fromLittleEndianFromStream() throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3, 4, 5 });
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(bin, 3));
    }

    @Test
    public void fromLittleEndianFromStreamThrowsForLengthTooBig() {
        assertThrows(IllegalArgumentException.class, () -> fromLittleEndian(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY), 9));
    }

    @Test
    public void fromLittleEndianFromStreamThrowsForPrematureEnd() {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3 });
        assertThrows(IOException.class, () -> fromLittleEndian(bin, 3));
    }

    @Test
    public void fromLittleEndianFromStreamUnsignedInt32() throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3, 4, (byte) 128 });
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, fromLittleEndian(bin, 4));
    }

    @Test
    public void fromLittleEndianFromSupplier() throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3, 4, 5 });
        assertEquals(2 + 3 * 256 + 4 * 256 * 256, fromLittleEndian(new InputStreamByteSupplier(bin), 3));
    }

    @Test
    public void fromLittleEndianFromSupplierThrowsForLengthTooBig() {
        assertThrows(IllegalArgumentException.class, () -> fromLittleEndian(new InputStreamByteSupplier(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY)), 9));
    }

    @Test
    public void fromLittleEndianFromSupplierThrowsForPrematureEnd() {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3 });
        assertThrows(IOException.class, () -> fromLittleEndian(new InputStreamByteSupplier(bin), 3));
    }

    @Test
    public void fromLittleEndianFromSupplierUnsignedInt32() throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(new byte[] { 2, 3, 4, (byte) 128 });
        assertEquals(2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256,
            fromLittleEndian(new InputStreamByteSupplier(bin), 4));
    }

    @Test
    public void toLittleEndianToByteArray() {
        final byte[] b = new byte[4];
        toLittleEndian(b, 2 + 3 * 256 + 4 * 256 * 256, 1, 3);
        assertArrayEquals(new byte[] { 2, 3, 4 }, Arrays.copyOfRange(b, 1, 4));
    }

    @Test
    public void toLittleEndianToByteArrayUnsignedInt32() {
        final byte[] b = new byte[4];
        toLittleEndian(b, 2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, 0, 4);
        assertArrayEquals(new byte[] { 2, 3, 4, (byte) 128 }, b);
    }

    @Test
    public void toLittleEndianToConsumer() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        toLittleEndian(new OutputStreamByteConsumer(bos), 2 + 3 * 256 + 4 * 256 * 256, 3);
        bos.close();
        assertArrayEquals(new byte[] { 2, 3, 4 }, bos.toByteArray());
    }

    @Test
    public void toLittleEndianToConsumerUnsignedInt32() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        toLittleEndian(new OutputStreamByteConsumer(bos), 2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, 4);
        bos.close();
        assertArrayEquals(new byte[] { 2, 3, 4, (byte) 128 }, bos.toByteArray());
    }

    @Test
    public void toLittleEndianToDataOutput() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutput dos = new DataOutputStream(bos);
        toLittleEndian(dos, 2 + 3 * 256 + 4 * 256 * 256, 3);
        bos.close();
        assertArrayEquals(new byte[] { 2, 3, 4 }, bos.toByteArray());
    }

    @Test
    public void toLittleEndianToDataOutputUnsignedInt32() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutput dos = new DataOutputStream(bos);
        toLittleEndian(dos, 2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, 4);
        bos.close();
        assertArrayEquals(new byte[] { 2, 3, 4, (byte) 128 }, bos.toByteArray());
    }


    @Test
    public void toLittleEndianToStream() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        toLittleEndian(bos, 2 + 3 * 256 + 4 * 256 * 256, 3);
        bos.close();
        assertArrayEquals(new byte[] { 2, 3, 4 }, bos.toByteArray());
    }

    @Test
    public void toLittleEndianToStreamUnsignedInt32() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        toLittleEndian(bos, 2 + 3 * 256 + 4 * 256 * 256 + 128L * 256 * 256 * 256, 4);
        bos.close();
        assertArrayEquals(new byte[] { 2, 3, 4, (byte) 128 }, bos.toByteArray());
    }
}

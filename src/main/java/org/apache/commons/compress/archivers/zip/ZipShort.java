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
 */
package org.apache.commons.compress.archivers.zip;

import java.io.Serializable;

import org.apache.commons.compress.utils.ByteUtils;

/**
 * Utility class that represents a two byte integer with conversion rules for the little-endian byte order of ZIP files.
 *
 * @Immutable
 */
public final class ZipShort implements Cloneable, Serializable {

    private static final int SIZE = 2;

    /**
     * ZipShort with a value of 0.
     *
     * @since 1.14
     */
    public static final ZipShort ZERO = new ZipShort(0);

    private static final long serialVersionUID = 1L;

    /**
     * Gets value as two bytes in big-endian byte order.
     *
     * @param value the Java int to convert to bytes
     * @return the converted int as a byte array in big-endian byte order
     */
    public static byte[] getBytes(final int value) {
        final byte[] result = new byte[SIZE];
        putShort(value, result, 0);
        return result;
    }

    /**
     * Helper method to get the value as a Java int from a two-byte array
     *
     * @param bytes the array of bytes
     * @return the corresponding Java int value
     */
    public static int getValue(final byte[] bytes) {
        return getValue(bytes, 0);
    }

    /**
     * Helper method to get the value as a Java int from two bytes starting at given array offset
     *
     * @param bytes  the array of bytes
     * @param offset the offset to start
     * @return the corresponding Java int value
     */
    public static int getValue(final byte[] bytes, final int offset) {
        return (int) ByteUtils.fromLittleEndian(bytes, offset, SIZE);
    }

    /**
     * put the value as two bytes in big-endian byte order.
     *
     * @param value  the Java int to convert to bytes
     * @param buf    the output buffer
     * @param offset The offset within the output buffer of the first byte to be written. must be non-negative and no larger than {@code buf.length-2}
     */
    public static void putShort(final int value, final byte[] buf, final int offset) {
        ByteUtils.toLittleEndian(buf, value, offset, SIZE);
    }

    private final int value;

    /**
     * Constructs a new instance from bytes.
     *
     * @param bytes the bytes to store as a ZipShort
     */
    public ZipShort(final byte[] bytes) {
        this(bytes, 0);
    }

    /**
     * Constructs a new instance from the two bytes starting at offset.
     *
     * @param bytes  the bytes to store as a ZipShort
     * @param offset the offset to start
     */
    public ZipShort(final byte[] bytes, final int offset) {
        value = getValue(bytes, offset);
    }

    /**
     * Constructs a new instance from a number.
     *
     * @param value the int to store as a ZipShort
     */
    public ZipShort(final int value) {
        this.value = value;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException cnfe) {
            // impossible
            throw new UnsupportedOperationException(cnfe); // NOSONAR
        }
    }

    /**
     * Override to make two instances with same value equal.
     *
     * @param o an object to compare
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof ZipShort)) {
            return false;
        }
        return value == ((ZipShort) o).getValue();
    }

    /**
     * Gets value as two bytes in big-endian byte order.
     *
     * @return the value as a two byte array in big-endian byte order
     */
    public byte[] getBytes() {
        final byte[] result = new byte[SIZE];
        ByteUtils.toLittleEndian(result, value, 0, SIZE);
        return result;
    }

    /**
     * Gets value as Java int.
     *
     * @return value as a Java int
     */
    public int getValue() {
        return value;
    }

    /**
     * Override to make two instances with same value equal.
     *
     * @return the value stored in the ZipShort
     */
    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "ZipShort value: " + value;
    }
}

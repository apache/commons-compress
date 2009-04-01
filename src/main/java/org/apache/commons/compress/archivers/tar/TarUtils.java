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
package org.apache.commons.compress.archivers.tar;

/**
 * This class provides static utility methods to work with byte streams.
 *
 * @Immutable
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF (bc)
public class TarUtils {

    private static final int BYTE_MASK = 255;

    /**
     * Parse an octal string from a buffer.
     * Leading spaces are ignored.
     * Parsing stops when a NUL is found, or a trailing space,
     * or the buffer length is reached.
     *
     * Behaviour with non-octal input is currently undefined.
     * 
     * @param buffer The buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The maximum number of bytes to parse.
     * @return The long value of the octal string.
     */
    public static long parseOctal(byte[] buffer, int offset, int length) {
        long    result = 0;
        boolean stillPadding = true;
        int     end = offset + length;

        for (int i = offset; i < end; ++i) {
            if (buffer[i] == 0) { // Found trailing null
                break;
            }

            // Ignore leading spaces ('0' can be ignored anyway)
            if (buffer[i] == (byte) ' ' || buffer[i] == '0') {
                if (stillPadding) {
                    continue;
                }

                if (buffer[i] == (byte) ' ') { // Found trailing space
                    break;
                }
            }

            stillPadding = false;
            // CheckStyle:MagicNumber OFF
            result = (result << 3) + (buffer[i] - '0');// TODO needs to reject invalid bytes
            // CheckStyle:MagicNumber ON
        }

        return result;
    }

    /**
     * Parse an entry name from a buffer.
     * Parsing stops when a NUL is found
     * or the buffer length is reached.
     *
     * @param buffer The buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The maximum number of bytes to parse.
     * @return The entry name.
     */
    public static StringBuffer parseName(byte[] buffer, int offset, int length) {
        StringBuffer result = new StringBuffer(length);
        int          end = offset + length;

        for (int i = offset; i < end; ++i) {
            if (buffer[i] == 0) { // Trailing null
                break;
            }

            result.append((char) buffer[i]);
        }

        return result;
    }

    /**
     * Copy a name (StringBuffer) into a buffer.
     * Copies characters from the name into the buffer
     * starting at the specified offset. 
     * If the buffer is longer than the name, the buffer
     * is filled with trailing NULs.
     * If the name is longer than the buffer,
     * the output is truncated.
     *
     * @param name The header name from which to copy the characters.
     * @param buf The buffer where the name is to be stored.
     * @param offset The starting offset into the buffer
     * @param length The maximum number of header bytes to copy.
     * @return The updated offset, i.e. offset + length
     */
    public static int getNameBytes(StringBuffer name, byte[] buf, int offset, int length) {
        int i;

        // copy until end of input or output is reached.
        for (i = 0; i < length && i < name.length(); ++i) {
            buf[offset + i] = (byte) name.charAt(i);
        }

        // Pad any remaining output bytes with NUL
        for (; i < length; ++i) {
            buf[offset + i] = 0;
        }

        return offset + length;
    }

    /**
     * Fill buffer with octal number, with leading zeroes
     * 
     * The output for negative numbers is not specified,
     * but currently the method returns a buffer filled with zeros.
     * This may change.
     * 
     * @param value number to convert to octal (assumed >=0)
     * @param buffer destination buffer
     * @param offset starting offset in buffer
     * @param length length of buffer to fill
     */
    public static void formatUnsignedOctalString(long value, byte[] buffer,
            int offset, int length) {
        length--;
        if (value == 0) {
            buffer[offset + length--] = (byte) '0';
        } else {
            for (long val = value; length >= 0 && val > 0; --length) {
                // CheckStyle:MagicNumber OFF
                buffer[offset + length] = (byte) ((byte) '0' + (byte) (val & 7));
                val = val >> 3;
                // CheckStyle:MagicNumber ON
            }
        }

        for (; length >= 0; --length) { // leading zeros
            buffer[offset + length] = (byte) '0';
        }
    }

    /**
     * Write an octal integer into a buffer.
     *
     * Adds a trailing space and NUL to end of the buffer.
     * [Appears to be standard for V7 Unix BSD]
     * Converts the long value (assumed positive) to the buffer.
     * Adds leading zeros to the buffer.
     * 
     * @param value The value to write
     * @param buf The buffer to receive the output
     * @param offset The starting offset into the buffer
     * @param length The size of the output buffer
     * @return The updated offset, i.e offset+length
     */
    public static int getOctalBytes(long value, byte[] buf, int offset, int length) {

        int idx=length-2; // For space and trailing null
        formatUnsignedOctalString(value, buf, offset, idx);

        buf[offset + idx++] = (byte) ' '; // Trailing space
        buf[offset + idx]   = 0; // Trailing null

        return offset + length;
    }

    /**
     * Write an octal long integer into a buffer.
     * Converts the long value (assumed positive) to the buffer.
     * Adds leading zeros to the buffer.
     * The buffer is terminated with a space.
     * 
     * @param value The value to write as octal
     * @param buf The destinationbuffer.
     * @param offset The starting offset into the buffer.
     * @param length The length of the buffer
     * @return The updated offset
     */
    public static int getLongOctalBytes(long value, byte[] buf, int offset, int length) {

        int idx=length-1; // For space
        
        formatUnsignedOctalString(value, buf, offset, idx);
        buf[offset + idx] = (byte) ' '; // Trailing space

        return offset + length;
    }

    /**
     * Writes an octal value into a buffer.
     *
     * Converts the long value (assumed positive) to the buffer.
     * Adds leading zeros to the buffer.
     * Checksum is followed by NUL and then space.
     *
     * @param value The value to convert
     * @param buf The destination buffer
     * @param offset The starting offset into the buffer.
     * @param length The size of the buffer.
     * @return The updated value of offset, i.e. offset+length
     */
    public static int getCheckSumOctalBytes(long value, byte[] buf, int offset, int length) {

        int idx=length-2; // for NUL and space
        formatUnsignedOctalString(value, buf, offset, idx);

        buf[offset + idx++]   = 0; // Trailing null
        buf[offset + idx]     = (byte) ' '; // Trailing space

        return offset + length;
    }

    /**
     * Compute the checksum of a tar entry header.
     *
     * @param buf The tar entry's header buffer.
     * @return The computed checksum.
     */
    public static long computeCheckSum(byte[] buf) {
        long sum = 0;

        for (int i = 0; i < buf.length; ++i) {
            sum += BYTE_MASK & buf[i];
        }

        return sum;
    }
}

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
package org.apache.commons.compress.archivers.cpio;

import java.util.Arrays;

/**
 * Package private utility class for Cpio
 *
 * @Immutable
 */
class CpioUtil {

    /**
     * Converts a byte array to a long. Halfwords can be swapped by setting
     * swapHalfWord=true.
     *
     * @param number
     *            An array of bytes containing a number
     * @param swapHalfWord
     *            Swap halfwords ([0][1][2][3]->[1][0][3][2])
     * @return The long value
     * @throws UnsupportedOperationException if number length is not a multiple of 2
     */
    static long byteArray2long(final byte[] number, final boolean swapHalfWord) {
        if (number.length % 2 != 0) {
            throw new UnsupportedOperationException();
        }

        long ret;
        int pos = 0;
        final byte[] tmpNumber = Arrays.copyOf(number, number.length);

        if (!swapHalfWord) {
            byte tmp = 0;
            for (pos = 0; pos < tmpNumber.length; pos++) {
                tmp = tmpNumber[pos];
                tmpNumber[pos++] = tmpNumber[pos];
                tmpNumber[pos] = tmp;
            }
        }

        ret = tmpNumber[0] & 0xFF;
        for (pos = 1; pos < tmpNumber.length; pos++) {
            ret <<= 8;
            ret |= tmpNumber[pos] & 0xFF;
        }
        return ret;
    }

    /**
     * Extracts the file type bits from a mode.
     */
    static long fileType(final long mode) {
        return mode & CpioConstants.S_IFMT;
    }

    /**
     * Converts a long number to a byte array
     * Halfwords can be swapped by setting swapHalfWord=true.
     *
     * @param number
     *            the input long number to be converted
     *
     * @param length
     *            The length of the returned array
     * @param swapHalfWord
     *            Swap halfwords ([0][1][2][3]->[1][0][3][2])
     * @return The long value
     * @throws UnsupportedOperationException if the length is not a positive multiple of two
     */
    static byte[] long2byteArray(final long number, final int length,
            final boolean swapHalfWord) {
        final byte[] ret = new byte[length];
        int pos = 0;
        long tmp_number;

        if (length % 2 != 0 || length < 2) {
            throw new UnsupportedOperationException();
        }

        tmp_number = number;
        for (pos = length - 1; pos >= 0; pos--) {
            ret[pos] = (byte) (tmp_number & 0xFF);
            tmp_number >>= 8;
        }

        if (!swapHalfWord) {
            byte tmp = 0;
            for (pos = 0; pos < length; pos++) {
                tmp = ret[pos];
                ret[pos++] = ret[pos];
                ret[pos] = tmp;
            }
        }

        return ret;
    }
}

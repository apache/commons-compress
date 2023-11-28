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
 */
package org.apache.commons.compress.utils;

import java.io.IOException;

/**
 * Utility methods for parsing data and converting it to other formats.
 *
 * @since 1.26
 */
public final class ParsingUtils {
    /**
     * Tries to parse the provided string value to an Integer, assuming a base-10 radix
     *
     * @param value string value to parse
     * @return parsed value as an int
     * @throws IOException
     */
    public static int parseIntValue(final String value) throws IOException {
        return parseIntValue(value, 10);
    }

    /**
     * Tries to parse the provided string value to an Integer
     *
     * @param value string value to parse
     * @param radix radix value to use for parsing
     * @return parsed value as an int
     * @throws IOException
     */
    public static int parseIntValue(final String value, final int radix) throws IOException {
        try {
            return Integer.parseInt(value, radix);
        } catch (final NumberFormatException exp) {
            throw new IOException("Unable to parse int from string value: " + value);
        }
    }

    /**
     * Tries to parse the provided string value to a Long, assuming a base-10 radix
     *
     * @param value string value to parse
     * @return parsed value as a long
     * @throws IOException
     */
    public static long parseLongValue(final String value) throws IOException {
        return parseLongValue(value, 10);
    }

    /**
     * Tries to parse the provided string value to a Long
     *
     * @param value string value to parse
     * @param radix radix value to use for parsing
     * @return parsed value as a long
     * @throws IOException
     */
    public static long parseLongValue(final String value, final int radix) throws IOException {
        try {
            return Long.parseLong(value, radix);
        } catch (final NumberFormatException exp) {
            throw new IOException("Unable to parse long from string value: " + value);
        }
    }

    private ParsingUtils() {
        /* no instances */ }
}

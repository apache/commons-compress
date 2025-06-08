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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for class {@link ParsingUtils}.
 *
 * @see ParsingUtils
 */
class ParsingUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {Integer.MIN_VALUE + "1", "x.x", "9e999", "1.1", "one", Integer.MAX_VALUE + "1"})
    void testParseIntValueInvalidValues(final String value) {
        assertThrows(IOException.class, () -> ParsingUtils.parseIntValue(value, 10));
    }

    @ParameterizedTest
    @ValueSource(strings = {Integer.MIN_VALUE + "", "-1", "1", "123456", Integer.MAX_VALUE + ""})
    void testParseIntValueValidValues(final String value) throws Exception {
        assertEquals(Long.parseLong(value), ParsingUtils.parseIntValue(value, 10));
    }

    @ParameterizedTest
    @ValueSource(strings = {Long.MIN_VALUE + "1", "x.x", "9e999", "1.1", "one", Long.MAX_VALUE + "1"})
    void testParseLongValueInvalidValues(final String value) {
        assertThrows(IOException.class, () -> ParsingUtils.parseLongValue(value, 10));
    }

    @ParameterizedTest
    @ValueSource(strings = {Long.MIN_VALUE + "", "-1", "1", "12345678901234", Long.MAX_VALUE + ""})
    void testParseLongValueValidValues(final String value) throws Exception {
        assertEquals(Long.parseLong(value), ParsingUtils.parseLongValue(value, 10));
    }

}

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

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link CharsetNames}.
 */
public class CharsetNamesTest {

    @Test
    void testConstants() {
        assertEquals(StandardCharsets.ISO_8859_1.name(), CharsetNames.ISO_8859_1);
        assertEquals(StandardCharsets.US_ASCII.name(), CharsetNames.US_ASCII);
        assertEquals(StandardCharsets.UTF_16.name(), CharsetNames.UTF_16);
        assertEquals(StandardCharsets.UTF_16BE.name(), CharsetNames.UTF_16BE);
        assertEquals(StandardCharsets.UTF_16LE.name(), CharsetNames.UTF_16LE);
        assertEquals(StandardCharsets.UTF_8.name(), CharsetNames.UTF_8);
    }
}

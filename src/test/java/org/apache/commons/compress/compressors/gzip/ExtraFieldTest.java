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
package org.apache.commons.compress.compressors.gzip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ExtraField}.
 */
class ExtraFieldTest {

    @Test
    void testEquals() throws IOException {
        assertEquals(new ExtraField(), new ExtraField());
        assertEquals(new ExtraField().addSubField("BB", "CCCC".getBytes(StandardCharsets.ISO_8859_1)),
                new ExtraField().addSubField("BB", "CCCC".getBytes(StandardCharsets.ISO_8859_1)));
        // not equals
        assertNotEquals(new ExtraField().addSubField("XX", "CCCC".getBytes(StandardCharsets.ISO_8859_1)),
                new ExtraField().addSubField("BB", "CCCC".getBytes(StandardCharsets.ISO_8859_1)));
        assertNotEquals(new ExtraField().addSubField("XX", "AAAA".getBytes(StandardCharsets.ISO_8859_1)),
                new ExtraField().addSubField("XX", "CCCC".getBytes(StandardCharsets.ISO_8859_1)));
    }

    @Test
    void testHashCode() throws IOException {
        assertEquals(new ExtraField().hashCode(), new ExtraField().hashCode());
        assertEquals(new ExtraField().addSubField("BB", "CCCC".getBytes(StandardCharsets.ISO_8859_1)).hashCode(),
                new ExtraField().addSubField("BB", "CCCC".getBytes(StandardCharsets.ISO_8859_1)).hashCode());
        // not equals
        assertNotEquals(new ExtraField().addSubField("XX", "CCCC".getBytes(StandardCharsets.ISO_8859_1)).hashCode(),
                new ExtraField().addSubField("BB", "CCCC".getBytes(StandardCharsets.ISO_8859_1)).hashCode());
        assertNotEquals(new ExtraField().addSubField("XX", "AAAA".getBytes(StandardCharsets.ISO_8859_1)).hashCode(),
                new ExtraField().addSubField("XX", "CCCC".getBytes(StandardCharsets.ISO_8859_1)).hashCode());
    }
}

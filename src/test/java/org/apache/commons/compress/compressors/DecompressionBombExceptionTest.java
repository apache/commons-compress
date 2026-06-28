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
package org.apache.commons.compress.compressors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;

import org.apache.commons.compress.CompressException;
import org.junit.jupiter.api.Test;

/**
 * {@link DecompressionBombException} must be an {@link IOException} so callers handle a bomb like any other stream failure.
 */
class DecompressionBombExceptionTest {

    @Test
    void isIOException() {
        assertInstanceOf(IOException.class, new DecompressionBombException("x"));
    }

    @Test
    void isCompressException() {
        assertInstanceOf(CompressException.class, new DecompressionBombException("x"));
    }

    @Test
    void formatsMessage() {
        assertEquals("limit 5 exceeded", new DecompressionBombException("limit %d exceeded", 5).getMessage());
    }
}

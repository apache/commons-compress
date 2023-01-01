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

package org.apache.commons.compress.archivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ExceptionMessageTest {

    private static final String ARCHIVER_NULL_MESSAGE = "Archivername must not be null.";

    private static final String INPUTSTREAM_NULL_MESSAGE = "InputStream must not be null.";

    private static final String OUTPUTSTREAM_NULL_MESSAGE = "OutputStream must not be null.";

    @Test
    public void testMessageWhenArchiverNameIsNull_1() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(null, System.in),
                "Should raise an IllegalArgumentException.");
        assertEquals(ARCHIVER_NULL_MESSAGE, e.getMessage());
    }

    @Test
    public void testMessageWhenArchiverNameIsNull_2() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(null, System.out),
                "Should raise an IllegalArgumentException.");
        assertEquals(ARCHIVER_NULL_MESSAGE, e.getMessage());
    }

    @Test
    public void testMessageWhenInputStreamIsNull() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", null),
                "Should raise an IllegalArgumentException.");
        assertEquals(INPUTSTREAM_NULL_MESSAGE, e.getMessage());
    }

    @Test
    public void testMessageWhenOutputStreamIsNull() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("zip", null),
                "Should raise an IllegalArgumentException.");
        assertEquals(OUTPUTSTREAM_NULL_MESSAGE, e.getMessage());
    }

}
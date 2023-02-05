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
package org.apache.commons.compress.changes;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PipedInputStream;

import org.apache.commons.compress.archivers.memory.MemoryArchiveEntry;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link Change}.
 *
 * @see Change
 */
public class ChangeTest {

    @Test
    public void testFailsToCreateChangeTakingFourArgumentsThrowsNullPointerExceptionOne() {
        final MemoryArchiveEntry memoryArchiveEntry = new MemoryArchiveEntry("x");
        assertThrows(NullPointerException.class, () -> new Change(memoryArchiveEntry, null, false));
    }

    @Test
    public void testFailsToCreateChangeTakingFourArgumentsThrowsNullPointerExceptionTwo() {
        final PipedInputStream pipedInputStream = new PipedInputStream(1);
        assertThrows(NullPointerException.class, () -> new Change(null, pipedInputStream, false));
    }

    @Test
    public void testFailsToCreateChangeTakingThreeArgumentsThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Change(null, (-407)));
    }

}

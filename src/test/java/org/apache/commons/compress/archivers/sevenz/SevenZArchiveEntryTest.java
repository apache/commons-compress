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
package org.apache.commons.compress.archivers.sevenz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link SevenZArchiveEntry}.
 */
public class SevenZArchiveEntryTest {

    @Test
    void testMethodConfigurationMattersInEquals() {
        final SevenZArchiveEntry z1 = new SevenZArchiveEntry();
        final SevenZArchiveEntry z2 = new SevenZArchiveEntry();
        final SevenZArchiveEntry z3 = new SevenZArchiveEntry();
        z1.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 1));
        z2.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 2));
        z3.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 2));
        assertNotEquals(z1, z2);
        assertNotEquals(z2, z1);
        assertEquals(z3, z2);
        assertEquals(z2, z3);
    }

    @Test
    void testMethodOrderMattersInEquals() {
        final SevenZArchiveEntry z1 = new SevenZArchiveEntry();
        final SevenZArchiveEntry z2 = new SevenZArchiveEntry();
        z1.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.LZMA2), new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER));
        z2.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER), new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        assertNotEquals(z1, z2);
        assertNotEquals(z2, z1);
    }

    @Test
    void testNoMethodsIsDifferentFromSomeMethods() {
        final SevenZArchiveEntry z1 = new SevenZArchiveEntry();
        final SevenZArchiveEntry z2 = new SevenZArchiveEntry();
        z2.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.COPY));
        assertNotEquals(z1, z2);
        assertNotEquals(z2, z1);
    }

    @Test
    void testOneMethodsIsDifferentFromTwoMethods() {
        final SevenZArchiveEntry z1 = new SevenZArchiveEntry();
        final SevenZArchiveEntry z2 = new SevenZArchiveEntry();
        z1.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.COPY));
        z2.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER), new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        assertNotEquals(z1, z2);
        assertNotEquals(z2, z1);
    }

    @Test
    void testSameMethodsYieldEqualEntries() {
        final SevenZArchiveEntry z1 = new SevenZArchiveEntry();
        final SevenZArchiveEntry z2 = new SevenZArchiveEntry();
        z1.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER), new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        z2.setContentMethods(new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER), new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        assertEquals(z1, z2);
        assertEquals(z2, z1);
    }

    @Test
    void testShouldThrowIfAccessDateIsSetToNull() {
        assertThrows(UnsupportedOperationException.class, () -> {
            final SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setAccessDate(null);
            entry.getAccessDate();
        });
    }

    @Test
    void testShouldThrowIfCreationDateIsSetToNull() {
        assertThrows(UnsupportedOperationException.class, () -> {
            final SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setCreationDate(null);
            entry.getCreationDate();
        });
    }

    @Test
    void testShouldThrowIfLastModifiedDateIsSetToNull() {
        assertThrows(UnsupportedOperationException.class, () -> {
            final SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setLastModifiedDate(null);
            entry.getLastModifiedDate();
        });
    }

    @Test
    void testShouldThrowIfNoAccessDateIsSet() {
        assertThrows(UnsupportedOperationException.class, () -> new SevenZArchiveEntry().getAccessDate());
    }

    @Test
    void testShouldThrowIfNoCreationDateIsSet() {
        assertThrows(UnsupportedOperationException.class, () -> new SevenZArchiveEntry().getCreationDate());
    }

    @Test
    void testShouldThrowIfNoLastModifiedDateIsSet() {
        assertThrows(UnsupportedOperationException.class, () -> new SevenZArchiveEntry().getLastModifiedDate());
    }

}

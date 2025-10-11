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
package org.apache.commons.compress.archivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IOIterator;
import org.apache.commons.io.function.IOStream;
import org.junit.jupiter.api.Test;

/**
 * Abstracts tests for {@link ArchiveFile} implementations.
 *
 * @param <T> The type of {@link ArchiveEntry} produced.
 */
public abstract class AbstractArchiveFileTest<T extends ArchiveEntry> extends AbstractTest {

    private static ArchiveEntry newEntry(final String name, final long size, final Instant lastModified) {
        return new ArchiveEntry() {

            @Override
            public Date getLastModifiedDate() {
                return Date.from(lastModified);
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public long getSize() {
                return size;
            }

            @Override
            public boolean isDirectory() {
                return false;
            }
        };
    }

    private static ArchiveEntry newEntryUtc(final String name, final long size, final LocalDateTime lastModified) {
        return newEntry(name, size, lastModified.toInstant(ZoneOffset.UTC));
    }

    /**
     * Gets an {@link ArchiveFile} to be tested.
     *
     * @return The archive file to be tested.
     * @throws IOException Indicates a test failure.
     */
    protected abstract ArchiveFile<T> getArchiveFile() throws IOException;

    /**
     * Gets the expected entries in the test archive.
     *
     * @return The expected entries.
     */
    private List<? extends ArchiveEntry> getExpectedEntries() {
        return Arrays.asList(
                newEntryUtc("test1.xml", 610, LocalDateTime.of(2007, 11, 14, 10, 19, 2)),
                newEntryUtc("test2.xml", 82, LocalDateTime.of(2007, 11, 14, 10, 19, 2)));
    }

    private T getMatchingEntry(final ArchiveFile<? extends T> archiveFile, final String name) throws Exception {
        try (IOStream<? extends T> stream = archiveFile.stream()) {
            return stream.filter(e -> e.getName().equals(name)).findFirst().orElse(null);
        }
    }

    /**
     * Tests that the entries returned by {@link ArchiveFile#entries()} match the expected entries.
     */
    @Test
    void testEntries() throws Exception {
        try (ArchiveFile<T> archiveFile = getArchiveFile()) {
            final List<? extends T> entries = archiveFile.entries();
            final List<? extends ArchiveEntry> expectedEntries = getExpectedEntries();
            assertEquals(expectedEntries.size(), entries.size(), "Number of entries");
            for (int i = 0; i < expectedEntries.size(); i++) {
                final ArchiveEntry expected = expectedEntries.get(i);
                final ArchiveEntry actual = entries.get(i);
                assertEquals(expected.getName(), actual.getName(), "Entry name at index " + i);
                assertEquals(expected.getSize(), actual.getSize(), "Size of entry " + expected.getName());
                assertEquals(expected.getLastModifiedDate(), actual.getLastModifiedDate(), "Last modified date of entry " + expected.getName());
            }
        }
    }

    /**
     * Tests that the input streams returned by {@link ArchiveFile#getInputStream(ArchiveEntry)} match the expected
     * entries.
     */
    @Test
    void testGetInputStream() throws Exception {
        try (ArchiveFile<T> archiveFile = getArchiveFile()) {
            final List<? extends ArchiveEntry> expectedEntries = getExpectedEntries();
            for (final ArchiveEntry expected : expectedEntries) {
                final T actual = getMatchingEntry(archiveFile, expected.getName());
                assertNotNull(actual, "Entry " + expected.getName() + " not found");
                try (InputStream inputStream = archiveFile.getInputStream(actual)) {
                    assertNotNull(inputStream, "Input stream for entry " + expected.getName());
                    final byte[] content = IOUtils.toByteArray(inputStream);
                    assertEquals(expected.getSize(), content.length, "Size of entry " + expected.getName());
                }
            }
        }
    }

    /**
     * Tests that the iterator returned by {@link ArchiveFile#iterator()} matches the expected entries.
     */
    @Test
    void testIterator() throws Exception {
        try (ArchiveFile<T> archiveFile = getArchiveFile()) {
            final IOIterator<T> iterator = archiveFile.iterator();
            final List<? extends ArchiveEntry> entries = getExpectedEntries();
            int count = 0;
            while (iterator.hasNext()) {
                final ArchiveEntry expected = entries.get(count);
                final ArchiveEntry actual = iterator.next();
                assertEquals(expected.getName(), actual.getName(), "Entry name at index " + count);
                assertEquals(expected.getSize(), actual.getSize(), "Size of entry " + expected.getName());
                assertEquals(
                        expected.getLastModifiedDate(),
                        actual.getLastModifiedDate(),
                        "Last modified date of entry " + expected.getName());
                count++;
            }
        }
    }
}

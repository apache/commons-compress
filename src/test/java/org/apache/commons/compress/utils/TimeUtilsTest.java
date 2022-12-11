/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.utils;

import static org.apache.commons.compress.utils.TimeUtils.HUNDRED_NANOS_PER_MILLISECOND;
import static org.apache.commons.compress.utils.TimeUtils.WINDOWS_EPOCH_OFFSET;
import static org.apache.commons.compress.utils.TimeUtils.ntfsTimeToDate;
import static org.apache.commons.compress.utils.TimeUtils.ntfsTimeToFileTime;
import static org.apache.commons.compress.utils.TimeUtils.toDate;
import static org.apache.commons.compress.utils.TimeUtils.toFileTime;
import static org.apache.commons.compress.utils.TimeUtils.toNtfsTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TimeUtilsTest {

    public static Stream<Arguments> dateToNtfsProvider() {
        return Stream.of(
            Arguments.of("1601-01-01T00:00:00.000Z", 0),
            Arguments.of("1601-01-01T00:00:00.000Z", 1),
            Arguments.of("1600-12-31T23:59:59.999Z", -1),
            Arguments.of("1601-01-01T00:00:00.001Z", HUNDRED_NANOS_PER_MILLISECOND),
            Arguments.of("1601-01-01T00:00:00.001Z", HUNDRED_NANOS_PER_MILLISECOND + 1),
            Arguments.of("1601-01-01T00:00:00.000Z", HUNDRED_NANOS_PER_MILLISECOND - 1),
            Arguments.of("1600-12-31T23:59:59.999Z", -HUNDRED_NANOS_PER_MILLISECOND),
            Arguments.of("1600-12-31T23:59:59.999Z", -HUNDRED_NANOS_PER_MILLISECOND + 1),
            Arguments.of("1600-12-31T23:59:59.998Z", -HUNDRED_NANOS_PER_MILLISECOND - 1),
            Arguments.of("1970-01-01T00:00:00.000Z", -WINDOWS_EPOCH_OFFSET),
            Arguments.of("1970-01-01T00:00:00.000Z", -WINDOWS_EPOCH_OFFSET + 1),
            Arguments.of("1970-01-01T00:00:00.001Z", -WINDOWS_EPOCH_OFFSET + HUNDRED_NANOS_PER_MILLISECOND),
            Arguments.of("1969-12-31T23:59:59.999Z", -WINDOWS_EPOCH_OFFSET - 1),
            Arguments.of("1969-12-31T23:59:59.999Z", -WINDOWS_EPOCH_OFFSET - HUNDRED_NANOS_PER_MILLISECOND)
        );
    }

    public static Stream<Arguments> fileTimeToNtfsProvider() {
        return Stream.of(
            Arguments.of("1601-01-01T00:00:00.0000000Z", 0),
            Arguments.of("1601-01-01T00:00:00.0000001Z", 1),
            Arguments.of("1600-12-31T23:59:59.9999999Z", -1),
            Arguments.of("1601-01-01T00:00:00.0010000Z", HUNDRED_NANOS_PER_MILLISECOND),
            Arguments.of("1601-01-01T00:00:00.0010001Z", HUNDRED_NANOS_PER_MILLISECOND + 1),
            Arguments.of("1601-01-01T00:00:00.0009999Z", HUNDRED_NANOS_PER_MILLISECOND - 1),
            Arguments.of("1600-12-31T23:59:59.9990000Z", -HUNDRED_NANOS_PER_MILLISECOND),
            Arguments.of("1600-12-31T23:59:59.9990001Z", -HUNDRED_NANOS_PER_MILLISECOND + 1),
            Arguments.of("1600-12-31T23:59:59.9989999Z", -HUNDRED_NANOS_PER_MILLISECOND - 1),
            Arguments.of("1970-01-01T00:00:00.0000000Z", -WINDOWS_EPOCH_OFFSET),
            Arguments.of("1970-01-01T00:00:00.0000001Z", -WINDOWS_EPOCH_OFFSET + 1),
            Arguments.of("1970-01-01T00:00:00.0010000Z", -WINDOWS_EPOCH_OFFSET + HUNDRED_NANOS_PER_MILLISECOND),
            Arguments.of("1969-12-31T23:59:59.9999999Z", -WINDOWS_EPOCH_OFFSET - 1),
            Arguments.of("1969-12-31T23:59:59.9990000Z", -WINDOWS_EPOCH_OFFSET - HUNDRED_NANOS_PER_MILLISECOND)
        );
    }

    @ParameterizedTest
    @MethodSource("dateToNtfsProvider")
    public void shouldConvertNtfsTimeToDate(final String instant, final long ntfsTime) {
        final Date converted = ntfsTimeToDate(ntfsTime);
        assertEquals(Instant.parse(instant), converted.toInstant());
        // ensuring the deprecated method still works
        assertEquals(converted, SevenZArchiveEntry.ntfsTimeToJavaTime(ntfsTime));
    }

    @ParameterizedTest
    @MethodSource("dateToNtfsProvider")
    public void shouldConvertDateToNtfsTime(final String instant, final long ntfsTime) {
        final long ntfsMillis = Math.floorDiv(ntfsTime, HUNDRED_NANOS_PER_MILLISECOND) * HUNDRED_NANOS_PER_MILLISECOND;
        final Date parsed = Date.from(Instant.parse(instant));
        final long converted = toNtfsTime(parsed);
        assertEquals(ntfsMillis, converted);
        // ensuring the deprecated method still works
        assertEquals(converted, SevenZArchiveEntry.javaTimeToNtfsTime(parsed));
    }

    @ParameterizedTest
    @MethodSource("fileTimeToNtfsProvider")
    public void shouldConvertFileTimeToNtfsTime(final String instant, final long ntfsTime) {
        final FileTime parsed = FileTime.from(Instant.parse(instant));
        assertEquals(ntfsTime, toNtfsTime(parsed));
    }

    @ParameterizedTest
    @MethodSource("fileTimeToNtfsProvider")
    public void shouldConvertNtfsTimeToFileTime(final String instant, final long ntfsTime) {
        final FileTime parsed = FileTime.from(Instant.parse(instant));
        assertEquals(parsed, ntfsTimeToFileTime(ntfsTime));
    }

    @Test
    public void shouldConvertNullDateToNullFileTime() {
        assertNull(toFileTime(null));
    }

    @Test
    public void shouldConvertNullFileTimeToNullDate() {
        assertNull(toDate(null));
    }

    @ParameterizedTest
    @MethodSource("dateToNtfsProvider")
    public void shouldConvertDateToFileTime(final String instant, final long ignored) {
        final Instant parsedInstant = Instant.parse(instant);
        final FileTime parsedFileTime = FileTime.from(parsedInstant);
        final Date parsedDate = Date.from(parsedInstant);
        assertEquals(parsedFileTime, toFileTime(parsedDate));
    }

    @ParameterizedTest
    @MethodSource("fileTimeToNtfsProvider")
    public void shouldConvertFileTimeToDate(final String instant, final long ignored) {
        final Instant parsedInstant = Instant.parse(instant);
        final FileTime parsedFileTime = FileTime.from(parsedInstant);
        final Date parsedDate = Date.from(parsedInstant);
        assertEquals(parsedDate, toDate(parsedFileTime));
    }

    public static Stream<Arguments> truncateFileTimeProvider() {
        return Stream.of(
                Arguments.of(
                        "2022-05-10T18:25:33.123456789Z",
                        "2022-05-10T18:25:33.1234567Z"
                ),
                Arguments.of(
                        "1970-01-01T00:00:00.000000001Z",
                        "1970-01-01T00:00:00.0000000Z"
                ),
                Arguments.of(
                        "1970-01-01T00:00:00.000000010Z",
                        "1970-01-01T00:00:00.0000000Z"
                ),
                Arguments.of(
                        "1970-01-01T00:00:00.000000199Z",
                        "1970-01-01T00:00:00.0000001Z"
                ),
                Arguments.of(
                        "1969-12-31T23:59:59.999999999Z",
                        "1969-12-31T23:59:59.9999999Z"
                ),
                Arguments.of(
                        "1969-12-31T23:59:59.000000001Z",
                        "1969-12-31T23:59:59.0000000Z"
                ),
                Arguments.of(
                        "1969-12-31T23:59:59.000000010Z",
                        "1969-12-31T23:59:59.0000000Z"
                ),
                Arguments.of(
                        "1969-12-31T23:59:59.000000199Z",
                        "1969-12-31T23:59:59.0000001Z"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("truncateFileTimeProvider")
    public void shouldTruncateFileTimeToHundredNanos(final String original, final String truncated) {
        final FileTime originalTime = FileTime.from(Instant.parse(original));
        final FileTime truncatedTime = FileTime.from(Instant.parse(truncated));
        assertEquals(truncatedTime, TimeUtils.truncateToHundredNanos(originalTime));
    }
}

package org.apache.commons.compress.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import static org.apache.commons.compress.utils.TimeUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertEquals(Instant.parse(instant), ntfsTimeToDate(ntfsTime).toInstant());
    }

    @ParameterizedTest
    @MethodSource("dateToNtfsProvider")
    public void shouldConvertDateToNtfsTime(final String instant, final long ntfsTime) {
        final long ntfsMillis = Math.floorDiv(ntfsTime, HUNDRED_NANOS_PER_MILLISECOND) * HUNDRED_NANOS_PER_MILLISECOND;
        final Date parsed = Date.from(Instant.parse(instant));
        assertEquals(ntfsMillis, dateToNtfsTime(parsed));
    }

    @ParameterizedTest
    @MethodSource("fileTimeToNtfsProvider")
    public void shouldConvertFileTimeToNtfsTime(final String instant, final long ntfsTime) {
        final FileTime parsed = FileTime.from(Instant.parse(instant));
        assertEquals(ntfsTime, fileTimeToNtfsTime(parsed));
    }

    @ParameterizedTest
    @MethodSource("fileTimeToNtfsProvider")
    public void shouldConvertNtfsTimeToFileTime(final String instant, final long ntfsTime) {
        final FileTime parsed = FileTime.from(Instant.parse(instant));
        assertEquals(parsed, ntfsTimeToFileTime(ntfsTime));
    }

    @Test
    public void shouldConvertNullDateToNullFileTime() {
        assertNull(dateToFileTime(null));
    }

    @Test
    public void shouldConvertNullFileTimeToNullDate() {
        assertNull(fileTimeToDate(null));
    }

    @ParameterizedTest
    @MethodSource("dateToNtfsProvider")
    public void shouldConvertDateToFileTime(final String instant, final long ignored) {
        final Instant parsedInstant = Instant.parse(instant);
        final FileTime parsedFileTime = FileTime.from(parsedInstant);
        final Date parsedDate = Date.from(parsedInstant);
        assertEquals(parsedFileTime, dateToFileTime(parsedDate));
    }

    @ParameterizedTest
    @MethodSource("fileTimeToNtfsProvider")
    public void shouldConvertFileTimeToDate(final String instant, final long ignored) {
        final Instant parsedInstant = Instant.parse(instant);
        final FileTime parsedFileTime = FileTime.from(parsedInstant);
        final Date parsedDate = Date.from(parsedInstant);
        assertEquals(parsedDate, fileTimeToDate(parsedFileTime));
    }
}
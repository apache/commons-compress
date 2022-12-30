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

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling time-related types and conversions.
 *
 * @since 1.23
 */
public final class TimeUtils {

    /** The amount of 100-nanosecond intervals in one millisecond. */
    static final long HUNDRED_NANOS_PER_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1) / 100;

    /** The amount of 100-nanosecond intervals in one second. */
    static final long HUNDRED_NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1) / 100;

    /**
     * <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/ms724290%28v=vs.85%29.aspx">Windows File Times</a>
     * <p>
     * A file time is a 64-bit value that represents the number of
     * 100-nanosecond intervals that have elapsed since 12:00
     * A.M. January 1, 1601 Coordinated Universal Time (UTC).
     * This is the offset of Windows time 0 to Unix epoch in 100-nanosecond intervals.
     * </p>
     */
    static final long WINDOWS_EPOCH_OFFSET = -116444736000000000L;

    /**
     * Converts standard UNIX time (in seconds, UTC/GMT) to {@link FileTime}.
     *
     * @param time UNIX timestamp
     * @return the corresponding FileTime
     */
    public static FileTime unixTimeToFileTime(final long time) {
        return FileTime.from(time, TimeUnit.SECONDS);
    }

    /**
     * Converts {@link FileTime} to standard UNIX time.
     *
     * @param time the original FileTime
     * @return the UNIX timestamp
     */
    public static long toUnixTime(final FileTime time) {
        return time.to(TimeUnit.SECONDS);
    }

    /**
     * Converts Java time (milliseconds since Epoch) to standard UNIX time.
     *
     * @param time the original Java time
     * @return the UNIX timestamp
     */
    public static long javaTimeToUnixTime(final long time) {
        return time / 1000L;
    }

    /**
     * Tests whether a FileTime can be safely represented in the standard UNIX time.
     *
     * <p>If the FileTime is null, this method always returns true.</p>
     *
     * @param time the FileTime to evaluate, can be null
     * @return true if the time exceeds the minimum or maximum UNIX time, false otherwise
     */
    public static boolean isUnixTime(final FileTime time) {
        if (time == null) {
            return true;
        }
        final long fileTimeToUnixTime = toUnixTime(time);
        return isUnixTime(fileTimeToUnixTime);
    }

    /**
     * Tests whether a given number of seconds (since Epoch) can be safely represented in the standard UNIX time.
     *
     * @param seconds the number of seconds (since Epoch) to evaluate
     * @return true if the time can be represented in the standard UNIX time, false otherwise
     */
    public static boolean isUnixTime(final long seconds) {
        return Integer.MIN_VALUE <= seconds && seconds <= Integer.MAX_VALUE;
    }

    /**
     * Converts NTFS time (100 nanosecond units since 1 January 1601) to Java time.
     *
     * @param ntfsTime the NTFS time in 100 nanosecond units
     * @return the Date
     */
    public static Date ntfsTimeToDate(final long ntfsTime) {
        final long javaHundredNanos = Math.addExact(ntfsTime, WINDOWS_EPOCH_OFFSET);
        final long javaMillis = Math.floorDiv(javaHundredNanos, HUNDRED_NANOS_PER_MILLISECOND);
        return new Date(javaMillis);
    }

    /**
     * Converts NTFS time (100-nanosecond units since 1 January 1601) to a FileTime.
     *
     * @param ntfsTime the NTFS time in 100-nanosecond units
     * @return the FileTime
     *
     * @see TimeUtils#WINDOWS_EPOCH_OFFSET
     * @see TimeUtils#toNtfsTime(FileTime)
     */
    public static FileTime ntfsTimeToFileTime(final long ntfsTime) {
        final long javaHundredsNanos = Math.addExact(ntfsTime, WINDOWS_EPOCH_OFFSET);
        final long javaSeconds = Math.floorDiv(javaHundredsNanos, HUNDRED_NANOS_PER_SECOND);
        final long javaNanos = Math.floorMod(javaHundredsNanos, HUNDRED_NANOS_PER_SECOND) * 100;
        return FileTime.from(Instant.ofEpochSecond(javaSeconds, javaNanos));
    }

    /**
     * Converts {@link FileTime} to a {@link Date}.
     * If the provided FileTime is {@code null}, the returned Date is also {@code null}.
     *
     * @param time the file time to be converted.
     * @return a {@link Date} which corresponds to the supplied time, or {@code null} if the time is {@code null}.
     * @see TimeUtils#toFileTime(Date)
     */
    public static Date toDate(final FileTime time) {
        return time != null ? new Date(time.toMillis()) : null;
    }

    /**
     * Converts {@link Date} to a {@link FileTime}.
     * If the provided Date is {@code null}, the returned FileTime is also {@code null}.
     *
     * @param date the date to be converted.
     * @return a {@link FileTime} which corresponds to the supplied date, or {@code null} if the date is {@code null}.
     * @see TimeUtils#toDate(FileTime)
     */
    public static FileTime toFileTime(final Date date) {
        return date != null ? FileTime.fromMillis(date.getTime()) : null;
    }

    /**
     * Converts a {@link Date} to NTFS time.
     *
     * @param date the Date
     * @return the NTFS time
     */
    public static long toNtfsTime(final Date date) {
        return toNtfsTime(date.getTime());
    }

    /**
     * Converts Java time (milliseconds since Epoch) to NTFS time.
     *
     * @param time the Java time
     * @return the NTFS time
     */
    public static long toNtfsTime(final long time) {
        final long javaHundredNanos = time * HUNDRED_NANOS_PER_MILLISECOND;
        return Math.subtractExact(javaHundredNanos, WINDOWS_EPOCH_OFFSET);
    }

    /**
     * Converts a {@link FileTime} to NTFS time (100-nanosecond units since 1 January 1601).
     *
     * @param time the FileTime
     * @return the NTFS time in 100-nanosecond units
     *
     * @see TimeUtils#WINDOWS_EPOCH_OFFSET
     * @see TimeUtils#ntfsTimeToFileTime(long)
     */
    public static long toNtfsTime(final FileTime time) {
        final Instant instant = time.toInstant();
        final long javaHundredNanos = (instant.getEpochSecond() * HUNDRED_NANOS_PER_SECOND) + (instant.getNano() / 100);
        return Math.subtractExact(javaHundredNanos, WINDOWS_EPOCH_OFFSET);
    }

    /**
     * Truncates a FileTime to 100-nanosecond precision.
     *
     * @param fileTime the FileTime to be truncated
     * @return the truncated FileTime
     */
    public static FileTime truncateToHundredNanos(final FileTime fileTime) {
        final Instant instant = fileTime.toInstant();
        return FileTime.from(Instant.ofEpochSecond(instant.getEpochSecond(), (instant.getNano() / 100) * 100));
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private TimeUtils(){
    }
}

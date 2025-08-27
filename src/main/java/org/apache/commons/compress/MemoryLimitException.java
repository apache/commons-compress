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

package org.apache.commons.compress;

/**
 * If a stream checks for estimated memory allocation, and the estimate goes above the memory limit, this is thrown. This can also be thrown if a stream tries
 * to allocate a byte array that is larger than the allowable limit.
 *
 * @since 1.14
 */
public class MemoryLimitException extends CompressException {

    private static final String LABEL = "total memory";
    private static final String MAXIMUM = "maxium";
    private static final String BYTES = "bytes";
    private static final String KIB = "KiB";
    private static final long serialVersionUID = 1L;

    private static long availMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    private static String buildMessage(final long memoryNeeded, final long memoryLimit, final String scale, final String boundary) {
        return String.format("%,d %s of memory requested; %s is %,d %s. If the file is not corrupt, consider increasing the memory limit.", memoryNeeded, scale,
                boundary, memoryLimit, scale);
    }

    private static long check(final long request, final long softMax, final long hardMax, final String memoryType, final String scale)
            throws MemoryLimitException {
        check(request, softMax, scale, MAXIMUM);
        check(request, hardMax, scale, memoryType);
        return request;
    }

    private static void check(final long request, final long max, final String scale, final String boundary) throws MemoryLimitException {
        if (request > max) {
            throw new MemoryLimitException(request, max, scale, boundary);
        }
    }

    /**
     * Throws a MemoryLimitException if the request is greater than the max.
     *
     * @param request The request in bytes.
     * @param max     The max in bytes.
     * @return The request in bytes.
     * @throws MemoryLimitException Thrown if the request is greater than the max.
     * @since 1.29.0
     */
    public static int checkBytes(final int request, final long max) throws MemoryLimitException {
        check(request, max, availMemory(), LABEL, BYTES);
        return request;
    }

    /**
     * Throws a MemoryLimitException if the request is greater than the max.
     *
     * @param request The request in bytes.
     * @param max     The max in bytes.
     * @return The request in bytes.
     * @throws MemoryLimitException Thrown if the request is greater than the max.
     * @since 1.29.0
     */
    public static long checkBytes(final long request, final long max) throws MemoryLimitException {
        check(request, max, availMemory(), LABEL, BYTES);
        return request;
    }

    /**
     * Throws a MemoryLimitException if the request is greater than the max.
     *
     * @param request The request in KiB.
     * @param max     The max in KiB.
     * @return The request in KiB.
     * @throws MemoryLimitException Thrown if the request is greater than the max.
     * @since 1.29.0
     */
    public static long checkKiB(final long request, final long max) throws MemoryLimitException {
        return check(request, max, availMemory() / 1024, LABEL, KIB);
    }

    /** A long instead of int to account for overflow in corrupt files. */
    private final long memoryRequestKiB;
    /** A long instead of int to account for overflow in corrupt files. */
    private final long memoryLimitKiB;

    /**
     * Constructs a new instance.
     *
     * @param memoryRequestKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB   The memory limit in kibibytes (KiB).
     */
    public MemoryLimitException(final long memoryRequestKiB, final int memoryLimitKiB) {
        super(buildMessage(memoryRequestKiB, memoryLimitKiB, KIB, MAXIMUM));
        this.memoryRequestKiB = memoryRequestKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Constructs a new instance.
     *
     * @param memoryRequestKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB   The memory limit in kibibytes (KiB).
     * @param cause            The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that
     *                         the cause is nonexistent or unknown.)
     * @deprecated Use {@link #MemoryLimitException(long, int, Throwable)}.
     */
    @Deprecated
    public MemoryLimitException(final long memoryRequestKiB, final int memoryLimitKiB, final Exception cause) {
        super(buildMessage(memoryRequestKiB, memoryLimitKiB, KIB, MAXIMUM), cause);
        this.memoryRequestKiB = memoryRequestKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Constructs a new instance.
     *
     * @param memoryRequestKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB   The memory limit in kibibytes (KiB).
     * @param cause            The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that
     *                         the cause is nonexistent or unknown.)
     */
    public MemoryLimitException(final long memoryRequestKiB, final int memoryLimitKiB, final Throwable cause) {
        super(buildMessage(memoryRequestKiB, memoryLimitKiB, KIB, MAXIMUM), cause);
        this.memoryRequestKiB = memoryRequestKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Constructs a new instance.
     *
     * @param memoryRequestKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB  The memory limit in kibibytes (KiB).
     * @since 1.29.0
     */
    public MemoryLimitException(final long memoryRequestKiB, final long memoryLimitKiB) {
        super(buildMessage(memoryRequestKiB, memoryLimitKiB, KIB, MAXIMUM));
        this.memoryRequestKiB = memoryRequestKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    private MemoryLimitException(final long memoryRequestKiB, final long memoryLimitKiB, final String scale, final String boundary) {
        super(buildMessage(memoryRequestKiB, memoryLimitKiB, scale, boundary));
        this.memoryRequestKiB = memoryRequestKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Gets the memory limit in kibibytes (KiB).
     *
     * @return the memory limit in kibibytes (KiB).
     */
    public int getMemoryLimitInKb() {
        return Math.toIntExact(memoryLimitKiB);
    }

    /**
     * Gets the memory limit in kibibytes (KiB).
     *
     * @return the memory limit in kibibytes (KiB).
     */
    public long getMemoryLimitInKiBLong() {
        return memoryLimitKiB;
    }

    /**
     * Gets the memory needed in kibibytes (KiB).
     *
     * @return the memory needed in kibibytes (KiB).
     */
    public long getMemoryNeededInKb() {
        return memoryRequestKiB;
    }
}

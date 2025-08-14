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

    private static final String MAXIMUM = "maximum";
    private static final String BYTES = "bytes";
    private static final String KIB = "KiB";
    private static final long serialVersionUID = 1L;

    private static String buildMessage(final long memoryNeeded, final long memoryLimit, final String scale, final String boundary) {
        return String.format("%,d %s of memory requested; %s set is %,d %s. If the file is not corrupt, consider increasing the memory limit.", memoryNeeded,
                scale, boundary, memoryLimit, scale);
    }

    private static long check(final long request, final long max, final long memory, final String memoryType, final String scale) throws MemoryLimitException {
        check(request, max, scale, MAXIMUM);
        check(request, memory, scale, memoryType);
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
     * @param request The request.
     * @param max     The max.
     * @return The request.
     * @throws MemoryLimitException Thrown if the request is greater than the max.
     * @since 1.29.0
     */
    public static int checkBytes(final int request, final long max) throws MemoryLimitException {
        check(request, max, Runtime.getRuntime().maxMemory(), "max memory", BYTES);
        return request;
    }

    /**
     * Throws a MemoryLimitException if the request is greater than the max.
     *
     * @param request The request.
     * @param max     The max.
     * @return The request.
     * @throws MemoryLimitException Thrown if the request is greater than the max.
     * @since 1.29.0
     */
    public static long checkBytes(final long request, final long max) throws MemoryLimitException {
        check(request, max, Runtime.getRuntime().maxMemory(), "max memory", BYTES);
        return request;
    }

    /**
     * Throws a MemoryLimitException if the request is greater than the max.
     *
     * @param request The request.
     * @param max     The max.
     * @return The request.
     * @throws MemoryLimitException Thrown if the request is greater than the max.
     * @since 1.29.0
     */
    public static long checkKiB(final long request, final long max) throws MemoryLimitException {
        return check(request, max, Runtime.getRuntime().maxMemory() / 1024, "max memory", KIB);
    }

    /** A long instead of int to account for overflow in corrupt files. */
    private final long memoryNeededKiB;
    /** A long instead of int to account for overflow in corrupt files. */
    private final long memoryLimitKiB;

    /**
     * Constructs a new instance.
     *
     * @param memoryNeededKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB  The memory limit in kibibytes (KiB).
     */
    public MemoryLimitException(final long memoryNeededKiB, final int memoryLimitKiB) {
        super(buildMessage(memoryNeededKiB, memoryLimitKiB, KIB, MAXIMUM));
        this.memoryNeededKiB = memoryNeededKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Constructs a new instance.
     *
     * @param memoryNeededKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB  The memory limit in kibibytes (KiB).
     * @param cause           The cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                        A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     * @deprecated Use {@link #MemoryLimitException(long, int, Throwable)}.
     */
    @Deprecated
    public MemoryLimitException(final long memoryNeededKiB, final int memoryLimitKiB, final Exception cause) {
        super(buildMessage(memoryNeededKiB, memoryLimitKiB, KIB, MAXIMUM), cause);
        this.memoryNeededKiB = memoryNeededKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Constructs a new instance.
     *
     * @param memoryNeededKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB  The memory limit in kibibytes (KiB).
     * @param cause           The cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                        A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public MemoryLimitException(final long memoryNeededKiB, final int memoryLimitKiB, final Throwable cause) {
        super(buildMessage(memoryNeededKiB, memoryLimitKiB, KIB, MAXIMUM), cause);
        this.memoryNeededKiB = memoryNeededKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Constructs a new instance.
     *
     * @param memoryNeededKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB  The memory limit in kibibytes (KiB).
     * @since 1.29.0
     */
    public MemoryLimitException(final long memoryNeededKiB, final long memoryLimitKiB) {
        super(buildMessage(memoryNeededKiB, memoryLimitKiB, KIB, MAXIMUM));
        this.memoryNeededKiB = memoryNeededKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    private MemoryLimitException(final long memoryNeededKiB, final long memoryLimitKiB, final String scale, final String boundary) {
        super(buildMessage(memoryNeededKiB, memoryLimitKiB, scale, boundary));
        this.memoryNeededKiB = memoryNeededKiB;
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
        return memoryNeededKiB;
    }
}

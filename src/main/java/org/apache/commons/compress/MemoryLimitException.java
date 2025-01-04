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

import java.io.IOException;

/**
 * If a stream checks for estimated memory allocation, and the estimate goes above the memory limit, this is thrown. This can also be thrown if a stream tries
 * to allocate a byte array that is larger than the allowable limit.
 *
 * @since 1.14
 */
public class MemoryLimitException extends IOException {

    private static final long serialVersionUID = 1L;

    private static String buildMessage(final long memoryNeededInKb, final int memoryLimitInKb) {
        return String.format("%,d KiB of memory would be needed; limit was %,d KiB. If the file is not corrupt, consider increasing the memory limit.",
                memoryNeededInKb, memoryLimitInKb);
    }

    /** A long instead of int to account for overflow for corrupt files. */
    private final long memoryNeededKiB;
    private final int memoryLimitKiB;

    /**
     * Constructs a new instance.
     *
     * @param memoryNeededKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB  The memory limit in kibibytes (KiB).
     */
    public MemoryLimitException(final long memoryNeededKiB, final int memoryLimitKiB) {
        super(buildMessage(memoryNeededKiB, memoryLimitKiB));
        this.memoryNeededKiB = memoryNeededKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Constructs a new instance.
     *
     * @param memoryNeededKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB  The memory limit in kibibytes (KiB).
     * @param cause            The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that
     *                         the cause is nonexistent or unknown.)
     * @deprecated Use {@link #MemoryLimitException(long, int, Throwable)}.
     */
    @Deprecated
    public MemoryLimitException(final long memoryNeededKiB, final int memoryLimitKiB, final Exception cause) {
        super(buildMessage(memoryNeededKiB, memoryLimitKiB), cause);
        this.memoryNeededKiB = memoryNeededKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Constructs a new instance.
     *
     * @param memoryNeededKiB The memory needed in kibibytes (KiB).
     * @param memoryLimitKiB  The memory limit in kibibytes (KiB).
     * @param cause            The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that
     *                         the cause is nonexistent or unknown.)
     */
    public MemoryLimitException(final long memoryNeededKiB, final int memoryLimitKiB, final Throwable cause) {
        super(buildMessage(memoryNeededKiB, memoryLimitKiB), cause);
        this.memoryNeededKiB = memoryNeededKiB;
        this.memoryLimitKiB = memoryLimitKiB;
    }

    /**
     * Gets the memory limit in kibibytes (KiB).
     *
     * @return the memory limit in kibibytes (KiB).
     */
    public int getMemoryLimitInKb() {
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

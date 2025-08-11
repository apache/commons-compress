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

package org.apache.commons.compress.archivers.dump;

import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Signals that an dump archive exception of some sort has occurred.
 *
 * @since 1.3
 */
public class DumpArchiveException extends ArchiveException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@code DumpArchiveException} with {@code null} as its error detail message.
     */
    public DumpArchiveException() {
    }

    /**
     * Constructs a {@code DumpArchiveException} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public DumpArchiveException(final String message) {
        super(message);
    }

    /**
     * Constructs a {@code DumpArchiveException} with the specified detail message and cause.
     *
     * <p>
     * Note that the detail message associated with {@code cause} is <em>not</em> automatically incorporated into this exception's detail message.
     * </p>
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     *
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that the cause
     *                is nonexistent or unknown.)
     */
    public DumpArchiveException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code DumpArchiveException} with the specified cause and a detail message of {@code (cause==null ? null : cause.toString())} (which
     * typically contains the class and detail message of {@code cause}). This constructor is useful for IO exceptions that are little more than wrappers for
     * other throwables.
     *
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that the cause
     *              is nonexistent or unknown.)
     */
    public DumpArchiveException(final Throwable cause) {
        super(cause);
    }
}

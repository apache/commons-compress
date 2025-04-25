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

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Signals that an Archive exception of some sort has occurred.
 */
public class ArchiveException extends IOException {

    /** Serial. */
    private static final long serialVersionUID = 2772690708123267100L;

    /**
     * Checks that the specified object reference is not {@code null} and throws a customized {@link ArchiveException} if it is. *
     *
     * @param obj             the object reference to check for nullity.
     * @param messageSupplier supplier of the detail message to be used in the event that a {@code ArchiveException} is thrown
     * @param <T>             the type of the reference.
     * @return {@code obj} if not {@code null}
     * @throws ArchiveException if {@code obj} is {@code null}
     * @since 1.28.0
     */
    public static <T> T requireNonNull(final T obj, final Supplier<String> messageSupplier) throws ArchiveException {
        if (obj == null) {
            throw new ArchiveException(messageSupplier == null ? null : messageSupplier.get());
        }
        return obj;
    }

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized.
     *
     * @param message The message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public ArchiveException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method). A null value indicates that the cause is nonexistent or
     *                unknown.
     */
    public ArchiveException(final String message, final Exception cause) {
        super(message, cause);
    }
}

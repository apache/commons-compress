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

import java.util.function.Supplier;

import org.apache.commons.compress.CompressException;

/**
 * Signals that an Archive exception of some sort has occurred.
 */
public class ArchiveException extends CompressException {

    /** Serial. */
    private static final long serialVersionUID = 2772690708123267100L;

    /**
     * Delegates to {@link Math#addExact(int, int)} wrapping its {@link ArithmeticException} in our {@link ArchiveException}.
     *
     * @param x the first value.
     * @param y the second value.
     * @return the result.
     * @throws ArchiveException if the result overflows an {@code int}.
     * @see Math#addExact(int, int)
     * @since 1.29.0
     */
    public static int addExact(final int x, final int y) throws ArchiveException {
        try {
            return Math.addExact(x, y);
        } catch (final ArithmeticException e) {
            throw new ArchiveException(e);
        }
    }

    /**
     * Delegates to {@link Math#addExact(int, int)} wrapping its {@link ArithmeticException} in our {@link ArchiveException}.
     *
     * @param x the first value.
     * @param y the second value.
     * @return the result.
     * @throws ArchiveException if the result overflows an {@code int}.
     * @see Math#addExact(int, int)
     * @since 1.29.0
     */
    public static int addExact(final int x, final long y) throws ArchiveException {
            return addExact(x, Math.toIntExact(y));
    }

    /**
     * Delegates to {@link Math#addExact(long, long)} wrapping its {@link ArithmeticException} in our {@link ArchiveException}.
     *
     * @param x the first value.
     * @param y the second value.
     * @return the result.
     * @throws ArchiveException if the result overflows a {@code long}.
     * @see Math#addExact(long, long)
     * @since 1.29.0
     */
    public static long addExact(final long x, final long y) throws ArchiveException {
        try {
            return Math.addExact(x, y);
        } catch (final ArithmeticException e) {
            throw new ArchiveException(e);
        }
    }

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
        return CompressException.requireNonNull(ArchiveException.class, obj, messageSupplier);
    }

    /**
     * Constructs an {@code ArchiveException} with {@code null} as its error detail message.
     *
     * @since 1.28.0
     */
    public ArchiveException() {
        // empty
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
     * @deprecated Use {@link #ArchiveException(String, Throwable)}.
     */
    @Deprecated
    public ArchiveException(final String message, final Exception cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified detail message format and arguments. The cause is not initialized.
     * <p>
     * The arguments are used with {@link String#format(String, Object...)}.
     * </p>
     *
     * @param message The message format (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param args    the format arguments to use.
     * @since 1.29.0
     * @see String#format(String, Object...)
     */
    public ArchiveException(final String message, final Object... args) {
        super(message, args);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method). A null value indicates that the cause is nonexistent or
     *                unknown.
     * @since 1.28.0
     */
    public ArchiveException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code ArchiveException} with the specified cause and a detail message.
     *
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that the cause
     *              is nonexistent or unknown.)
     * @since 1.28.0
     */
    public ArchiveException(final Throwable cause) {
        super(cause);
    }
}

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
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.lang3.function.Suppliers;

/**
 * Signals that a Pack200 Compress exception of some sort has occurred.
 *
 * @since 1.28.0
 */
public class CompressException extends IOException {

    /** Serial. */
    private static final long serialVersionUID = 1;

    /**
     * Delegates to {@link Math#addExact(int, int)} wrapping its {@link ArithmeticException} in our {@link ArchiveException}.
     *
     * @param <E>             The type of the exception.
     * @param x the first value.
     * @param y the second value.
     * @param eFunction How to create an exception.
     * @return the result.
     * @throws E if the result or input overflows an {@code int}.
     * @see Math#addExact(int, int)
     * @since 1.29.0
     */
    public static <E extends CompressException> int addExact(final int x, final long y, final Function<Throwable, E> eFunction) throws E {
        try {
            return Math.addExact(x, Math.toIntExact(y));
        } catch (final ArithmeticException e) {
            throw eFunction.apply(e);
        }
    }

    /**
     * Checks that the specified object reference is not {@code null} and throws a customized {@link CompressException} if it is. *
     *
     * @param <T>             The type of the reference.
     * @param <E>             The type of the exception.
     * @param cls             The exception class.
     * @param obj             The object reference to check for nullity.
     * @param messageSupplier supplier of the detail message to be used in the event that a {@code ArchiveException} is thrown
     * @return {@code obj} if not {@code null}.
     * @throws E if {@code obj} is {@code null}.
     */
    protected static <T, E extends Throwable> T requireNonNull(final Class<? super E> cls, final T obj, final Supplier<String> messageSupplier) throws E {
        if (obj == null) {
            try {
                cls.getConstructor(String.class).newInstance(Suppliers.get(messageSupplier));
            } catch (ReflectiveOperationException | SecurityException e) {
                new CompressException(Suppliers.get(messageSupplier), e);
            }
        }
        return obj;
    }

    /**
     * Constructs an {@code CompressException} with {@code null} as its error detail message.
     */
    public CompressException() {
        // empty
    }

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized.
     *
     * @param message The message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public CompressException(final String message) {
        super(message);
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
    public CompressException(final String message, final Object... args) {
        super(String.format(message, args));
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                A {@code null} value indicates that the cause is nonexistent or unknown.
     */
    public CompressException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code CompressException} with the specified cause and a detail message.
     *
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public CompressException(final Throwable cause) {
        super(cause);
    }
}

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

package org.apache.commons.compress.harmony.pack200;

import java.util.function.Function;

import org.apache.commons.compress.CompressException;
import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Signals a problem with a Pack200 coding or decoding issue.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class Pack200Exception extends CompressException {

    private static final Function<Throwable, Pack200Exception> E_FUNCTION = Pack200Exception::new;

    private static final long serialVersionUID = 5168177401552611803L;

    /**
     * Delegates to {@link Math#addExact(int, int)} wrapping its {@link ArithmeticException} in our {@link ArchiveException}.
     *
     * @param x the first value.
     * @param y the second value.
     * @return the result.
     * @throws Pack200Exception if the result or input overflows an {@code int}.
     * @see Math#addExact(int, int)
     * @since 1.29.0
     */
    public static int addExact(final int x, final long y) throws Pack200Exception {
        return addExact(x, y, E_FUNCTION);
    }

    /**
     * Throws a MemoryLimitException if the request couldn't allocate an {@code int} array of the given size.
     *
     * @param size The requested array size.
     * @return The request.
     * @throws Pack200Exception Thrown if the request is greater than the max.
     * @since 1.29.0
     */
    public static int checkIntArray(final int size) throws Pack200Exception {
        try {
            MemoryLimitException.checkBytes((long) Integer.BYTES * size, Runtime.getRuntime().maxMemory());
        } catch (final MemoryLimitException e) {
            throw new Pack200Exception(e);
        }
        return size;
    }

    /**
     * Throws a MemoryLimitException if the request couldn't allocate an {@code int} array of the given size.
     *
     * @param size The requested array size.
     * @param count How many arrays to request.
     * @throws Pack200Exception Thrown if the request is greater than the max.
     * @since 1.29.0
     */
    public static void checkIntArray(final int size, final int count) throws Pack200Exception {
        try {
            checkIntArray(Math.multiplyExact(size, count));
        } catch (final ArithmeticException e) {
            throw new Pack200Exception("No room to allocate %,d int arrays of length %,d", size, count);
        }
    }

    /**
     * Throws a Pack200Exception if the given object is null.
     *
     * @param <T>     The object type.
     * @param obj     The object to test.
     * @param message The message format (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param args    the format arguments to use.
     * @return The given object.
     * @throws Pack200Exception Thrown if {@code obj} is null.
     * @since 1.29.0
     */
    public static <T> T requireNonNull(final T obj, final String message, final Object... args) throws Pack200Exception {
        if (obj == null) {
            throw new Pack200Exception(message, args);
        }
        return obj;
    }

    /**
     * Constructs an {@code Pack200Exception} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public Pack200Exception(final String message) {
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
    public Pack200Exception(final String message, final Object... args) {
        super(message, args);
    }

    /**
     * Constructs an {@code Pack200Exception} with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated into this exception's detail message.
     * </p>
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     *
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that the cause
     *                is nonexistent or unknown.)
     *
     * @since 1.28.0
     */
    public Pack200Exception(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code Pack200Exception} with the specified cause and a detail message.
     *
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that the cause
     *              is nonexistent or unknown.)
     * @since 1.29.0
     */
    public Pack200Exception(final Throwable cause) {
        super(cause);
    }

}

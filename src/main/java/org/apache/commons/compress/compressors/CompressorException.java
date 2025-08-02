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
package org.apache.commons.compress.compressors;

import org.apache.commons.compress.CompressException;

/**
 * Signals that an Compressor exception of some sort has occurred.
 */
public class CompressorException extends CompressException {

    /** Serial. */
    private static final long serialVersionUID = -2932901310255908814L;

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized.
     *
     * @param message The message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public CompressorException(final String message) {
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
    public CompressorException(final String message, final Object... args) {
        super(message, args);
    }


    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method). A null indicates that the cause is nonexistent or
     *                unknown.
     */
    public CompressorException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

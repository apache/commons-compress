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

import java.io.IOException;

/**
 * Signals a problem with a Pack200 coding or decoding issue.
 */
public class Pack200Exception extends IOException {

    private static final long serialVersionUID = 5168177401552611803L;

    /**
     * Constructs an {@code Pack200Exception} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public Pack200Exception(final String message) {
        super(message);
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
}

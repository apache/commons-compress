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
 * Thrown when decompression exceeds a configured decompression-bomb limit, either the absolute decompressed-size cap or the
 * mean compression-ratio guard enforced by {@link BombGuardCompressorInputStream} (also configurable on
 * {@link CompressorStreamFactory}).
 * <p>
 * This is an {@link java.io.IOException}, so callers that already handle stream failures handle a decompression bomb the same
 * way; the protection is off by default and must be configured to take effect.
 * </p>
 *
 * @since 1.29.0
 */
public class DecompressionBombException extends CompressException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an instance with a formatted detail message.
     *
     * @param format the {@link java.util.Formatter} format string.
     * @param args   the format arguments.
     */
    public DecompressionBombException(final String format, final Object... args) {
        super(format, args);
    }
}

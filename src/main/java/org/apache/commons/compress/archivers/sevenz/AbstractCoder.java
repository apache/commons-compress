/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.sevenz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.ByteUtils;

/**
 * Abstracts a base Codec class.
 */
abstract class AbstractCoder {

    /**
     * If the option represents a number, return its integer value, otherwise return the given default value.
     *
     * @param options      A Number.
     * @param defaultValue A default value if options is not a number.
     * @return The given number or default value.
     */
    protected static int toInt(final Object options, final int defaultValue) {
        return options instanceof Number ? ((Number) options).intValue() : defaultValue;
    }

    private final Class<?>[] optionClasses;

    /**
     * Constructs a new instance.
     *
     * @param optionClasses types that can be used as options for this codec.
     */
    protected AbstractCoder(final Class<?>... optionClasses) {
        this.optionClasses = Objects.requireNonNull(optionClasses, "optionClasses");
    }

    /**
     * Decodes using stream that reads from in using the configured coder and password.
     *
     * @return a stream that reads from in using the configured coder and password.
     */
    abstract InputStream decode(final String archiveName, final InputStream in, long uncompressedLength, final Coder coder, byte[] password,
            int maxMemoryLimitInKb) throws IOException;

    /**
     * Encodes using a stream that writes to out using the given configuration.
     *
     * @return a stream that writes to out using the given configuration.
     * @throws IOException Optionally thrown by subclassses.
     */
    OutputStream encode(final OutputStream out, final Object options) throws IOException {
        throw new UnsupportedOperationException("Method doesn't support writing");
    }

    /**
     * Gets property bytes to write in a Folder block.
     *
     * @return property bytes to write in a Folder block.
     * @throws IOException Optionally thrown by subclassses.
     */
    byte[] getOptionsAsProperties(final Object options) throws IOException {
        return ByteUtils.EMPTY_BYTE_ARRAY;
    }

    /**
     * Gets configuration options that have been used to create the given InputStream from the given Coder.
     *
     * @return configuration options that have been used to create the given InputStream from the given Coder
     * @throws IOException Optionally thrown by subclassses.
     */
    Object getOptionsFromCoder(final Coder coder, final InputStream in) throws IOException {
        return null;
    }

    /**
     * Tests whether this method can extract options from the given object.
     *
     * @return whether this method can extract options from the given object.
     */
    boolean isOptionInstance(final Object opts) {
        return Stream.of(optionClasses).anyMatch(c -> c.isInstance(opts));
    }
}

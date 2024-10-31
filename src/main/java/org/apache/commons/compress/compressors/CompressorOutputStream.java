/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors;

import java.io.FilterOutputStream;
import java.io.OutputStream;

/**
 * Abstracts all classes that compress an output stream.
 *
 * @param <T> The underlying OutputStream type.
 */
public abstract class CompressorOutputStream<T extends OutputStream> extends FilterOutputStream {

    /**
     * Constructs a new instance without a backing OutputStream.
     * <p>
     * You must initialize {@code this.out} after construction.
     * </p>
     */
    public CompressorOutputStream() {
        super(null);
    }

    /**
     * Creates an output stream filter built on top of the specified underlying output stream.
     *
     * @param out the underlying output stream to be assigned to the field {@code this.out} for later use, or {@code null} if this instance is to be created
     *            without an underlying stream.
     * @since 1.27.0
     */
    public CompressorOutputStream(final T out) {
        super(out);
    }

    /**
     * Gets the underlying output stream.
     *
     * @return the underlying output stream.
     * @since 1.27.0
     */
    @SuppressWarnings("unchecked")
    protected T out() {
        return (T) out;
    }
}

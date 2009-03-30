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
package org.apache.commons.compress.archivers;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Archive output stream implementations are expected to override the
 * {@link #write(byte[], int, int)} method to improve performance.
 * They should also override {@link #close()} to ensure that any necessary
 * trailers are added.
 * <p>
 * Example usage:<br/>
 * TBA
 */
public abstract class ArchiveOutputStream extends OutputStream {
    
    /** Temporary buffer used for the {@link #write(int)} method */
    private final byte[] oneByte = new byte[1];
    static final int BYTE_MASK = 0xFF;

    public abstract void putArchiveEntry(ArchiveEntry entry) throws IOException;

    /**
     * Closes the archive entry, writing any trailer information that may
     * be required.
     * @throws IOException
     */
    public abstract void closeArchiveEntry() throws IOException;

    // Generic implementations of OutputStream methods that may be useful to sub-classes
    
    /**
     * Writes a byte to the current archive entry.
     *
     * This method simply calls write( byte[], 0, 1 ).
     *
     * MUST be overridden if the {@link #write(byte[], int, int)} method
     * is not overridden; may be overridden otherwise.
     * 
     * @param b The byte to be written.
     * @throws IOException on error
     */
    public void write(int b) throws IOException {
        oneByte[0] = (byte) (b & BYTE_MASK);
        write(oneByte, 0, 1);
    }

}

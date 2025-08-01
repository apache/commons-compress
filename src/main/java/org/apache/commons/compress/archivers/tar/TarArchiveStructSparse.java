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
package org.apache.commons.compress.archivers.tar;

import java.util.Objects;

/**
 * A {@code struct sparse} in a <a href="https://www.gnu.org/software/tar/manual/html_node/Standard.html">Tar archive</a>.
 * <p>
 * Whereas, "struct sparse" is:
 * </p>
 * <pre>
 * struct sparse {
 * char offset[12];   // offset 0
 * char numbytes[12]; // offset 12
 * };
 * </pre>
 *
 * @since 1.20
 */
public final class TarArchiveStructSparse {
    private final long offset;
    private final long numbytes;

    /**
     * Constructs a new instance.
     *
     * @param offset An offset greater or equal to zero.
     * @param numBytes An count greater or equal to zero.
     */
    public TarArchiveStructSparse(final long offset, final long numBytes) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (numBytes < 0) {
            throw new IllegalArgumentException("numbytes must not be negative");
        }
        this.offset = offset;
        this.numbytes = numBytes;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TarArchiveStructSparse that = (TarArchiveStructSparse) o;
        return offset == that.offset && numbytes == that.numbytes;
    }

    /**
     * Gets the byte count.
     *
     * @return the byte count.
     */
    public long getNumbytes() {
        return numbytes;
    }

    /**
     * Gets the offset.
     *
     * @return the offset.
     */
    public long getOffset() {
        return offset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, numbytes);
    }

    @Override
    public String toString() {
        return "TarArchiveStructSparse{offset=" + offset + ", numbytes=" + numbytes + '}';
    }
}

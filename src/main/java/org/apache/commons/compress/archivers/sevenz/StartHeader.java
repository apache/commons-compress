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
package org.apache.commons.compress.archivers.sevenz;

final class StartHeader {

    final long nextHeaderOffset;
    final int nextHeaderSize;
    final long nextHeaderCrc;

    StartHeader(final long nextHeaderOffset, final int nextHeaderSize, final long nextHeaderCrc) {
        // The interval [SIGNATURE_HEADER_SIZE + nextHeaderOffset, SIGNATURE_HEADER_SIZE + nextHeaderOffset + nextHeaderSize)
        // must be a valid range of the file, in particular must be within [0, Long.MAX_VALUE).
        assert nextHeaderOffset >= 0 && nextHeaderOffset <= Long.MAX_VALUE - SevenZFile.SIGNATURE_HEADER_SIZE;
        assert nextHeaderSize >= 0 && nextHeaderSize <= Long.MAX_VALUE - SevenZFile.SIGNATURE_HEADER_SIZE - nextHeaderOffset;
        this.nextHeaderOffset = nextHeaderOffset;
        this.nextHeaderSize = nextHeaderSize;
        this.nextHeaderCrc = nextHeaderCrc;
    }

    /**
     * Gets the position of the next header in the file.
     *
     * @return the position of the next header
     */
    long position() {
        return SevenZFile.SIGNATURE_HEADER_SIZE + nextHeaderOffset;
    }
}

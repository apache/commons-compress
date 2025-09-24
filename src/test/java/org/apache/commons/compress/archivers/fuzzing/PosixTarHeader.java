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
package org.apache.commons.compress.archivers.fuzzing;

import java.nio.ByteBuffer;

public class PosixTarHeader extends AbstractTarHeader {
    public PosixTarHeader(String fileName, long fileSize, long lastModifiedTime, byte linkIndicator, String linkName) {
        super(fileName, fileSize, lastModifiedTime, linkIndicator, linkName, USTAR_MAGIC_AND_VERSION);
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        final int startPosition = buffer.position();
        super.writeTo(buffer);
        while (buffer.position() - startPosition < 512) {
            buffer.put((byte) 0); // Pad to 512 bytes
        }
        addChecksum(buffer, startPosition);
    }

}

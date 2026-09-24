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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.channels.ByteArraySeekableByteChannel;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.junit.jupiter.api.Test;

/**
 * Tests that a kPackInfo declaring packed streams but omitting the kSize property is rejected instead of leaving an empty pack size array behind.
 */
class SevenZMissingPackSizesTest {

    /**
     * Builds a one folder, one entry archive whose kPackInfo declares one packed stream. When {@code withSize} is false the kSize property that carries the
     * pack sizes is omitted, which a compliant archive with a non-zero packed stream count must not do.
     *
     * @param withSize whether to write the kSize property.
     * @return the archive bytes.
     */
    private static byte[] archive(final boolean withSize) {
        final UnsynchronizedByteArrayOutputStream header = UnsynchronizedByteArrayOutputStream.builder().get();
        header.write(NID.kHeader);
        header.write(NID.kMainStreamsInfo);
        header.write(NID.kPackInfo);
        header.write(0); // packPos
        header.write(1); // numPackStreams
        if (withSize) {
            header.write(NID.kSize);
            header.write(1); // pack size
        }
        header.write(NID.kEnd); // of kPackInfo
        header.write(NID.kUnpackInfo);
        header.write(NID.kFolder);
        header.write(1); // one folder
        header.write(0); // not external
        header.write(1); // one coder
        header.write(1); // a one byte coder id, one input and one output stream, no properties
        header.write(0); // coder id COPY
        header.write(NID.kCodersUnpackSize);
        header.write(1); // unpack size
        header.write(NID.kEnd); // of kUnpackInfo
        header.write(NID.kSubStreamsInfo);
        header.write(NID.kEnd); // of kSubStreamsInfo, one substream for the folder
        header.write(NID.kEnd); // of kMainStreamsInfo
        header.write(NID.kFilesInfo);
        header.write(1); // one entry
        header.write(NID.kName);
        header.write(1 + 4); // one UTF-16LE name
        header.write(0); // not external
        header.write('a');
        header.write(0);
        header.write(0);
        header.write(0);
        header.write(NID.kEnd); // of kFilesInfo
        header.write(NID.kEnd); // of kHeader
        final byte[] headerBytes = header.toByteArray();
        final CRC32 crc = new CRC32();
        crc.update(headerBytes, 0, headerBytes.length);
        final ByteBuffer startHeader = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        startHeader.putLong(1); // next header offset, past the one packed stream byte
        startHeader.putLong(headerBytes.length); // next header size
        startHeader.putInt((int) crc.getValue());
        crc.reset();
        crc.update(startHeader.array(), 0, startHeader.capacity());
        final ByteArrayOutputStream archive = new ByteArrayOutputStream();
        archive.write(SevenZFile.SIGNATURE, 0, SevenZFile.SIGNATURE.length);
        archive.write(0); // major version
        archive.write(4); // minor version
        final ByteBuffer startHeaderCrc = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        startHeaderCrc.putInt((int) crc.getValue());
        archive.write(startHeaderCrc.array(), 0, startHeaderCrc.capacity());
        archive.write(startHeader.array(), 0, startHeader.capacity());
        archive.write(0); // the one packed stream byte
        archive.write(headerBytes, 0, headerBytes.length);
        return archive.toByteArray();
    }

    private static SevenZFile open(final boolean withSize) throws IOException {
        return SevenZFile.builder().setChannel(ByteArraySeekableByteChannel.builder().setByteArray(archive(withSize)).get()).get();
    }

    @Test
    void testMissingPackSizesRejected() {
        assertThrows(ArchiveException.class, () -> {
            try (SevenZFile sevenZFile = open(false)) {
                sevenZFile.getNextEntry();
            }
        });
    }

    @Test
    void testPresentPackSizesAccepted() throws IOException {
        try (SevenZFile sevenZFile = open(true)) {
            final SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            assertNotNull(entry);
            assertEquals("a", entry.getName());
            assertEquals(1, entry.getSize());
            assertNull(sevenZFile.getNextEntry());
        }
    }
}

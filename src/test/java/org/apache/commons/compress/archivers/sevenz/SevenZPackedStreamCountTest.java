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
 * Tests the number of packed streams the folders of an archive consume against the number of packed streams the header declares.
 */
class SevenZPackedStreamCountTest {

    /**
     * Builds an archive of {@code numFolders} folders, each holding a single one byte entry behind one COPY coder, and a header declaring
     * {@code numPackStreams} packed streams. Every folder consumes one packed stream, so the archive is well formed only when the two counts are equal.
     *
     * @param numPackStreams the number of packed streams the header declares, must be lower than 128.
     * @param numFolders     the number of folders, must be lower than 128.
     * @return the archive bytes.
     */
    private static byte[] archive(final int numPackStreams, final int numFolders) {
        final UnsynchronizedByteArrayOutputStream header = UnsynchronizedByteArrayOutputStream.builder().get();
        header.write(NID.kHeader);
        header.write(NID.kMainStreamsInfo);
        header.write(NID.kPackInfo);
        header.write(0); // packPos
        header.write(numPackStreams);
        header.write(NID.kSize);
        writeRepeat(1, numPackStreams, header); // pack size
        header.write(NID.kEnd); // of kPackInfo
        header.write(NID.kUnpackInfo);
        header.write(NID.kFolder);
        header.write(numFolders);
        header.write(0); // not external
        for (int i = 0; i < numFolders; i++) {
            header.write(1); // one coder
            header.write(1); // a one byte coder id, one input and one output stream, no properties
            header.write(0); // coder id COPY
        }
        header.write(NID.kCodersUnpackSize);
        writeRepeat(1, numFolders, header); // unpack size
        header.write(NID.kEnd); // of kUnpackInfo
        header.write(NID.kSubStreamsInfo);
        header.write(NID.kEnd); // of kSubStreamsInfo, one substream per folder
        header.write(NID.kEnd); // of kMainStreamsInfo
        header.write(NID.kFilesInfo);
        header.write(numFolders); // one entry per folder
        header.write(NID.kName);
        header.write(1 + 4 * numFolders);
        header.write(0); // not external
        for (int i = 0; i < numFolders; i++) {
            header.write('a' + i); // a one character UTF-16LE name
            writeRepeat(0, 3, header); // terminator
        }
        header.write(NID.kEnd); // of kFilesInfo
        header.write(NID.kEnd); // of kHeader
        final byte[] headerBytes = header.toByteArray();
        final CRC32 crc = new CRC32();
        crc.update(headerBytes, 0, headerBytes.length);
        final ByteBuffer startHeader = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        startHeader.putLong(numPackStreams); // next header offset, past the packed streams
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
        archive.write(new byte[numPackStreams], 0, numPackStreams); // the packed streams
        archive.write(headerBytes, 0, headerBytes.length);
        return archive.toByteArray();
    }

    private static SevenZFile open(final int numPackStreams, final int numFolders) throws IOException {
        return SevenZFile.builder().setChannel(ByteArraySeekableByteChannel.builder().setByteArray(archive(numPackStreams, numFolders)).get()).get();
    }

    static void writeRepeat(final int value, final int numPackStreams, final UnsynchronizedByteArrayOutputStream header) {
        // TODO use UnsynchronizedByteArrayOutputStream.writeRepeat(int, int) in Commons IO 2.23.0
        for (int i = 0; i < numPackStreams; i++) {
            header.write(value);
        }
    }

    @Test
    void testFoldersConsumingMorePackedStreamsThanDeclared() {
        // the second folder maps to a packed stream index the header never declared
        assertThrows(ArchiveException.class, () -> open(1, 2).close());
    }

    @Test
    void testMatchingPackedStreamCount() throws IOException {
        try (SevenZFile sevenZFile = open(2, 2)) {
            for (int i = 0; i < 2; i++) {
                final SevenZArchiveEntry entry = sevenZFile.getNextEntry();
                assertNotNull(entry);
                assertEquals(String.valueOf((char) ('a' + i)), entry.getName());
                assertEquals(1, entry.getSize());
            }
            assertNull(sevenZFile.getNextEntry());
        }
    }
}

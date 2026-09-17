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
package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import org.apache.commons.compress.archivers.ArchiveException;

/**
 * The PKZIP UNIX hard link convention. The marker is in external attributes, not the general-purpose UTF-8 flags.
 */
final class UnixHardLink {

    private static final ZipShort UNIX_EXTRA_FIELD = new ZipShort(0x000d);
    private static final long HARD_LINK_FLAG = 0x800L;
    private static final int FIXED_SIZE = 12;

    /**
     * Validates a marked reference and decodes its PKWARE target pathname.
     */
    static String getTarget(final ZipArchiveEntry entry, final Charset charset) throws IOException {
        // Hard link references carry their target in metadata and have no stored body.
        if (entry.isDirectory() || entry.getMethod() != ZipMethod.STORED.getCode() || entry.getSize() != 0 || entry.getCompressedSize() != 0
                || entry.getCrc() != 0 || entry.getGeneralPurposeBit().usesEncryption()) {
            throw new ArchiveException("Invalid UNIX hard link entry '%s': expected an unencrypted empty stored entry", entry.getName());
        }
        // Accept either header's target, but reject conflicting local and central values.
        final ZipExtraField field = entry.getExtraField(UNIX_EXTRA_FIELD);
        if (field == null) {
            throw new ArchiveException("Missing UNIX hard link target for '%s'", entry.getName());
        }
        final byte[] central = targetBytes(entry, field.getCentralDirectoryData());
        final byte[] local = targetBytes(entry, field.getLocalFileDataData());
        if (central != null && local != null && !Arrays.equals(central, local)) {
            throw new ArchiveException("Conflicting UNIX hard link targets for '%s'", entry.getName());
        }
        final byte[] target = central != null ? central : local;
        if (target == null) {
            throw new ArchiveException("Missing UNIX hard link target for '%s'; local file header data may be required", entry.getName());
        }
        // Replacement characters could name an unrelated member, so target decoding must be strict.
        final String name;
        try {
            name = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(target)).toString();
        } catch (final CharacterCodingException ex) {
            throw new ArchiveException("Invalid UNIX hard link target encoding for '" + entry.getName() + "'", (Throwable) ex);
        }
        if (name.isEmpty() || name.indexOf('\0') >= 0) {
            throw new ArchiveException("Invalid UNIX hard link target for '%s'", entry.getName());
        }
        return name;
    }

    /**
     * Tests the regular-file marker, optionally accepting the historical FAT-platform convention.
     */
    static boolean isMarked(final ZipArchiveEntry entry, final boolean allowLegacy) {
        // Host and mode checks keep Windows compressed attributes from becoming hard links.
        return entry != null && isRegularFile(entry) && (entry.getExternalAttributes() & HARD_LINK_FLAG) != 0
                && (entry.getPlatform() == ZipArchiveEntry.PLATFORM_UNIX
                    || allowLegacy && entry.getPlatform() == ZipArchiveEntry.PLATFORM_FAT && entry.getExtraField(UNIX_EXTRA_FIELD) != null);
    }

    /**
     * Tests the raw UNIX file type even when the declared host platform is FAT.
     */
    static boolean isRegularFile(final ZipArchiveEntry entry) {
        return (entry.getExternalAttributes() >>> 16 & UnixStat.FILE_TYPE_FLAG) == UnixStat.FILE_FLAG;
    }

    /**
     * Extracts the unterminated target bytes, or null when this header has no target.
     */
    private static byte[] targetBytes(final ZipArchiveEntry entry, final byte[] data) throws IOException {
        // Empty data and the fixed timestamp/ownership prefix alone contain no pathname.
        if (data == null || data.length == 0) {
            return null;
        }
        if (data.length < FIXED_SIZE) {
            throw new ArchiveException("Truncated UNIX extra field in hard link '%s'", entry.getName());
        }
        if (data.length == FIXED_SIZE) {
            return null;
        }
        // APPNOTE stores the pathname without a terminating NUL.
        for (int i = FIXED_SIZE; i < data.length; i++) {
            if (data[i] == 0) {
                throw new ArchiveException("NUL in UNIX hard link target for '%s'", entry.getName());
            }
        }
        return Arrays.copyOfRange(data, FIXED_SIZE, data.length);
    }

    /**
     * Prevents instantiation of this utility class.
     */
    private UnixHardLink() {
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.PKWareExtraHeader.HashAlgorithm;
import org.junit.jupiter.api.Test;

class X0015_X0016_CertificateIdTest {

    // RCount 0x00010001 (4 bytes LE) then HashAlg SHA256 0x800C (2 bytes LE).
    // The low 16 bits of RCount (1) and its high 16 bits (1) are chosen so that a 2-byte read of RCount at
    // the wrong width/offset would yield rcount == 1 and hashAlg == CRC32 (code 1) instead of the real values.
    private static final byte[] CENTRAL_DATA = { 0x01, 0x00, 0x01, 0x00, 0x0C, (byte) 0x80 };

    @Test
    void testX0015ReadsFourByteRecordCountAndHashAlg() throws ZipException {
        final X0015_CertificateIdForFile field = new X0015_CertificateIdForFile();
        field.parseFromCentralDirectoryData(CENTRAL_DATA, 0, CENTRAL_DATA.length);
        assertEquals(0x00010001, field.getRecordCount());
        assertEquals(HashAlgorithm.SHA256, field.getHashAlgorithm());
    }

    @Test
    void testX0016ReadsFourByteRecordCountAndHashAlg() throws ZipException {
        final X0016_CertificateIdForCentralDirectory field = new X0016_CertificateIdForCentralDirectory();
        field.parseFromCentralDirectoryData(CENTRAL_DATA, 0, CENTRAL_DATA.length);
        assertEquals(0x00010001, field.getRecordCount());
        assertEquals(HashAlgorithm.SHA256, field.getHashAlgorithm());
    }

    @Test
    void testX0015RejectsTruncatedHashAlg() {
        final X0015_CertificateIdForFile field = new X0015_CertificateIdForFile();
        assertThrows(ZipException.class, () -> field.parseFromCentralDirectoryData(CENTRAL_DATA, 0, 5));
    }

    @Test
    void testX0016RejectsTruncatedHashAlg() {
        final X0016_CertificateIdForCentralDirectory field = new X0016_CertificateIdForCentralDirectory();
        assertThrows(ZipException.class, () -> field.parseFromCentralDirectoryData(CENTRAL_DATA, 0, 5));
    }
}

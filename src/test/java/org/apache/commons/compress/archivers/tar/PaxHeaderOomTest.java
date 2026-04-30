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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.MemoryLimitException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TarConstants#DEFAULT_MAX_PAX_HEADER_SIZE} enforcement.
 */
class PaxHeaderOomTest {

    private static final int BLOCK = 512;

    private static byte[] buildTarGzWithPaxValue(final long valueSize) throws IOException {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(buf, 8192)) {
            writeTar(gz, valueSize);
        }
        return buf.toByteArray();
    }

    private static void writeTar(final OutputStream out, final long valueSize) throws IOException {
        final String keyword = "test.data";
        final long fixedPart = 1L + keyword.length() + 1 + 1;
        long totalLen = fixedPart + valueSize;
        int lenDigits = Long.toString(totalLen).length();
        totalLen = fixedPart + valueSize + lenDigits;
        if (Long.toString(totalLen).length() != lenDigits) {
            totalLen++;
        }
        final long paxContentSize = totalLen;
        final byte[] paxPrefix = (totalLen + " " + keyword + "=").getBytes(StandardCharsets.UTF_8);

        out.write(tarHeader("PaxHeader/entry", paxContentSize, (byte) 'x'));
        out.write(paxPrefix);
        final byte[] chunk = new byte[8192];
        Arrays.fill(chunk, (byte) 'A');
        long remaining = valueSize;
        while (remaining > 0) {
            out.write(chunk, 0, (int) Math.min(remaining, chunk.length));
            remaining -= Math.min(remaining, chunk.length);
        }
        out.write('\n');
        out.write(new byte[pad(paxContentSize)]);

        final byte[] body = "hello\n".getBytes(StandardCharsets.UTF_8);
        out.write(tarHeader("entry.txt", body.length, (byte) '0'));
        out.write(body);
        out.write(new byte[pad(body.length)]);
        out.write(new byte[BLOCK * 2]);
    }

    private static byte[] tarHeader(final String name, final long size, final byte type) {
        final byte[] h = new byte[BLOCK];
        System.arraycopy(name.getBytes(StandardCharsets.UTF_8), 0, h, 0, Math.min(name.length(), 100));
        System.arraycopy("0000644\0".getBytes(), 0, h, 100, 8);
        System.arraycopy("0000000\0".getBytes(), 0, h, 108, 8);
        System.arraycopy("0000000\0".getBytes(), 0, h, 116, 8);
        System.arraycopy(String.format("%011o", size).getBytes(), 0, h, 124, 11);
        h[135] = 0;
        System.arraycopy("00000000000\0".getBytes(), 0, h, 136, 12);
        h[156] = type;
        System.arraycopy("ustar\0".getBytes(), 0, h, 257, 6);
        h[263] = '0';
        h[264] = '0';
        Arrays.fill(h, 148, 156, (byte) ' ');
        long chk = 0;
        for (final byte b : h) {
            chk += b & 0xFF;
        }
        System.arraycopy(String.format("%06o\0 ", chk).getBytes(), 0, h, 148, 8);
        return h;
    }

    private static int pad(final long len) {
        final int rem = (int) (len % BLOCK);
        return rem == 0 ? 0 : BLOCK - rem;
    }

    @Test
    void testDefaultLimitRejectsOversizedPaxHeader() throws Exception {
        final byte[] tgz = buildTarGzWithPaxValue(20L * 1024 * 1024);
        try (TarArchiveInputStream tis = new TarArchiveInputStream(
                new GZIPInputStream(new ByteArrayInputStream(tgz)))) {
            tis.getNextEntry();
            fail("Should have thrown MemoryLimitException");
        } catch (final MemoryLimitException ignored) {
        }
    }

    @Test
    void testCustomLimitAllowsHeader() throws Exception {
        final byte[] tgz = buildTarGzWithPaxValue(1024);
        try (TarArchiveInputStream tis = TarArchiveInputStream.builder()
                .setInputStream(new GZIPInputStream(new ByteArrayInputStream(tgz)))
                .setMaxPaxHeaderSize(100 * 1024 * 1024)
                .get()) {
            final TarArchiveEntry entry = tis.getNextEntry();
            assertNotNull(entry);
            assertEquals("entry.txt", entry.getName());
            assertEquals(1024, entry.getExtraPaxHeader("test.data").length());
        }
    }

    @Test
    void testDefaultLimitAllowsNormalHeader() throws Exception {
        final byte[] tgz = buildTarGzWithPaxValue(1024);
        try (TarArchiveInputStream tis = new TarArchiveInputStream(
                new GZIPInputStream(new ByteArrayInputStream(tgz)))) {
            final TarArchiveEntry entry = tis.getNextEntry();
            assertNotNull(entry);
            assertEquals("entry.txt", entry.getName());
        }
    }
}

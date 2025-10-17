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
package org.apache.commons.compress.archivers;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public final class TestArchiveGenerator {

    private static final byte[] USTAR_TRAILER = new byte[1024];
    private static final int FILE_MODE = 0100644;
    private static final int GROUP_ID = 0;
    private static final String GROUP_NAME = "group";
    // TAR
    private static final String OLD_GNU_MAGIC = "ustar  ";
    private static final int OWNER_ID = 0;
    private static final String OWNER_NAME = "owner";
    private static final String PAX_MAGIC = "ustar\u000000";
    private static final int TIMESTAMP = 0;

    private static byte[] createData(final int size) {
        final byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }

    // Very fragmented sparse file
    private static List<Pair<Integer, Integer>> createFragmentedSparseEntries(final int realSize) {
        final List<Pair<Integer, Integer>> sparseEntries = new ArrayList<>();
        for (int offset = 0; offset < realSize; offset++) {
            sparseEntries.add(Pair.of(offset, 1));
        }
        return sparseEntries;
    }

    private static byte[] createGnuSparse00PaxData(
            final Collection<? extends Pair<Integer, Integer>> sparseEntries, final int realSize) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, US_ASCII))) {
            writePaxKeyValue("GNU.sparse.size", realSize, writer);
            writePaxKeyValue("GNU.sparse.numblocks", sparseEntries.size(), writer);
            for (final Pair<Integer, Integer> entry : sparseEntries) {
                writePaxKeyValue("GNU.sparse.offset", entry.getLeft(), writer);
                writePaxKeyValue("GNU.sparse.numbytes", entry.getRight(), writer);
            }
        }
        return baos.toByteArray();
    }

    private static byte[] createGnuSparse01PaxData(
            final Collection<? extends Pair<Integer, Integer>> sparseEntries, final int realSize) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, US_ASCII))) {
            writePaxKeyValue("GNU.sparse.size", realSize, writer);
            writePaxKeyValue("GNU.sparse.numblocks", sparseEntries.size(), writer);
            final String map = sparseEntries.stream()
                    .map(e -> e.getLeft() + "," + e.getRight())
                    .collect(Collectors.joining(","));
            writePaxKeyValue("GNU.sparse.map", map, writer);
        }
        return baos.toByteArray();
    }

    private static byte[] createGnuSparse1EntriesData(final Collection<? extends Pair<Integer, Integer>> sparseEntries)
            throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, US_ASCII))) {
            writer.printf("%d\n", sparseEntries.size());
            for (final Pair<Integer, Integer> entry : sparseEntries) {
                writer.printf("%d\n", entry.getLeft());
                writer.printf("%d\n", entry.getRight());
            }
        }
        padTo512Bytes(baos.size(), baos);
        return baos.toByteArray();
    }

    private static byte[] createGnuSparse1PaxData(
            final Collection<Pair<Integer, Integer>> sparseEntries, final int realSize) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, US_ASCII))) {
            writePaxKeyValue("GNU.sparse.realsize", realSize, writer);
            writePaxKeyValue("GNU.sparse.numblocks", sparseEntries.size(), writer);
            writePaxKeyValue("GNU.sparse.major", 1, writer);
            writePaxKeyValue("GNU.sparse.minor", 0, writer);
        }
        return baos.toByteArray();
    }

    public static void createSparseFileTestCases(final Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Not a directory: " + path);
        }
        oldGnuSparse(path);
        gnuSparse00(path);
        gnuSparse01(path);
        gnuSparse1X(path);
    }

    private static void gnuSparse00(final Path path) throws IOException {
        final Path file = path.resolve("gnu-sparse-00.tar");
        try (OutputStream out = Files.newOutputStream(file)) {
            final byte[] data = createData(8 * 1024);
            final List<Pair<Integer, Integer>> sparseEntries = createFragmentedSparseEntries(data.length);
            final byte[] paxData = createGnuSparse00PaxData(sparseEntries, data.length);
            writeGnuSparse0File(data, paxData, out);
            writeUstarTrailer(out);
        }
    }

    private static void gnuSparse01(final Path path) throws IOException {
        final Path file = path.resolve("gnu-sparse-01.tar");
        try (OutputStream out = Files.newOutputStream(file)) {
            final byte[] data = createData(8 * 1024);
            final List<Pair<Integer, Integer>> sparseEntries = createFragmentedSparseEntries(data.length);
            final byte[] paxData = createGnuSparse01PaxData(sparseEntries, data.length);
            writeGnuSparse0File(data, paxData, out);
            writeUstarTrailer(out);
        }
    }

    private static void gnuSparse1X(final Path path) throws IOException {
        final Path file = path.resolve("gnu-sparse-1.tar");
        try (OutputStream out = Files.newOutputStream(file)) {
            final byte[] data = createData(8 * 1024);
            final List<Pair<Integer, Integer>> sparseEntries = createFragmentedSparseEntries(data.length);
            writeGnuSparse1File(sparseEntries, data, out);
            writeUstarTrailer(out);
        }
    }

    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Expected one argument: output directory");
            System.exit(1);
        }
        final Path path = Paths.get(args[0]);
        if (!Files.isDirectory(path)) {
            System.err.println("Not a directory: " + path);
            System.exit(1);
        }
        // Sparse file examples
        final Path sparsePath = path.resolve("sparse");
        Files.createDirectories(sparsePath);
        createSparseFileTestCases(sparsePath);
    }

    private static void oldGnuSparse(final Path path) throws IOException {
        final Path file = path.resolve("old-gnu-sparse.tar");
        try (OutputStream out = Files.newOutputStream(file)) {
            final byte[] data = createData(8 * 1024);
            final List<Pair<Integer, Integer>> sparseEntries = createFragmentedSparseEntries(data.length);
            writeOldGnuSparseFile(sparseEntries, data, data.length, out);
            writeUstarTrailer(out);
        }
    }

    private static int padTo512Bytes(final int offset, final OutputStream out) throws IOException {
        int count = offset;
        while (count % 512 != 0) {
            out.write(0);
            count++;
        }
        return count;
    }

    private static void writeGnuSparse0File(final byte[] data, final byte[] paxData, final OutputStream out)
            throws IOException {
        // PAX entry
        int offset = writeTarUstarHeader("./GNUSparseFile.1/" + "sparse-file.txt", paxData.length, PAX_MAGIC, 'x', out);
        offset = padTo512Bytes(offset, out);
        // PAX data
        out.write(paxData);
        offset += paxData.length;
        offset = padTo512Bytes(offset, out);
        // File entry
        offset += writeTarUstarHeader("sparse-file.txt", data.length, PAX_MAGIC, '0', out);
        offset = padTo512Bytes(offset, out);
        // File data
        out.write(data);
        offset += data.length;
        padTo512Bytes(offset, out);
    }

    private static void writeGnuSparse1File(
            final Collection<Pair<Integer, Integer>> sparseEntries, final byte[] data, final OutputStream out)
            throws IOException {
        // PAX entry
        final byte[] paxData = createGnuSparse1PaxData(sparseEntries, data.length);
        int offset = writeTarUstarHeader("./GNUSparseFile.1/sparse-file.txt", paxData.length, PAX_MAGIC, 'x', out);
        offset = padTo512Bytes(offset, out);
        // PAX data
        out.write(paxData);
        offset += paxData.length;
        offset = padTo512Bytes(offset, out);
        // File entry
        final byte[] sparseEntriesData = createGnuSparse1EntriesData(sparseEntries);
        offset += writeTarUstarHeader("sparse-file.txt", sparseEntriesData.length + data.length, PAX_MAGIC, '0', out);
        offset = padTo512Bytes(offset, out);
        // File data
        out.write(sparseEntriesData);
        offset += sparseEntriesData.length;
        out.write(data);
        offset += data.length;
        padTo512Bytes(offset, out);
    }

    private static int writeOctalString(final long value, final int length, final OutputStream out) throws IOException {
        int count = 0;
        final String s = Long.toOctalString(value);
        count += writeString(s, length - 1, out);
        out.write('\0');
        return ++count;
    }

    private static int writeOldGnuSparseEntries(
            final Iterable<Pair<Integer, Integer>> sparseEntries, final int limit, final OutputStream out)
            throws IOException {
        int offset = 0;
        int count = 0;
        final Iterator<Pair<Integer, Integer>> it = sparseEntries.iterator();
        while (it.hasNext()) {
            if (count >= limit) {
                out.write(1); // more entries follow
                return ++offset;
            }
            final Pair<Integer, Integer> entry = it.next();
            it.remove();
            count++;
            offset += writeOldGnuSparseEntry(entry.getLeft(), entry.getRight(), out);
        }
        while (count < limit) {
            // pad with empty entries
            offset += writeOldGnuSparseEntry(0, 0, out);
            count++;
        }
        out.write(0); // no more entries
        return ++offset;
    }

    private static int writeOldGnuSparseEntry(final int offset, final int length, final OutputStream out)
            throws IOException {
        int count = 0;
        count += writeOctalString(offset, 12, out);
        count += writeOctalString(length, 12, out);
        return count;
    }

    private static int writeOldGnuSparseExtendedHeader(
            final Iterable<Pair<Integer, Integer>> sparseEntries, final OutputStream out) throws IOException {
        int offset = 0;
        offset += writeOldGnuSparseEntries(sparseEntries, 21, out);
        offset = padTo512Bytes(offset, out);
        return offset;
    }

    private static void writeOldGnuSparseFile(
            final Collection<Pair<Integer, Integer>> sparseEntries,
            final byte[] data,
            final int realSize,
            final OutputStream out)
            throws IOException {
        int offset = writeTarUstarHeader("sparse-file.txt", data.length, OLD_GNU_MAGIC, 'S', out);
        while (offset < 386) {
            out.write(0);
            offset++;
        }
        // Sparse entries (24 bytes each)
        offset += writeOldGnuSparseEntries(sparseEntries, 4, out);
        // Real size (12 bytes)
        offset += writeOctalString(realSize, 12, out);
        offset = padTo512Bytes(offset, out);
        // Write extended headers
        while (!sparseEntries.isEmpty()) {
            offset += writeOldGnuSparseExtendedHeader(sparseEntries, out);
        }
        // Write file data
        out.write(data);
        offset += data.length;
        padTo512Bytes(offset, out);
    }

    private static void writePaxKeyValue(final String key, final int value, final PrintWriter out) {
        writePaxKeyValue(key, Integer.toString(value), out);
    }

    private static void writePaxKeyValue(final String key, final String value, final PrintWriter out) {
        final String entry = ' ' + key + "=" + value + "\n";
        // Guess length: length of length + space + entry
        final int length = String.valueOf(entry.length()).length() + entry.length();
        // Recompute if number of digits changes
        out.print(String.valueOf(length).length() + entry.length());
        out.print(entry);
    }

    private static int writeString(final String s, final int length, final OutputStream out) throws IOException {
        final byte[] bytes = s.getBytes(US_ASCII);
        out.write(bytes);
        for (int i = bytes.length; i < length; i++) {
            out.write('\0');
        }
        return length;
    }

    private static int writeTarUstarHeader(
            final String fileName,
            final long fileSize,
            final String magicAndVersion,
            final char typeFlag,
            final OutputStream out)
            throws IOException {
        int count = 0;
        // File name (100 bytes)
        count += writeString(fileName, 100, out);
        // File mode (8 bytes)
        count += writeOctalString(FILE_MODE, 8, out);
        // Owner ID (8 bytes)
        count += writeOctalString(OWNER_ID, 8, out);
        // Group ID (8 bytes)
        count += writeOctalString(GROUP_ID, 8, out);
        // File size (12 bytes)
        count += writeOctalString(fileSize, 12, out);
        // Modification timestamp (12 bytes)
        count += writeOctalString(TIMESTAMP, 12, out);
        // Checksum (8 bytes), filled with spaces for now
        count += writeString(StringUtils.repeat(' ', 7), 8, out);
        // Link indicator (1 byte)
        out.write(typeFlag);
        count++;
        // Name of linked file (100 bytes)
        count += writeString("", 100, out);
        // Magic (6 bytes) + Version (2 bytes)
        count += writeString(magicAndVersion, 8, out);
        // Owner user name (32 bytes)
        count += writeString(OWNER_NAME, 32, out);
        // Owner group name (32 bytes)
        count += writeString(GROUP_NAME, 32, out);
        // Device major number (8 bytes)
        count += writeString("", 8, out);
        // Device minor number (8 bytes)
        count += writeString("", 8, out);
        return count;
    }

    private static void writeUstarTrailer(final OutputStream out) throws IOException {
        out.write(USTAR_TRAILER);
    }

    private TestArchiveGenerator() {
        // hide constructor
    }
}

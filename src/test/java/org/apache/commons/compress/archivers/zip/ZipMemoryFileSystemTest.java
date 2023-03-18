/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.zip;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.compress.AbstractTestCase.getPath;
import static org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest.createZipArchiveEntryRequest;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

public class ZipMemoryFileSystemTest {

    static void println(final String x) {
        // System.out.println(x);
    }

    private Path dir;

    private InputStreamSupplier createPayloadSupplier(final ByteArrayInputStream payload) {
        return () -> payload;
    }

    @Test
    public void forPathsReturnCorrectClassInMemory() throws IOException {
        final Path firstFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final Path secondFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final Path lastFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        final byte[] firstBytes = Files.readAllBytes(firstFile);
        final byte[] secondBytes = Files.readAllBytes(secondFile);
        final byte[] lastBytes = Files.readAllBytes(lastFile);
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            Files.write(fileSystem.getPath("split_zip_created_by_zip.z01"), firstBytes);
            Files.write(fileSystem.getPath("split_zip_created_by_zip.z02"), secondBytes);
            Files.write(fileSystem.getPath("split_zip_created_by_zip.zip"), lastBytes);
            final ArrayList<Path> list = new ArrayList<>();
            list.add(firstFile);
            list.add(secondFile);

            SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forPaths(lastFile, list);
            assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);

            channel = ZipSplitReadOnlySeekableByteChannel.forPaths(firstFile, secondFile, lastFile);
            assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
        }
    }

    @Test
    public void positionToSomeZipSplitSegmentInMemory() throws IOException {
        final Path firstFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final Path secondFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final Path lastFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        final byte[] firstBytes = Files.readAllBytes(firstFile);
        final byte[] secondBytes = Files.readAllBytes(secondFile);
        final byte[] lastBytes = Files.readAllBytes(lastFile);
        final int firstFileSize = firstBytes.length;
        final int secondFileSize = secondBytes.length;
        final int lastFileSize = lastBytes.length;

        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path lastMemoryPath = fileSystem.getPath("split_zip_created_by_zip.zip");
            Files.write(fileSystem.getPath("split_zip_created_by_zip.z01"), firstBytes);
            Files.write(fileSystem.getPath("split_zip_created_by_zip.z02"), secondBytes);
            Files.write(lastMemoryPath, lastBytes);
            final Random random = new Random();
            final int randomDiskNumber = random.nextInt(3);
            final int randomOffset = randomDiskNumber < 2 ? random.nextInt(firstFileSize) : random.nextInt(lastFileSize);

            final ZipSplitReadOnlySeekableByteChannel channel = (ZipSplitReadOnlySeekableByteChannel)
                    ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastMemoryPath);
            channel.position(randomDiskNumber, randomOffset);
            long expectedPosition = randomOffset;

            expectedPosition += randomDiskNumber > 0 ? firstFileSize : 0;
            expectedPosition += randomDiskNumber > 1 ? secondFileSize : 0;

            assertEquals(expectedPosition, channel.position());
        }

    }

    @Test
    public void scatterFileInMemory() throws IOException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path scatterFile = fileSystem.getPath("scattertest.notzip");
            final ScatterZipOutputStream scatterZipOutputStream = ScatterZipOutputStream.pathBased(scatterFile);
            final byte[] B_PAYLOAD = "RBBBBBBS".getBytes();
            final byte[] A_PAYLOAD = "XAAY".getBytes();

            final ZipArchiveEntry zab = new ZipArchiveEntry("b.txt");
            zab.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload = new ByteArrayInputStream(B_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zab, createPayloadSupplier(payload)));

            final ZipArchiveEntry zae = new ZipArchiveEntry("a.txt");
            zae.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload1 = new ByteArrayInputStream(A_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zae, createPayloadSupplier(payload1)));

            final Path target = Files.createTempFile(dir, "scattertest", ".zip");
            final ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(target);
            scatterZipOutputStream.writeTo(outputStream);
            outputStream.close();
            scatterZipOutputStream.close();

            final ZipFile zf = new ZipFile(target.toFile());
            final ZipArchiveEntry b_entry = zf.getEntries("b.txt").iterator().next();
            assertEquals(8, b_entry.getSize());
            assertArrayEquals(B_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(b_entry)));

            final ZipArchiveEntry a_entry = zf.getEntries("a.txt").iterator().next();
            assertEquals(4, a_entry.getSize());
            assertArrayEquals(A_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(a_entry)));
            zf.close();
        }

    }

    @Test
    public void scatterFileWithCompressionAndTargetInMemory() throws IOException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path scatterFile = fileSystem.getPath("scattertest.notzip");
            final ScatterZipOutputStream scatterZipOutputStream = ScatterZipOutputStream.pathBased(scatterFile,
                    Deflater.BEST_COMPRESSION);
            final byte[] B_PAYLOAD = "RBBBBBBS".getBytes();
            final byte[] A_PAYLOAD = "XAAY".getBytes();

            final ZipArchiveEntry zab = new ZipArchiveEntry("b.txt");
            zab.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload = new ByteArrayInputStream(B_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zab, createPayloadSupplier(payload)));

            final ZipArchiveEntry zae = new ZipArchiveEntry("a.txt");
            zae.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload1 = new ByteArrayInputStream(A_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zae, createPayloadSupplier(payload1)));

            final Path target = fileSystem.getPath("scattertest.zip");
            final ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(target);
            scatterZipOutputStream.writeTo(outputStream);
            outputStream.close();
            scatterZipOutputStream.close();

            try (final ZipFile zf = new ZipFile(Files.newByteChannel(target, StandardOpenOption.READ),
                    target.getFileName().toString(), ZipEncodingHelper.UTF8, true)) {
                final ZipArchiveEntry b_entry = zf.getEntries("b.txt").iterator().next();
                assertEquals(8, b_entry.getSize());
                assertArrayEquals(B_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(b_entry)));

                final ZipArchiveEntry a_entry = zf.getEntries("a.txt").iterator().next();
                assertEquals(4, a_entry.getSize());
                assertArrayEquals(A_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(a_entry)));
            }
        }
    }

    @Test
    public void scatterFileWithCompressionInMemory() throws IOException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path scatterFile = fileSystem.getPath("scattertest.notzip");
            final ScatterZipOutputStream scatterZipOutputStream = ScatterZipOutputStream.pathBased(scatterFile,
                    Deflater.BEST_COMPRESSION);
            final byte[] B_PAYLOAD = "RBBBBBBS".getBytes();
            final byte[] A_PAYLOAD = "XAAY".getBytes();

            final ZipArchiveEntry zab = new ZipArchiveEntry("b.txt");
            zab.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload = new ByteArrayInputStream(B_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zab, createPayloadSupplier(payload)));

            final ZipArchiveEntry zae = new ZipArchiveEntry("a.txt");
            zae.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload1 = new ByteArrayInputStream(A_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zae, createPayloadSupplier(payload1)));

            final Path target = Files.createTempFile(dir, "scattertest", ".zip");
            final ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(target);
            scatterZipOutputStream.writeTo(outputStream);
            outputStream.close();
            scatterZipOutputStream.close();

            final ZipFile zf = new ZipFile(target.toFile());
            final ZipArchiveEntry b_entry = zf.getEntries("b.txt").iterator().next();
            assertEquals(8, b_entry.getSize());
            assertArrayEquals(B_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(b_entry)));

            final ZipArchiveEntry a_entry = zf.getEntries("a.txt").iterator().next();
            assertEquals(4, a_entry.getSize());
            assertArrayEquals(A_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(a_entry)));
            zf.close();
        }

    }

    @BeforeEach
    public void setup() throws IOException {
        dir = Files.createTempDirectory(UUID.randomUUID().toString());
    }

    @AfterEach
    public void tearDown() throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .peek(path -> println("Deleting: " + path.toAbsolutePath()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (final IOException ignore) {
                        }
                    });
        }
    }

    @Test
    public void zipFileInMemory() throws IOException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path scatterFile = fileSystem.getPath("scattertest.notzip");
            final ScatterZipOutputStream scatterZipOutputStream = ScatterZipOutputStream.pathBased(scatterFile,
                    Deflater.BEST_COMPRESSION);
            final byte[] B_PAYLOAD = "RBBBBBBS".getBytes();
            final byte[] A_PAYLOAD = "XAAY".getBytes();

            final ZipArchiveEntry zab = new ZipArchiveEntry("b.txt");
            zab.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload = new ByteArrayInputStream(B_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zab, createPayloadSupplier(payload)));

            final ZipArchiveEntry zae = new ZipArchiveEntry("a.txt");
            zae.setMethod(ZipEntry.DEFLATED);
            final ByteArrayInputStream payload1 = new ByteArrayInputStream(A_PAYLOAD);
            scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zae, createPayloadSupplier(payload1)));

            final Path target = fileSystem.getPath("scattertest.zip");
            final ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(target);
            scatterZipOutputStream.writeTo(outputStream);
            outputStream.close();
            scatterZipOutputStream.close();

            try (final ZipFile zf = new ZipFile(target)) {
                final ZipArchiveEntry b_entry = zf.getEntries("b.txt").iterator().next();
                assertEquals(8, b_entry.getSize());
                assertArrayEquals(B_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(b_entry)));

                final ZipArchiveEntry a_entry = zf.getEntries("a.txt").iterator().next();
                assertEquals(4, a_entry.getSize());
                assertArrayEquals(A_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(a_entry)));
            }
        }
    }

    @Test
    public void zipFromMemoryFileSystemFile() throws IOException, NoSuchAlgorithmException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path textFileInMemSys = fileSystem.getPath("test.txt");
            final byte[] bytes = new byte[100 * 1024];
            SecureRandom.getInstanceStrong().nextBytes(bytes);
            Files.write(textFileInMemSys, bytes);

            final Path zipInLocalSys = Files.createTempFile(dir, "commons-compress-memoryfs", ".zip");
            try (final ArchiveOutputStream zipOut = new ZipArchiveOutputStream(zipInLocalSys.toFile())) {
                final ZipArchiveEntry entry = new ZipArchiveEntry(textFileInMemSys, textFileInMemSys.getFileName().toString());
                entry.setSize(Files.size(textFileInMemSys));
                zipOut.putArchiveEntry(entry);

                Files.copy(textFileInMemSys, zipOut);
                zipOut.closeArchiveEntry();
                zipOut.finish();
                assertEquals(Files.size(zipInLocalSys), zipOut.getBytesWritten());
            }
        }
    }

    @Test
    public void zipFromMemoryFileSystemOutputStream() throws IOException, ArchiveException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path p = fileSystem.getPath("test.txt");
            Files.write(p, "Test".getBytes(UTF_8));

            final Path f = Files.createTempFile(dir, "commons-compress-memoryfs", ".zip");
            try (final OutputStream out = Files.newOutputStream(f);
                 final ArchiveOutputStream zipOut = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.ZIP, out)) {
                final ZipArchiveEntry entry = new ZipArchiveEntry(p, p.getFileName().toString());
                entry.setSize(Files.size(p));
                zipOut.putArchiveEntry(entry);

                Files.copy(p, zipOut);
                zipOut.closeArchiveEntry();
                assertEquals(Files.size(f), zipOut.getBytesWritten());
            }
        }
    }

    @Test
    public void zipFromMemoryFileSystemPath() throws IOException, NoSuchAlgorithmException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path textFileInMemSys = fileSystem.getPath("test.txt");
            final byte[] bytes = new byte[100 * 1024];
            SecureRandom.getInstanceStrong().nextBytes(bytes);
            Files.write(textFileInMemSys, bytes);

            final Path zipInLocalSys = Files.createTempFile(dir, "commons-compress-memoryfs", ".zip");
            try (final ArchiveOutputStream zipOut = new ZipArchiveOutputStream(zipInLocalSys)) {
                final ZipArchiveEntry entry = new ZipArchiveEntry(textFileInMemSys, textFileInMemSys.getFileName().toString());
                entry.setSize(Files.size(textFileInMemSys));
                zipOut.putArchiveEntry(entry);

                Files.copy(textFileInMemSys, zipOut);
                zipOut.closeArchiveEntry();
                zipOut.finish();
                assertEquals(Files.size(zipInLocalSys), zipOut.getBytesWritten());
            }
        }
    }

    @Test
    public void zipFromMemoryFileSystemSeekableByteChannel() throws IOException, NoSuchAlgorithmException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path textFileInMemSys = fileSystem.getPath("test.txt");
            final byte[] bytes = new byte[100 * 1024];
            SecureRandom.getInstanceStrong().nextBytes(bytes);
            Files.write(textFileInMemSys, bytes);

            final Path zipInLocalSys = Files.createTempFile(dir, "commons-compress-memoryfs", ".zip");
            try (final SeekableByteChannel byteChannel = Files.newByteChannel(zipInLocalSys,
                    EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                 final ArchiveOutputStream zipOut = new ZipArchiveOutputStream(byteChannel)) {
                final ZipArchiveEntry entry = new ZipArchiveEntry(textFileInMemSys, textFileInMemSys.getFileName().toString());
                entry.setSize(Files.size(textFileInMemSys));
                zipOut.putArchiveEntry(entry);

                Files.copy(textFileInMemSys, zipOut);
                zipOut.closeArchiveEntry();
                zipOut.finish();
                assertEquals(Files.size(zipInLocalSys), zipOut.getBytesWritten());
            }
        }
    }

    @Test
    public void zipFromMemoryFileSystemSplitFile() throws IOException, NoSuchAlgorithmException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path textFileInMemSys = fileSystem.getPath("test.txt");
            final byte[] bytes = new byte[100 * 1024];
            SecureRandom.getInstanceStrong().nextBytes(bytes);
            Files.write(textFileInMemSys, bytes);

            final Path zipInLocalSys = Files.createTempFile(dir, "commons-compress-memoryfs", ".zip");
            try (final ArchiveOutputStream zipOut = new ZipArchiveOutputStream(zipInLocalSys.toFile(), 64 * 1024L)) {
                final ZipArchiveEntry entry = new ZipArchiveEntry(textFileInMemSys, textFileInMemSys.getFileName().toString());
                entry.setSize(Files.size(textFileInMemSys));
                zipOut.putArchiveEntry(entry);

                Files.copy(textFileInMemSys, zipOut);
                zipOut.closeArchiveEntry();
                zipOut.finish();
                List<Path> splitZips;
                try (Stream<Path> paths = Files.walk(dir, 1)) {
                    splitZips = paths
                            .filter(Files::isRegularFile)
                            .peek(path -> println("Found: " + path.toAbsolutePath()))
                            .collect(Collectors.toList());
                }
                assertEquals(splitZips.size(), 2);
                assertEquals(Files.size(splitZips.get(0)) +
                        Files.size(splitZips.get(1)) - 4, zipOut.getBytesWritten());
            }
        }

    }

    @Test
    public void zipToMemoryFileSystemOutputStream() throws IOException, ArchiveException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path p = fileSystem.getPath("target.zip");

            try (final OutputStream out = Files.newOutputStream(p);
                 final ArchiveOutputStream zipOut = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.ZIP, out)) {
                final String content = "Test";
                final ZipArchiveEntry entry = new ZipArchiveEntry("test.txt");
                entry.setSize(content.length());
                zipOut.putArchiveEntry(entry);

                zipOut.write("Test".getBytes(UTF_8));
                zipOut.closeArchiveEntry();

                assertTrue(Files.exists(p));
                assertEquals(Files.size(p), zipOut.getBytesWritten());
            }
        }
    }

    @Test
    public void zipToMemoryFileSystemPath() throws IOException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path zipInMemSys = fileSystem.getPath("target.zip");

            try (final ArchiveOutputStream zipOut = new ZipArchiveOutputStream(zipInMemSys)) {
                final String content = "Test";
                final ZipArchiveEntry entry = new ZipArchiveEntry("test.txt");
                entry.setSize(content.length());
                zipOut.putArchiveEntry(entry);

                zipOut.write("Test".getBytes(UTF_8));
                zipOut.closeArchiveEntry();

                assertTrue(Files.exists(zipInMemSys));
                assertEquals(Files.size(zipInMemSys), zipOut.getBytesWritten());
            }
        }
    }

    @Test
    public void zipToMemoryFileSystemSeekableByteChannel() throws IOException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path zipInMemSys = fileSystem.getPath("target.zip");

            try (final SeekableByteChannel byteChannel = Files.newByteChannel(zipInMemSys,
                    EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE_NEW));
                 final ArchiveOutputStream zipOut = new ZipArchiveOutputStream(byteChannel)) {
                final String content = "Test";
                final ZipArchiveEntry entry = new ZipArchiveEntry("test.txt");
                entry.setSize(content.length());
                zipOut.putArchiveEntry(entry);

                zipOut.write("Test".getBytes(UTF_8));
                zipOut.closeArchiveEntry();

                assertTrue(Files.exists(zipInMemSys));
                assertEquals(Files.size(zipInMemSys), zipOut.getBytesWritten());
            }
        }
    }

    @Test
    public void zipToMemoryFileSystemSplitPath() throws IOException, NoSuchAlgorithmException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path zipInMemSys = fileSystem.getPath("target.zip");
            final byte[] bytes = new byte[100 * 1024];
            SecureRandom.getInstanceStrong().nextBytes(bytes);

            try (final ArchiveOutputStream zipOut = new ZipArchiveOutputStream(zipInMemSys, 64 * 1024L)) {
                final ZipArchiveEntry entry = new ZipArchiveEntry("test.txt");
                entry.setSize(bytes.length);
                zipOut.putArchiveEntry(entry);

                zipOut.write(bytes);

                zipOut.closeArchiveEntry();
                zipOut.finish();

                List<Path> splitZips;
                try (Stream<Path> paths = Files.walk(fileSystem.getPath("."), 1)) {
                    splitZips = paths
                            .filter(Files::isRegularFile)
                            .peek(path -> println("Found: " + path.toAbsolutePath()))
                            .collect(Collectors.toList());
                }
                assertEquals(splitZips.size(), 2);
                assertEquals(Files.size(splitZips.get(0)) +
                        Files.size(splitZips.get(1)) - 4, zipOut.getBytesWritten());
            }
        }

    }
}

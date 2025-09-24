package org.apache.commons.compress.archivers;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.apache.commons.compress.archivers.dump.DumpArchiveConstants.SEGMENT_TYPE;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry.TYPE;
import org.apache.commons.compress.archivers.fuzzing.ArHeader;
import org.apache.commons.compress.archivers.fuzzing.ArjLocalHeader;
import org.apache.commons.compress.archivers.fuzzing.ArjMainHeader;
import org.apache.commons.compress.archivers.fuzzing.CpioBinaryHeader;
import org.apache.commons.compress.archivers.fuzzing.CpioNewAsciiHeader;
import org.apache.commons.compress.archivers.fuzzing.CpioOldAsciiHeader;
import org.apache.commons.compress.archivers.fuzzing.DumpDirectoryEntry;
import org.apache.commons.compress.archivers.fuzzing.DumpLocalHeader;
import org.apache.commons.compress.archivers.fuzzing.DumpSummaryHeader;
import org.apache.commons.compress.archivers.fuzzing.PosixTarHeader;
import org.apache.commons.compress.archivers.fuzzing.ZipCentralDirectoryHeader;
import org.apache.commons.compress.archivers.fuzzing.ZipEndOfCentralDirectory;
import org.apache.commons.compress.archivers.fuzzing.ZipLocalHeader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility to generate test archives with specific properties.
 * <p>
 * Run from the command line, it takes one argument: the output directory.
 * </p>
 * <p>
 * The generated files are checked into the src/test/resources/invalid directory.
 * </p>
 */
public final class TestArchiveGenerator {

    private static final int TIMESTAMP = 0;
    private static final int OWNER_ID = 0;
    private static final int GROUP_ID = 0;

    @SuppressWarnings("OctalInteger")
    private static final int FILE_MODE = 0100644;

    // Maximum size for a Java array: AR, CPIO and TAR support longer names
    private static final int SOFT_ARRAY_MAX_SIZE = Integer.MAX_VALUE - 8;
    private static final int ARJ_MAX_SIZE = 2568; // ARJ header - fixed fields

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
        // Long name examples
        final Path longNamePath = path.resolve("long-name");
        Files.createDirectories(longNamePath);
        generateLongFileNames(longNamePath);
    }

    public static void generateLongFileNames(final Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new IOException("Not a directory: " + path);
        }
        Files.createDirectories(path);
        // AR
        arInvalidBsdLongName(path);
        arInvalidGnuLongName(path);
        arValidBsdLongName(path);
        arValidGnuLongName(path);
        // ARJ
        arjLongName(path);
        // CPIO
        cpioOldAsciiTruncatedLongNames(path);
        cpioNewAsciiTruncatedLongNames(path);
        cpioBinaryValidLongNames(path);
        cpioOldAsciiValidLongNames(path);
        cpioNewAsciiValidLongNames(path);
        // DUMP
        dumpValidLongName(path);
        dumpReversedLongName(path);
        // TAR
        tarPaxInvalidLongNames(path);
        tarGnuInvalidLongNames(path);
        tarPaxValidLongNames(path);
        tarGnuValidLongNames(path);
        // ZIP
        zipValidLongName(path);
    }

    /**
     * Generates a truncated AR archive with a very long BSD name.
     * <p>
     * The name has a declared length of {@link #SOFT_ARRAY_MAX_SIZE}, which is the largest
     * name a Java array can hold.
     * </p>
     * <p>
     * The AR archive specification allows for even longer names.
     * </p>
     * @param path The output directory
     */
    private static void arInvalidBsdLongName(final Path path) throws IOException {
        final Path file = path.resolve("bsd-fail.ar");
        try (final PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            writeArHeader(out);
            final ArHeader header = new ArHeader(
                    "#1/" + SOFT_ARRAY_MAX_SIZE, TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, SOFT_ARRAY_MAX_SIZE);
            header.writeTo(out);
        }
    }

    /**
     * Generates a valid AR archive with a very long BSD name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     * @param path The output directory
     */
    private static void arValidBsdLongName(final Path path) throws IOException {
        final Path file = path.resolve("bsd-short-max-value.ar");
        try (final PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            writeArHeader(out);
            final ArHeader header =
                    new ArHeader("#1/" + Short.MAX_VALUE, TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, Short.MAX_VALUE);
            header.writeTo(out);
            out.write(StringUtils.repeat('a', Short.MAX_VALUE));
        }
    }

    /**
     * Generates a truncated AR archive with a very long GNU name.
     * <p>
     * The name has a declared length of {@link #SOFT_ARRAY_MAX_SIZE}, which is the largest
     * name a Java array can hold.
     * </p>
     * <p>
     * The AR archive specification allows for even longer names.
     * </p>
     * @param path The output directory
     */
    private static void arInvalidGnuLongName(final Path path) throws IOException {
        final Path file = path.resolve("gnu-fail.ar");
        try (final PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            writeArHeader(out);
            final ArHeader header = new ArHeader("//", TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, SOFT_ARRAY_MAX_SIZE);
            header.writeTo(out);
        }
    }

    /**
     * Generates a valid AR archive with a very long GNU name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     * @param path The output directory
     */
    private static void arValidGnuLongName(final Path path) throws IOException {
        final Path file = path.resolve("gnu-short-max-value.ar");
        try (final PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            writeArHeader(out);
            // GNU long name table with one entry and a new line
            final ArHeader header1 = new ArHeader("//", TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, Short.MAX_VALUE + 1);
            header1.writeTo(out);
            out.write(StringUtils.repeat('a', Short.MAX_VALUE));
            // End with a new line
            out.write('\n');
            // Add a file to make the archive valid
            final ArHeader header = new ArHeader("/0", TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, 0);
            header.writeTo(out);
        }
    }

    /**
     * Generates an ARJ archive with a very long file name.
     * <p>
     * The name in ARJ must be contained in 2600 bytes of the header, and 32 bytes are used by
     * compulsory fields and null terminator, so the maximum length is 2568 bytes.
     * </p>
     * @param path The output directory
     */
    private static void arjLongName(final Path path) throws IOException {
        final Path file = path.resolve("long-name.arj");
        try (final OutputStream out = Files.newOutputStream(file)) {
            ByteBuffer buffer = ByteBuffer.allocate(IOUtils.DEFAULT_BUFFER_SIZE);
            final String longName = StringUtils.repeat('a', ARJ_MAX_SIZE);
            ArjMainHeader mainHeader = new ArjMainHeader(US_ASCII, "long-name.arj", "");
            mainHeader.writeTo(buffer);
            ArjLocalHeader localHeader = new ArjLocalHeader(US_ASCII, longName, "");
            localHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            byte[] trailer = {(byte) 0x60, (byte) 0xEA, 0x00, 0x00}; // ARJ file trailer
            out.write(trailer);
        }
    }

    /**
     * Generates CPIO binary archives with a very long file name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE} - 1.
     * </p>
     * @param path The output directory
     */
    private static void cpioBinaryValidLongNames(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', Short.MAX_VALUE - 1);
        CpioBinaryHeader header = new CpioBinaryHeader(US_ASCII, longName, 0);
        final ByteBuffer buffer = ByteBuffer.allocate(2 * Short.MAX_VALUE);
        try (OutputStream out = Files.newOutputStream(path.resolve("bin-big-endian.cpio"))) {
            header.writeTo(buffer, ByteOrder.BIG_ENDIAN);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
        }
        try (OutputStream out = Files.newOutputStream(path.resolve("bin-little-endian.cpio"))) {
            header.writeTo(buffer, ByteOrder.LITTLE_ENDIAN);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
        }
    }

    /**
     * Generates CPIO old ASCII archives with a very long file name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     * @param path The output directory
     */
    private static void cpioOldAsciiValidLongNames(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', Short.MAX_VALUE);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("odc.cpio")))) {
            CpioOldAsciiHeader header = new CpioOldAsciiHeader(US_ASCII, longName, 0);
            header.writeTo(out);
        }
    }

    /**
     * Generates a truncated CPIO old ASCII archive with a very long file name.
     * <p>
     * The name has a length of {@code 0777776}, which is the largest
     * name that can be represented in the name size field of the header.
     * </p>
     * @param path The output directory
     */
    private static void cpioOldAsciiTruncatedLongNames(final Path path) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("odc-fail.cpio")))) {
            @SuppressWarnings("OctalInteger")
            CpioOldAsciiHeader header = new CpioOldAsciiHeader(US_ASCII, "", 0777776, 0);
            header.writeTo(out);
        }
    }

    /**
     * Generates CPIO new ASCII and CRC archives with a very long file name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     * @param path The output directory
     */
    private static void cpioNewAsciiValidLongNames(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', Short.MAX_VALUE);
        CpioNewAsciiHeader header = new CpioNewAsciiHeader(US_ASCII, longName, 0);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("newc.cpio")))) {
            header.writeTo(out, false);
        }
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("crc.cpio")))) {
            header.writeTo(out, true);
        }
    }

    /**
     * Generates a truncated CPIO new ASCII archive with a very long file name.
     * <p>
     * The name has a length of {@code SOFT_MAX_ARRAY_SIZE}, which is the largest
     * name that can be theoretically represented in Java.
     * </p>
     * <p>
     * The CPIO archive specification allows for even longer names.
     * </p>
     * @param path The output directory
     */
    private static void cpioNewAsciiTruncatedLongNames(final Path path) throws IOException {
        CpioNewAsciiHeader header = new CpioNewAsciiHeader(US_ASCII, "", SOFT_ARRAY_MAX_SIZE, 0);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("newc-fail.cpio")))) {
            header.writeTo(out, false);
        }
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("crc-fail.cpio")))) {
            header.writeTo(out, true);
        }
    }

    /**
     * Generates a TAR archive with a very long name using the PAX format.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     * @param path The output directory
     */
    private static void tarPaxValidLongNames(final Path path) throws IOException {
        final byte[] paxEntryContent = createPaxKeyValue("path", StringUtils.repeat('a', Short.MAX_VALUE))
                .getBytes(US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(512);
        PosixTarHeader paxHeader = new PosixTarHeader("PaxHeader/long", paxEntryContent.length, 0, (byte) 'x', "");
        PosixTarHeader fileHeader = new PosixTarHeader("a", 0, 0, (byte) '0', "");
        try (OutputStream out = Files.newOutputStream(path.resolve("pax.tar"))) {
            paxHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            out.write(paxEntryContent);
            padTo512Bytes(paxEntryContent.length, out);
            buffer.clear();
            fileHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            writeUstarTrailer(out);
        }
    }

    /**
     * Generates a truncated TAR archive with a very long name using the PAX format.
     * <p>
     * The name has a declared length of {@link #SOFT_ARRAY_MAX_SIZE}, which is the largest
     * name a Java array can hold.
     * </p>
     * <p>
     * The TAR archive specification allows for even longer names.
     * </p>
     * @param path The output directory
     */
    private static void tarPaxInvalidLongNames(final Path path) throws IOException {
        // The size of a pax entry for a file with a name of SOFT_ARRAY_MAX_SIZE
        final long paxEntrySize =
                String.valueOf(SOFT_ARRAY_MAX_SIZE).length() + " path=".length() + SOFT_ARRAY_MAX_SIZE + "\n".length();
        ByteBuffer buffer = ByteBuffer.allocate(512);
        PosixTarHeader paxHeader = new PosixTarHeader("PaxHeader/long", paxEntrySize, 0, (byte) 'x', "");
        try (WritableByteChannel out = Files.newByteChannel(
                path.resolve("pax-fail.tar"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            paxHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer);
        }
    }

    /**
     * Generates a TAR archive with a very long name using the old GNU format.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     * @param path The output directory
     */
    private static void tarGnuValidLongNames(final Path path) throws IOException {
        final byte[] gnuEntryContent = StringUtils.repeat('a', Short.MAX_VALUE).getBytes(US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(512);
        PosixTarHeader gnuHeader = new PosixTarHeader("././@LongLink", gnuEntryContent.length, 0, (byte) 'L', "");
        PosixTarHeader fileHeader = new PosixTarHeader("a", 0, 0, (byte) '0', "");
        try (OutputStream out = Files.newOutputStream(path.resolve("gnu.tar"))) {
            gnuHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            out.write(gnuEntryContent);
            padTo512Bytes(gnuEntryContent.length, out);
            buffer.clear();
            fileHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            writeUstarTrailer(out);
        }
    }

    /**
     * Generates a truncated TAR archive with a very long name using the old GNU format.
     * <p>
     * The name has a declared length of {@link #SOFT_ARRAY_MAX_SIZE}, which is the largest
     * name a Java array can hold.
     * </p>
     * <p>
     * The TAR archive specification allows for even longer names.
     * </p>
     * @param path The output directory
     */
    private static void tarGnuInvalidLongNames(final Path path) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        PosixTarHeader gnuHeader = new PosixTarHeader("././@LongLink", SOFT_ARRAY_MAX_SIZE, 0, (byte) 'L', "");
        try (WritableByteChannel out = Files.newByteChannel(
                path.resolve("gnu-fail.tar"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            gnuHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer);
        }
    }

    /**
     * Generates a Dump archive with a very long name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE} - 1, which is the longest
     * name that can be represented in a DumpDirectoryEntry.
     * </p>
     * @param path The output directory
     */
    private static void dumpValidLongName(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', 255);
        try (OutputStream out = Files.newOutputStream(path.resolve("long-name.dump"))) {
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            // Archive summary
            DumpSummaryHeader summary = new DumpSummaryHeader(1);
            summary.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // Ignored records
            DumpLocalHeader header = new DumpLocalHeader(SEGMENT_TYPE.CLRI, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            header = new DumpLocalHeader(SEGMENT_TYPE.BITS, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // 128 directory entries with a single file of very long name
            //
            // The first directory is the root directory with an empty name.
            // The total path length for the file will be 127 * 256 + 255 = Short.MAX_VALUE
            final int rootInode = 2;
            for (int i = rootInode; i < 128 + rootInode; i++) {
                writeSingleFileDumpDirectory(i, longName, out);
            }
            // Empty file
            header = new DumpLocalHeader(SEGMENT_TYPE.INODE, TYPE.FILE, 1, 128 + rootInode, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // End of dump
            header = new DumpLocalHeader(SEGMENT_TYPE.END, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
        }
    }

    /**
     * Generates a Dump archive with a very long name, but with the directories in reverse order.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE} - 1, which is the longest
     * name that can be represented in a DumpDirectoryEntry.
     * </p>
     * @param path The output directory
     */
    private static void dumpReversedLongName(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', 255);
        try (OutputStream out = Files.newOutputStream(path.resolve("long-name-reversed.dump"))) {
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            // Archive summary
            DumpSummaryHeader summary = new DumpSummaryHeader(1);
            summary.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // Ignored records
            DumpLocalHeader header = new DumpLocalHeader(SEGMENT_TYPE.CLRI, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            header = new DumpLocalHeader(SEGMENT_TYPE.BITS, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // Empty file
            final int rootInode = 2;
            header = new DumpLocalHeader(SEGMENT_TYPE.INODE, TYPE.FILE, 1, 128 + rootInode, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // 128 directory entries with a single file of very long name
            //
            // The first directory is the root directory with an empty name.
            // The total path length for the file will be 127 * 256 + 255 = Short.MAX_VALUE
            for (int i = 127 + rootInode; i >= rootInode; i--) {
                writeSingleFileDumpDirectory(i, longName, out);
            }
            // End of dump
            header = new DumpLocalHeader(SEGMENT_TYPE.END, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
        }
    }

    /**
     * Generates a ZIP archive with a very long name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}, which is the longest
     * name that can be represented in a ZIP local file header.
     * </p>
     * @param path The output directory
     */
    private static void zipValidLongName(final Path path) throws IOException {
        try (OutputStream out = Files.newOutputStream(path.resolve("long-name.zip"))) {
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            // File entry
            String fileName = StringUtils.repeat('a', Short.MAX_VALUE);
            ZipLocalHeader header = new ZipLocalHeader(US_ASCII, fileName, 0, 0);
            header.writeTo(buffer);
            final int offsetCentralDirectory = buffer.position();
            writeByteBuffer(buffer, out);
            // Central directory entry
            ZipCentralDirectoryHeader centralHeader = new ZipCentralDirectoryHeader(US_ASCII, fileName, 0);
            centralHeader.writeTo(buffer);
            final int sizeCentralDirectory = buffer.position();
            writeByteBuffer(buffer, out);
            // End of central directory
            ZipEndOfCentralDirectory end =
                    new ZipEndOfCentralDirectory(1, sizeCentralDirectory, offsetCentralDirectory);
            end.writeTo(buffer);
            writeByteBuffer(buffer, out);
        }
    }

    private static void writeSingleFileDumpDirectory(int inode, String fileName, OutputStream out) throws IOException {
        final DumpDirectoryEntry dotEntry = new DumpDirectoryEntry(inode, ".");
        final DumpDirectoryEntry dotDotEntry = new DumpDirectoryEntry(inode > 2 ? inode - 1 : inode, "..");
        final DumpDirectoryEntry entry = new DumpDirectoryEntry(inode + 1, fileName);
        int totalLength = dotEntry.recordLength() + dotDotEntry.recordLength() + entry.recordLength();
        final DumpLocalHeader header = new DumpLocalHeader(SEGMENT_TYPE.INODE, TYPE.DIRECTORY, 1, inode, totalLength);
        final ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
        header.writeTo(buffer);
        writeByteBuffer(buffer, out);
        dotEntry.writeTo(buffer);
        writeByteBuffer(buffer, out);
        dotDotEntry.writeTo(buffer);
        writeByteBuffer(buffer, out);
        entry.writeTo(buffer);
        writeByteBuffer(buffer, out);
        while (totalLength % 1024 != 0) {
            out.write(0);
            totalLength++;
        }
    }

    private static void writeByteBuffer(final ByteBuffer buffer, final OutputStream out) throws IOException {
        buffer.flip();
        out.write(buffer.array(), 0, buffer.limit());
        buffer.clear();
    }

    private static void writeArHeader(final PrintWriter out) {
        out.print("!<arch>\n");
    }

    private static void writeUstarTrailer(final OutputStream out) throws IOException {
        int offset = 0;
        // 1024 bytes of zero
        while (offset < 1024) {
            out.write(0);
            offset++;
        }
    }

    private static String createPaxKeyValue(final String key, final String value) {
        final String entry = ' ' + key + "=" + value + "\n";
        // Guess length: length of length + space + entry
        int length = String.valueOf(entry.length()).length() + entry.length();
        // Recompute if number of digits changes
        length = String.valueOf(length).length() + entry.length();
        // Return the value
        return length + entry;
    }

    private static void padTo512Bytes(final int offset, final OutputStream out) throws IOException {
        int count = offset;
        while (count % 512 != 0) {
            out.write(0);
            count++;
        }
    }

    private TestArchiveGenerator() {
        // hide constructor
    }
}

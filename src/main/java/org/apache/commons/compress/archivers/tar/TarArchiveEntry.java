/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.tar;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.EntryStreamOffsets;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.TimeUtils;

/**
 * This class represents an entry in a Tar archive. It consists
 * of the entry's header, as well as the entry's File. Entries
 * can be instantiated in one of three ways, depending on how
 * they are to be used.
 * <p>
 * TarEntries that are created from the header bytes read from
 * an archive are instantiated with the {@link TarArchiveEntry#TarArchiveEntry(byte[])}
 * constructor. These entries will be used when extracting from
 * or listing the contents of an archive. These entries have their
 * header filled in using the header bytes. They also set the File
 * to null, since they reference an archive entry not a file.
 * </p>
 * <p>
 * TarEntries that are created from Files that are to be written
 * into an archive are instantiated with the {@link TarArchiveEntry#TarArchiveEntry(File)}
 * or {@link TarArchiveEntry#TarArchiveEntry(Path)} constructor.
 * These entries have their header filled in using the File's information.
 * They also keep a reference to the File for convenience when writing entries.
 * </p>
 * <p>
 * Finally, TarEntries can be constructed from nothing but a name.
 * This allows the programmer to construct the entry by hand, for
 * instance when only an InputStream is available for writing to
 * the archive, and the header information is constructed from
 * other information. In this case the header fields are set to
 * defaults and the File is set to null.
 * </p>
 * <p>
 * The C structure for a Tar Entry's header is:
 * </p>
 * <pre>
 * struct header {
 *   char name[100];     // TarConstants.NAMELEN    - offset   0
 *   char mode[8];       // TarConstants.MODELEN    - offset 100
 *   char uid[8];        // TarConstants.UIDLEN     - offset 108
 *   char gid[8];        // TarConstants.GIDLEN     - offset 116
 *   char size[12];      // TarConstants.SIZELEN    - offset 124
 *   char mtime[12];     // TarConstants.MODTIMELEN - offset 136
 *   char chksum[8];     // TarConstants.CHKSUMLEN  - offset 148
 *   char linkflag[1];   //                         - offset 156
 *   char linkname[100]; // TarConstants.NAMELEN    - offset 157
 *   // The following fields are only present in new-style POSIX tar archives:
 *   char magic[6];      // TarConstants.MAGICLEN   - offset 257
 *   char version[2];    // TarConstants.VERSIONLEN - offset 263
 *   char uname[32];     // TarConstants.UNAMELEN   - offset 265
 *   char gname[32];     // TarConstants.GNAMELEN   - offset 297
 *   char devmajor[8];   // TarConstants.DEVLEN     - offset 329
 *   char devminor[8];   // TarConstants.DEVLEN     - offset 337
 *   char prefix[155];   // TarConstants.PREFIXLEN  - offset 345
 *   // Used if "name" field is not long enough to hold the path
 *   char pad[12];       // NULs                    - offset 500
 * } header;
 * </pre>
 * <p>
 * All unused bytes are set to null.
 * New-style GNU tar files are slightly different from the above.
 * For values of size larger than 077777777777L (11 7s)
 * or uid and gid larger than 07777777L (7 7s)
 * the sign bit of the first byte is set, and the rest of the
 * field is the binary representation of the number.
 * See {@link TarUtils#parseOctalOrBinary(byte[], int, int)}.
 * <p>
 * The C structure for a old GNU Tar Entry's header is:
 * </p>
 * <pre>
 * struct oldgnu_header {
 *   char unused_pad1[345]; // TarConstants.PAD1LEN_GNU       - offset 0
 *   char atime[12];        // TarConstants.ATIMELEN_GNU      - offset 345
 *   char ctime[12];        // TarConstants.CTIMELEN_GNU      - offset 357
 *   char offset[12];       // TarConstants.OFFSETLEN_GNU     - offset 369
 *   char longnames[4];     // TarConstants.LONGNAMESLEN_GNU  - offset 381
 *   char unused_pad2;      // TarConstants.PAD2LEN_GNU       - offset 385
 *   struct sparse sp[4];   // TarConstants.SPARSELEN_GNU     - offset 386
 *   char isextended;       // TarConstants.ISEXTENDEDLEN_GNU - offset 482
 *   char realsize[12];     // TarConstants.REALSIZELEN_GNU   - offset 483
 *   char unused_pad[17];   // TarConstants.PAD3LEN_GNU       - offset 495
 * };
 * </pre>
 * <p>
 * Whereas, "struct sparse" is:
 * </p>
 * <pre>
 * struct sparse {
 *   char offset[12];   // offset 0
 *   char numbytes[12]; // offset 12
 * };
 * </pre>
 * <p>
 * The C structure for a xstar (Jörg Schilling star) Tar Entry's header is:
 * </p>
 * <pre>
 * struct star_header {
 *   char name[100];     // offset   0
 *   char mode[8];       // offset 100
 *   char uid[8];        // offset 108
 *   char gid[8];        // offset 116
 *   char size[12];      // offset 124
 *   char mtime[12];     // offset 136
 *   char chksum[8];     // offset 148
 *   char typeflag;      // offset 156
 *   char linkname[100]; // offset 157
 *   char magic[6];      // offset 257
 *   char version[2];    // offset 263
 *   char uname[32];     // offset 265
 *   char gname[32];     // offset 297
 *   char devmajor[8];   // offset 329
 *   char devminor[8];   // offset 337
 *   char prefix[131];   // offset 345
 *   char atime[12];     // offset 476
 *   char ctime[12];     // offset 488
 *   char mfill[8];      // offset 500
 *   char xmagic[4];     // offset 508  "tar\0"
 * };
 * </pre>
 * <p>
 * which is identical to new-style POSIX up to the first 130 bytes of the prefix.
 * </p>
 * <p>
 * The C structure for the xstar-specific parts of a xstar Tar Entry's header is:
 * </p>
 * <pre>
 * struct xstar_in_header {
 *   char fill[345];         // offset 0     Everything before t_prefix
 *   char prefix[1];         // offset 345   Prefix for t_name
 *   char fill2;             // offset 346
 *   char fill3[8];          // offset 347
 *   char isextended;        // offset 355
 *   struct sparse sp[SIH];  // offset 356   8 x 12
 *   char realsize[12];      // offset 452   Real size for sparse data
 *   char offset[12];        // offset 464   Offset for multivolume data
 *   char atime[12];         // offset 476
 *   char ctime[12];         // offset 488
 *   char mfill[8];          // offset 500
 *   char xmagic[4];         // offset 508   "tar\0"
 * };
 * </pre>
 *
 * @NotThreadSafe
 */
public class TarArchiveEntry implements ArchiveEntry, TarConstants, EntryStreamOffsets {

    private static final TarArchiveEntry[] EMPTY_TAR_ARCHIVE_ENTRY_ARRAY = {};

    /**
     * Value used to indicate unknown mode, user/groupids, device numbers and modTime when parsing a file in lenient
     * mode and the archive contains illegal fields.
     * @since 1.19
     */
    public static final long UNKNOWN = -1L;

    /** Maximum length of a user's name in the tar file */
    public static final int MAX_NAMELEN = 31;

    /** Default permissions bits for directories */
    public static final int DEFAULT_DIR_MODE = 040755;

    /** Default permissions bits for files */
    public static final int DEFAULT_FILE_MODE = 0100644;

    /**
     * Convert millis to seconds
     * @deprecated Unused.
     */
    @Deprecated
    public static final int MILLIS_PER_SECOND = 1000;

    private static FileTime fileTimeFromOptionalSeconds(final long seconds) {
        if (seconds <= 0) {
            return null;
        }
        return TimeUtils.unixTimeToFileTime(seconds);
    }

    /**
     * Strips Windows' drive letter as well as any leading slashes, turns path separators into forward slashes.
     */
    private static String normalizeFileName(String fileName, final boolean preserveAbsolutePath) {
        if (!preserveAbsolutePath) {
            final String property = System.getProperty("os.name");
            if (property != null) {
                final String osName = property.toLowerCase(Locale.ROOT);

                // Strip off drive letters!
                // REVIEW Would a better check be "(File.separator == '\')"?

                if (osName.startsWith("windows")) {
                    if (fileName.length() > 2) {
                        final char ch1 = fileName.charAt(0);
                        final char ch2 = fileName.charAt(1);

                        if (ch2 == ':' && (ch1 >= 'a' && ch1 <= 'z' || ch1 >= 'A' && ch1 <= 'Z')) {
                            fileName = fileName.substring(2);
                        }
                    }
                } else if (osName.contains("netware")) {
                    final int colon = fileName.indexOf(':');
                    if (colon != -1) {
                        fileName = fileName.substring(colon + 1);
                    }
                }
            }
        }

        fileName = fileName.replace(File.separatorChar, '/');

        // No absolute pathnames
        // Windows (and Posix?) paths can start with "\\NetworkDrive\",
        // so we loop on starting /'s.
        while (!preserveAbsolutePath && fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        return fileName;
    }

    private static Instant parseInstantFromDecimalSeconds(final String value) {
        final BigDecimal epochSeconds = new BigDecimal(value);
        final long seconds = epochSeconds.longValue();
        final long nanos = epochSeconds.remainder(BigDecimal.ONE).movePointRight(9).longValue();
        return Instant.ofEpochSecond(seconds, nanos);
    }

    /** The entry's name. */
    private String name = "";

    /** Whether to allow leading slashes or drive names inside the name */
    private final boolean preserveAbsolutePath;

    /** The entry's permission mode. */
    private int mode;

    /** The entry's user id. */
    private long userId;

    /** The entry's group id. */
    private long groupId;

    /** The entry's size. */
    private long size;

    /**
     * The entry's modification time.
     * Corresponds to the POSIX {@code mtime} attribute.
     */
    private FileTime mTime;
    /**
     * The entry's status change time.
     * Corresponds to the POSIX {@code ctime} attribute.
     *
     * @since 1.22
     */
    private FileTime cTime;

    /**
     * The entry's last access time.
     * Corresponds to the POSIX {@code atime} attribute.
     *
     * @since 1.22
     */
    private FileTime aTime;

    /**
     * The entry's creation time.
     * Corresponds to the POSIX {@code birthtime} attribute.
     *
     * @since 1.22
     */
    private FileTime birthTime;

    /** If the header checksum is reasonably correct. */
    private boolean checkSumOK;

    /** The entry's link flag. */
    private byte linkFlag;

    /** The entry's link name. */
    private String linkName = "";

    /** The entry's magic tag. */
    private String magic = MAGIC_POSIX;

    /** The version of the format */
    private String version = VERSION_POSIX;

    /** The entry's user name. */
    private String userName;

    /** The entry's group name. */
    private String groupName = "";

    /** The entry's major device number. */
    private int devMajor;

    /** The entry's minor device number. */
    private int devMinor;

    /** The sparse headers in tar */
    private List<TarArchiveStructSparse> sparseHeaders;

    /** If an extension sparse header follows. */
    private boolean isExtended;

    /** The entry's real size in case of a sparse file. */
    private long realSize;

    /** is this entry a GNU sparse entry using one of the PAX formats? */
    private boolean paxGNUSparse;

    /** is this entry a GNU sparse entry using 1.X PAX formats?
     *  the sparse headers of 1.x PAX Format is stored in file data block */
    private boolean paxGNU1XSparse;

    /** is this entry a star sparse entry using the PAX header? */
    private boolean starSparse;

    /** The entry's file reference */
    private final Path file;

    /** The entry's file linkOptions*/
    private final LinkOption[] linkOptions;

    /** Extra, user supplied pax headers     */
    private final Map<String,String> extraPaxHeaders = new HashMap<>();

    private long dataOffset = EntryStreamOffsets.OFFSET_UNKNOWN;

    /**
     * Construct an empty entry and prepares the header values.
     */
    private TarArchiveEntry(final boolean preserveAbsolutePath) {
        String user = System.getProperty("user.name", "");

        if (user.length() > MAX_NAMELEN) {
            user = user.substring(0, MAX_NAMELEN);
        }

        this.userName = user;
        this.file = null;
        this.linkOptions = IOUtils.EMPTY_LINK_OPTIONS;
        this.preserveAbsolutePath = preserveAbsolutePath;
    }

    /**
     * Construct an entry from an archive's header bytes. File is set
     * to null.
     *
     * @param headerBuf The header bytes from a tar archive entry.
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format
     */
    public TarArchiveEntry(final byte[] headerBuf) {
        this(false);
        parseTarHeader(headerBuf);
    }

    /**
     * Construct an entry from an archive's header bytes. File is set
     * to null.
     *
     * @param headerBuf The header bytes from a tar archive entry.
     * @param encoding encoding to use for file names
     * @since 1.4
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format
     * @throws IOException on error
     */
    public TarArchiveEntry(final byte[] headerBuf, final ZipEncoding encoding)
        throws IOException {
        this(headerBuf, encoding, false);
    }

    /**
     * Construct an entry from an archive's header bytes. File is set
     * to null.
     *
     * @param headerBuf The header bytes from a tar archive entry.
     * @param encoding encoding to use for file names
     * @param lenient when set to true illegal values for group/userid, mode, device numbers and timestamp will be
     * ignored and the fields set to {@link #UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @since 1.19
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format
     * @throws IOException on error
     */
    public TarArchiveEntry(final byte[] headerBuf, final ZipEncoding encoding, final boolean lenient)
        throws IOException {
        this(Collections.emptyMap(), headerBuf, encoding, lenient);
    }

    /**
     * Construct an entry from an archive's header bytes for random access tar. File is set to null.
     * @param headerBuf the header bytes from a tar archive entry.
     * @param encoding encoding to use for file names.
     * @param lenient when set to true illegal values for group/userid, mode, device numbers and timestamp will be
     * ignored and the fields set to {@link #UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @param dataOffset position of the entry data in the random access file.
     * @since 1.21
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format.
     * @throws IOException on error.
     */
    public TarArchiveEntry(final byte[] headerBuf, final ZipEncoding encoding, final boolean lenient,
            final long dataOffset) throws IOException {
        this(headerBuf, encoding, lenient);
        setDataOffset(dataOffset);
    }

    /**
     * Construct an entry for a file. File is set to file, and the
     * header is constructed from information from the file.
     * The name is set from the normalized file path.
     *
     * <p>The entry's name will be the value of the {@code file}'s
     * path with all file separators replaced by forward slashes and
     * leading slashes as well as Windows drive letters stripped. The
     * name will end in a slash if the {@code file} represents a
     * directory.</p>
     *
     * <p>Note: Since 1.21 this internally uses the same code as the
     * TarArchiveEntry constructors with a {@link Path} as parameter.
     * But all thrown exceptions are ignored. If handling those
     * exceptions is needed consider switching to the path constructors.</p>
     *
     * @param file The file that the entry represents.
     */
    public TarArchiveEntry(final File file) {
        this(file, file.getPath());
    }

    /**
     * Construct an entry for a file. File is set to file, and the
     * header is constructed from information from the file.
     *
     * <p>The entry's name will be the value of the {@code fileName}
     * argument with all file separators replaced by forward slashes
     * and leading slashes as well as Windows drive letters stripped.
     * The name will end in a slash if the {@code file} represents a
     * directory.</p>
     *
     * <p>Note: Since 1.21 this internally uses the same code as the
     * TarArchiveEntry constructors with a {@link Path} as parameter.
     * But all thrown exceptions are ignored. If handling those
     * exceptions is needed consider switching to the path constructors.</p>
     *
     * @param file The file that the entry represents.
     * @param fileName the name to be used for the entry.
     */
    public TarArchiveEntry(final File file, final String fileName) {
        final String normalizedName = normalizeFileName(fileName, false);
        this.file = file.toPath();
        this.linkOptions = IOUtils.EMPTY_LINK_OPTIONS;

        try {
            readFileMode(this.file, normalizedName);
        } catch (final IOException e) {
            // Ignore exceptions from NIO for backwards compatibility
            // Fallback to get size of file if it's no directory to the old file api
            if (!file.isDirectory()) {
                this.size = file.length();
            }
        }

        this.userName = "";
        try {
            readOsSpecificProperties(this.file);
        } catch (final IOException e) {
            // Ignore exceptions from NIO for backwards compatibility
            // Fallback to get the last modified date of the file from the old file api
            this.mTime = FileTime.fromMillis(file.lastModified());
        }
        preserveAbsolutePath = false;
    }

    /**
     * Construct an entry from an archive's header bytes. File is set to null.
     *
     * @param globalPaxHeaders the parsed global PAX headers, or null if this is the first one.
     * @param headerBuf The header bytes from a tar archive entry.
     * @param encoding encoding to use for file names
     * @param lenient when set to true illegal values for group/userid, mode, device numbers and timestamp will be
     * ignored and the fields set to {@link #UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @since 1.22
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format
     * @throws IOException on error
     */
    public TarArchiveEntry(final Map<String, String> globalPaxHeaders, final byte[] headerBuf,
            final ZipEncoding encoding, final boolean lenient) throws IOException {
        this(false);
        parseTarHeader(globalPaxHeaders, headerBuf, encoding, false, lenient);
    }

    /**
     * Construct an entry from an archive's header bytes for random access tar. File is set to null.
     * @param globalPaxHeaders the parsed global PAX headers, or null if this is the first one.
     * @param headerBuf the header bytes from a tar archive entry.
     * @param encoding encoding to use for file names.
     * @param lenient when set to true illegal values for group/userid, mode, device numbers and timestamp will be
     * ignored and the fields set to {@link #UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @param dataOffset position of the entry data in the random access file.
     * @since 1.22
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format.
     * @throws IOException on error.
     */
    public TarArchiveEntry(final Map<String, String> globalPaxHeaders, final byte[] headerBuf,
            final ZipEncoding encoding, final boolean lenient, final long dataOffset) throws IOException {
        this(globalPaxHeaders,headerBuf, encoding, lenient);
        setDataOffset(dataOffset);
    }

    /**
     * Construct an entry for a file. File is set to file, and the
     * header is constructed from information from the file.
     * The name is set from the normalized file path.
     *
     * <p>The entry's name will be the value of the {@code file}'s
     * path with all file separators replaced by forward slashes and
     * leading slashes as well as Windows drive letters stripped. The
     * name will end in a slash if the {@code file} represents a
     * directory.</p>
     *
     * @param file The file that the entry represents.
     * @throws IOException if an I/O error occurs
     * @since 1.21
     */
    public TarArchiveEntry(final Path file) throws IOException {
        this(file, file.toString());
    }

    /**
     * Construct an entry for a file. File is set to file, and the
     * header is constructed from information from the file.
     *
     * <p>The entry's name will be the value of the {@code fileName}
     * argument with all file separators replaced by forward slashes
     * and leading slashes as well as Windows drive letters stripped.
     * The name will end in a slash if the {@code file} represents a
     * directory.</p>
     *
     * @param file     The file that the entry represents.
     * @param fileName the name to be used for the entry.
     * @param linkOptions options indicating how symbolic links are handled.
     * @throws IOException if an I/O error occurs
     * @since 1.21
     */
    public TarArchiveEntry(final Path file, final String fileName, final LinkOption... linkOptions) throws IOException {
        final String normalizedName = normalizeFileName(fileName, false);
        this.file = file;
        this.linkOptions = linkOptions == null ? IOUtils.EMPTY_LINK_OPTIONS : linkOptions;

        readFileMode(file, normalizedName, linkOptions);

        this.userName = "";
        readOsSpecificProperties(file);
        preserveAbsolutePath = false;
    }

    /**
     * Construct an entry with only a name. This allows the programmer
     * to construct the entry's header "by hand". File is set to null.
     *
     * <p>The entry's name will be the value of the {@code name}
     * argument with all file separators replaced by forward slashes
     * and leading slashes as well as Windows drive letters stripped.</p>
     *
     * @param name the entry name
     */
    public TarArchiveEntry(final String name) {
        this(name, false);
    }

    /**
     * Construct an entry with only a name. This allows the programmer
     * to construct the entry's header "by hand". File is set to null.
     *
     * <p>The entry's name will be the value of the {@code name}
     * argument with all file separators replaced by forward slashes.
     * Leading slashes and Windows drive letters are stripped if
     * {@code preserveAbsolutePath} is {@code false}.</p>
     *
     * @param name the entry name
     * @param preserveAbsolutePath whether to allow leading slashes
     * or drive letters in the name.
     *
     * @since 1.1
     */
    public TarArchiveEntry(String name, final boolean preserveAbsolutePath) {
        this(preserveAbsolutePath);

        name = normalizeFileName(name, preserveAbsolutePath);
        final boolean isDir = name.endsWith("/");

        this.name = name;
        this.mode = isDir ? DEFAULT_DIR_MODE : DEFAULT_FILE_MODE;
        this.linkFlag = isDir ? LF_DIR : LF_NORMAL;
        this.mTime = FileTime.from(Instant.now());
        this.userName = "";
    }

    /**
     * Construct an entry with a name and a link flag.
     *
     * <p>The entry's name will be the value of the {@code name}
     * argument with all file separators replaced by forward slashes
     * and leading slashes as well as Windows drive letters
     * stripped.</p>
     *
     * @param name the entry name
     * @param linkFlag the entry link flag.
     */
    public TarArchiveEntry(final String name, final byte linkFlag) {
        this(name, linkFlag, false);
    }

    /**
     * Construct an entry with a name and a link flag.
     *
     * <p>The entry's name will be the value of the {@code name}
     * argument with all file separators replaced by forward slashes.
     * Leading slashes and Windows drive letters are stripped if
     * {@code preserveAbsolutePath} is {@code false}.</p>
     *
     * @param name the entry name
     * @param linkFlag the entry link flag.
     * @param preserveAbsolutePath whether to allow leading slashes
     * or drive letters in the name.
     *
     * @since 1.5
     */
    public TarArchiveEntry(final String name, final byte linkFlag, final boolean preserveAbsolutePath) {
        this(name, preserveAbsolutePath);
        this.linkFlag = linkFlag;
        if (linkFlag == LF_GNUTYPE_LONGNAME) {
            magic = MAGIC_GNU;
            version = VERSION_GNU_SPACE;
        }
    }

    /**
     * add a PAX header to this entry. If the header corresponds to an existing field in the entry,
     * that field will be set; otherwise the header will be added to the extraPaxHeaders Map
     * @param name  The full name of the header to set.
     * @param value value of header.
     * @since 1.15
     */
    public void addPaxHeader(final String name, final String value) {
        try {
            processPaxHeader(name,value);
        } catch (final IOException ex) {
            throw new IllegalArgumentException("Invalid input", ex);
        }
    }

    /**
     * clear all extra PAX headers.
     * @since 1.15
     */
    public void clearExtraPaxHeaders() {
        extraPaxHeaders.clear();
    }

    /**
     * Determine if the two entries are equal. Equality is determined
     * by the header names being equal.
     *
     * @param it Entry to be checked for equality.
     * @return True if the entries are equal.
     */
    @Override
    public boolean equals(final Object it) {
        if (it == null || getClass() != it.getClass()) {
            return false;
        }
        return equals((TarArchiveEntry) it);
    }

    /**
     * Determine if the two entries are equal. Equality is determined
     * by the header names being equal.
     *
     * @param it Entry to be checked for equality.
     * @return True if the entries are equal.
     */
    public boolean equals(final TarArchiveEntry it) {
        return it != null && getName().equals(it.getName());
    }

    /**
     * Evaluate an entry's header format from a header buffer.
     *
     * @param header The tar entry header buffer to evaluate the format for.
     * @return format type
     */
    private int evaluateType(final Map<String, String> globalPaxHeaders, final byte[] header) {
        if (ArchiveUtils.matchAsciiBuffer(MAGIC_GNU, header, MAGIC_OFFSET, MAGICLEN)) {
            return FORMAT_OLDGNU;
        }
        if (ArchiveUtils.matchAsciiBuffer(MAGIC_POSIX, header, MAGIC_OFFSET, MAGICLEN)) {
            if (isXstar(globalPaxHeaders, header)) {
                return FORMAT_XSTAR;
            }
            return FORMAT_POSIX;
        }
        return 0;
    }

    private int fill(final byte value, final int offset, final byte[] outbuf, final int length) {
        for (int i = 0; i < length; i++) {
            outbuf[offset + i] = value;
        }
        return offset + length;
    }

    private int fill(final int value, final int offset, final byte[] outbuf, final int length) {
        return fill((byte) value, offset, outbuf, length);
    }

    void fillGNUSparse0xData(final Map<String, String> headers) {
        paxGNUSparse = true;
        realSize = Integer.parseInt(headers.get(TarGnuSparseKeys.SIZE));
        if (headers.containsKey(TarGnuSparseKeys.NAME)) {
            // version 0.1
            name = headers.get(TarGnuSparseKeys.NAME);
        }
    }

    void fillGNUSparse1xData(final Map<String, String> headers) throws IOException {
        paxGNUSparse = true;
        paxGNU1XSparse = true;
        if (headers.containsKey(TarGnuSparseKeys.NAME)) {
            name = headers.get(TarGnuSparseKeys.NAME);
        }
        if (headers.containsKey(TarGnuSparseKeys.REALSIZE)) {
            try {
                realSize = Integer.parseInt(headers.get(TarGnuSparseKeys.REALSIZE));
            } catch (final NumberFormatException ex) {
                throw new IOException("Corrupted TAR archive. GNU.sparse.realsize header for "
                    + name + " contains non-numeric value");
            }
        }
    }

    void fillStarSparseData(final Map<String, String> headers) throws IOException {
        starSparse = true;
        if (headers.containsKey("SCHILY.realsize")) {
            try {
                realSize = Long.parseLong(headers.get("SCHILY.realsize"));
            } catch (final NumberFormatException ex) {
                throw new IOException("Corrupted TAR archive. SCHILY.realsize header for "
                    + name + " contains non-numeric value");
            }
        }
    }

    /**
     * Get this entry's creation time.
     *
     * @since 1.22
     * @return This entry's computed creation time.
     */
    public FileTime getCreationTime() {
        return birthTime;
    }

    /**
     * {@inheritDoc}
     * @since 1.21
     */
    @Override
    public long getDataOffset() {
        return dataOffset;
    }

    /**
     * Get this entry's major device number.
     *
     * @return This entry's major device number.
     * @since 1.4
     */
    public int getDevMajor() {
        return devMajor;
    }

    /**
     * Get this entry's minor device number.
     *
     * @return This entry's minor device number.
     * @since 1.4
     */
    public int getDevMinor() {
        return devMinor;
    }

    /**
     * If this entry represents a file, and the file is a directory, return
     * an array of TarEntries for this entry's children.
     *
     * <p>This method is only useful for entries created from a {@code
     * File} or {@code Path} but not for entries read from an archive.</p>
     *
     * @return An array of TarEntry's for this entry's children.
     */
    public TarArchiveEntry[] getDirectoryEntries() {
        if (file == null || !isDirectory()) {
            return EMPTY_TAR_ARCHIVE_ENTRY_ARRAY;
        }

        final List<TarArchiveEntry> entries = new ArrayList<>();
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(file)) {
            for (final Path p : dirStream) {
                entries.add(new TarArchiveEntry(p));
            }
        } catch (final IOException e) {
            return EMPTY_TAR_ARCHIVE_ENTRY_ARRAY;
        }
        return entries.toArray(EMPTY_TAR_ARCHIVE_ENTRY_ARRAY);
    }

    /**
     * get named extra PAX header
     * @param name The full name of an extended PAX header to retrieve
     * @return The value of the header, if any.
     * @since 1.15
     */
    public String getExtraPaxHeader(final String name) {
        return extraPaxHeaders.get(name);
    }

    /**
     * get extra PAX Headers
     * @return read-only map containing any extra PAX Headers
     * @since 1.15
     */
    public Map<String, String> getExtraPaxHeaders() {
        return Collections.unmodifiableMap(extraPaxHeaders);
    }

    /**
     * Get this entry's file.
     *
     * <p>This method is only useful for entries created from a {@code
     * File} or {@code Path} but not for entries read from an archive.</p>
     *
     * @return this entry's file or null if the entry was not created from a file.
     */
    public File getFile() {
        if (file == null) {
            return null;
        }
        return file.toFile();
    }

    /**
     * Get this entry's group id.
     *
     * @return This entry's group id.
     * @deprecated use #getLongGroupId instead as group ids can be
     * bigger than {@link Integer#MAX_VALUE}
     */
    @Deprecated
    public int getGroupId() {
        return (int) (groupId & 0xffffffff);
    }

    /**
     * Get this entry's group name.
     *
     * @return This entry's group name.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Get this entry's last access time.
     *
     * @since 1.22
     * @return This entry's last access time.
     */
    public FileTime getLastAccessTime() {
        return aTime;
    }

    /**
     * Get this entry's modification time.
     * This is equivalent to {@link TarArchiveEntry#getLastModifiedTime()}, but precision is truncated to milliseconds.
     *
     * @return This entry's modification time.
     * @see TarArchiveEntry#getLastModifiedTime()
     */
    @Override
    public Date getLastModifiedDate() {
        return getModTime();
    }

    /**
     * Get this entry's modification time.
     *
     * @since 1.22
     * @return This entry's modification time.
     */
    public FileTime getLastModifiedTime() {
        return mTime;
    }

    /**
     * Get this entry's link name.
     *
     * @return This entry's link name.
     */
    public String getLinkName() {
        return linkName;
    }

    /**
     * Get this entry's group id.
     *
     * @since 1.10
     * @return This entry's group id.
     */
    public long getLongGroupId() {
        return groupId;
    }

    /**
     * Get this entry's user id.
     *
     * @return This entry's user id.
     * @since 1.10
     */
    public long getLongUserId() {
        return userId;
    }

    /**
     * Get this entry's mode.
     *
     * @return This entry's mode.
     */
    public int getMode() {
        return mode;
    }

    /**
     * Get this entry's modification time.
     * This is equivalent to {@link TarArchiveEntry#getLastModifiedTime()}, but precision is truncated to milliseconds.
     *
     * @return This entry's modification time.
     * @see TarArchiveEntry#getLastModifiedTime()
     */
    public Date getModTime() {
        return TimeUtils.toDate(mTime);
    }

    /**
     * Get this entry's name.
     *
     * <p>This method returns the raw name as it is stored inside of the archive.</p>
     *
     * @return This entry's name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get this entry's sparse headers ordered by offset with all empty sparse sections at the start filtered out.
     *
     * @return immutable list of this entry's sparse headers, never null
     * @since 1.21
     * @throws IOException if the list of sparse headers contains blocks that overlap
     */
    public List<TarArchiveStructSparse> getOrderedSparseHeaders() throws IOException {
        if (sparseHeaders == null || sparseHeaders.isEmpty()) {
            return Collections.emptyList();
        }
        final List<TarArchiveStructSparse> orderedAndFiltered = sparseHeaders.stream()
            .filter(s -> s.getOffset() > 0 || s.getNumbytes() > 0)
            .sorted(Comparator.comparingLong(TarArchiveStructSparse::getOffset))
            .collect(Collectors.toList());

        final int numberOfHeaders = orderedAndFiltered.size();
        for (int i = 0; i < numberOfHeaders; i++) {
            final TarArchiveStructSparse str = orderedAndFiltered.get(i);
            if (i + 1 < numberOfHeaders
                && str.getOffset() + str.getNumbytes() > orderedAndFiltered.get(i + 1).getOffset()) {
                throw new IOException("Corrupted TAR archive. Sparse blocks for "
                    + getName() + " overlap each other.");
            }
            if (str.getOffset() + str.getNumbytes() < 0) {
                // integer overflow?
                throw new IOException("Unreadable TAR archive. Offset and numbytes for sparse block in "
                    + getName() + " too large.");
            }
        }
        if (!orderedAndFiltered.isEmpty()) {
            final TarArchiveStructSparse last = orderedAndFiltered.get(numberOfHeaders - 1);
            if (last.getOffset() + last.getNumbytes() > getRealSize()) {
                throw new IOException("Corrupted TAR archive. Sparse block extends beyond real size of the entry");
            }
        }

        return orderedAndFiltered;
    }

    /**
     * Get this entry's file.
     *
     * <p>This method is only useful for entries created from a {@code
     * File} or {@code Path} but not for entries read from an archive.</p>
     *
     * @return this entry's file or null if the entry was not created from a file.
     * @since 1.21
     */
    public Path getPath() {
        return file;
    }

    /**
     * Get this entry's real file size in case of a sparse file.
     *
     * <p>This is the size a file would take on disk if the entry was expanded.</p>
     *
     * <p>If the file is not a sparse file, return size instead of realSize.</p>
     *
     * @return This entry's real file size, if the file is not a sparse file, return size instead of realSize.
     */
    public long getRealSize() {
        if (!isSparse()) {
            return getSize();
        }
        return realSize;
    }

    /**
     * Get this entry's file size.
     *
     * <p>This is the size the entry's data uses inside of the archive. Usually this is the same as {@link
     * #getRealSize}, but it doesn't take the "holes" into account when the entry represents a sparse file.
     *
     * @return This entry's file size.
     */
    @Override
    public long getSize() {
        return size;
    }

    /**
     * Get this entry's sparse headers
     *
     * @return This entry's sparse headers
     * @since 1.20
     */
    public List<TarArchiveStructSparse> getSparseHeaders() {
        return sparseHeaders;
    }

    /**
     * Get this entry's status change time.
     *
     * @since 1.22
     * @return This entry's status change time.
     */
    public FileTime getStatusChangeTime() {
        return cTime;
    }

    /**
     * Get this entry's user id.
     *
     * @return This entry's user id.
     * @deprecated use #getLongUserId instead as user ids can be
     * bigger than {@link Integer#MAX_VALUE}
     */
    @Deprecated
    public int getUserId() {
        return (int) (userId & 0xffffffff);
    }

    /**
     * Get this entry's user name.
     *
     * @return This entry's user name.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Hashcodes are based on entry names.
     *
     * @return the entry hash code
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Check if this is a block device entry.
     *
     * @since 1.2
     * @return whether this is a block device
     */
    public boolean isBlockDevice() {
        return linkFlag == LF_BLK;
    }

    /**
     * Check if this is a character device entry.
     *
     * @since 1.2
     * @return whether this is a character device
     */
    public boolean isCharacterDevice() {
        return linkFlag == LF_CHR;
    }

    /**
     * Get this entry's checksum status.
     *
     * @return if the header checksum is reasonably correct
     * @see TarUtils#verifyCheckSum(byte[])
     * @since 1.5
     */
    public boolean isCheckSumOK() {
        return checkSumOK;
    }

    /**
     * Determine if the given entry is a descendant of this entry.
     * Descendancy is determined by the name of the descendant
     * starting with this entry's name.
     *
     * @param desc Entry to be checked as a descendent of this.
     * @return True if entry is a descendant of this.
     */
    public boolean isDescendent(final TarArchiveEntry desc) {
        return desc.getName().startsWith(getName());
    }

    /**
     * Return whether or not this entry represents a directory.
     *
     * @return True if this entry is a directory.
     */
    @Override
    public boolean isDirectory() {
        if (file != null) {
            return Files.isDirectory(file, linkOptions);
        }

        if (linkFlag == LF_DIR) {
            return true;
        }

        return !isPaxHeader() && !isGlobalPaxHeader() && getName().endsWith("/");
    }

    /**
     * Indicates in case of an oldgnu sparse file if an extension
     * sparse header follows.
     *
     * @return true if an extension oldgnu sparse header follows.
     */
    public boolean isExtended() {
        return isExtended;
    }

    /**
     * Check if this is a FIFO (pipe) entry.
     *
     * @since 1.2
     * @return whether this is a FIFO entry
     */
    public boolean isFIFO() {
        return linkFlag == LF_FIFO;
    }

    /**
     * Check if this is a "normal file"
     *
     * @since 1.2
     * @return whether this is a "normal file"
     */
    public boolean isFile() {
        if (file != null) {
            return Files.isRegularFile(file, linkOptions);
        }
        if (linkFlag == LF_OLDNORM || linkFlag == LF_NORMAL) {
            return true;
        }
        return !getName().endsWith("/");
    }

    /**
     * Check if this is a Pax header.
     *
     * @return {@code true} if this is a Pax header.
     *
     * @since 1.1
     */
    public boolean isGlobalPaxHeader() {
        return linkFlag == LF_PAX_GLOBAL_EXTENDED_HEADER;
    }

    /**
     * Indicate if this entry is a GNU long linkname block
     *
     * @return true if this is a long name extension provided by GNU tar
     */
    public boolean isGNULongLinkEntry() {
        return linkFlag == LF_GNUTYPE_LONGLINK;
    }

    /**
     * Indicate if this entry is a GNU long name block
     *
     * @return true if this is a long name extension provided by GNU tar
     */
    public boolean isGNULongNameEntry() {
        return linkFlag == LF_GNUTYPE_LONGNAME;
    }

    /**
     * Indicate if this entry is a GNU sparse block.
     *
     * @return true if this is a sparse extension provided by GNU tar
     */
    public boolean isGNUSparse() {
        return isOldGNUSparse() || isPaxGNUSparse();
    }

    private boolean isInvalidPrefix(final byte[] header) {
        // prefix[130] is is guaranteed to be '\0' with XSTAR/XUSTAR
        if (header[XSTAR_PREFIX_OFFSET + 130] != 0) {
            // except when typeflag is 'M'
            if (header[LF_OFFSET] != LF_MULTIVOLUME) {
                return true;
            }
            // We come only here if we try to read in a GNU/xstar/xustar multivolume archive starting past volume #0
            // As of 1.22, commons-compress does not support multivolume tar archives.
            // If/when it does, this should work as intended.
            if ((header[XSTAR_MULTIVOLUME_OFFSET] & 0x80) == 0
                    && header[XSTAR_MULTIVOLUME_OFFSET + 11] != ' ') {
                return true;
            }
        }
        return false;
    }

    private boolean isInvalidXtarTime(final byte[] buffer, final int offset, final int length) {
        // If atime[0]...atime[10] or ctime[0]...ctime[10] is not a POSIX octal number it cannot be 'xstar'.
        if ((buffer[offset] & 0x80) == 0) {
            final int lastIndex = length - 1;
            for (int i = 0; i < lastIndex; i++) {
                final byte b = buffer[offset + i];
                if (b < '0' || b > '7') {
                    return true;
                }
            }
            // Check for both POSIX compliant end of number characters if not using base 256
            final byte b = buffer[offset + lastIndex];
            if (b != ' ' && b != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this is a link entry.
     *
     * @since 1.2
     * @return whether this is a link entry
     */
    public boolean isLink() {
        return linkFlag == LF_LINK;
    }

    /**
     * Indicate if this entry is a GNU or star sparse block using the
     * oldgnu format.
     *
     * @return true if this is a sparse extension provided by GNU tar or star
     * @since 1.11
     */
    public boolean isOldGNUSparse() {
        return linkFlag == LF_GNUTYPE_SPARSE;
    }

    /**
     * Get if this entry is a sparse file with 1.X PAX Format or not
     *
     * @return True if this entry is a sparse file with 1.X PAX Format
     * @since 1.20
     */
    public boolean isPaxGNU1XSparse() {
        return paxGNU1XSparse;
    }

    /**
     * Indicate if this entry is a GNU sparse block using one of the
     * PAX formats.
     *
     * @return true if this is a sparse extension provided by GNU tar
     * @since 1.11
     */
    public boolean isPaxGNUSparse() {
        return paxGNUSparse;
    }

    /**
     * Check if this is a Pax header.
     *
     * @return {@code true} if this is a Pax header.
     *
     * @since 1.1
     *
     */
    public boolean isPaxHeader() {
        return linkFlag == LF_PAX_EXTENDED_HEADER_LC
            || linkFlag == LF_PAX_EXTENDED_HEADER_UC;
    }

    /**
     * Check whether this is a sparse entry.
     *
     * @return whether this is a sparse entry
     * @since 1.11
     */
    public boolean isSparse() {
        return isGNUSparse() || isStarSparse();
    }

    /**
     * Indicate if this entry is a star sparse block using PAX headers.
     *
     * @return true if this is a sparse extension provided by star
     * @since 1.11
     */
    public boolean isStarSparse() {
        return starSparse;
    }

    /**
     * {@inheritDoc}
     * @since 1.21
     */
    @Override
    public boolean isStreamContiguous() {
        return true;
    }

    /**
     * Check if this is a symbolic link entry.
     *
     * @since 1.2
     * @return whether this is a symbolic link
     */
    public boolean isSymbolicLink() {
        return linkFlag == LF_SYMLINK;
    }

    /**
     * Check for XSTAR / XUSTAR format.
     *
     * Use the same logic found in star version 1.6 in {@code header.c}, function {@code isxmagic(TCB *ptb)}.
     */
    private boolean isXstar(final Map<String, String> globalPaxHeaders, final byte[] header) {
        // Check if this is XSTAR
        if (ArchiveUtils.matchAsciiBuffer(MAGIC_XSTAR, header, XSTAR_MAGIC_OFFSET, XSTAR_MAGIC_LEN)) {
            return true;
        }

        /*
        If SCHILY.archtype is present in the global PAX header, we can use it to identify the type of archive.

        Possible values for XSTAR:
        - xustar: 'xstar' format without "tar" signature at header offset 508.
        - exustar: 'xustar' format variant that always includes x-headers and g-headers.
         */
        final String archType = globalPaxHeaders.get("SCHILY.archtype");
        if (archType != null) {
            return "xustar".equals(archType) || "exustar".equals(archType);
        }

        // Check if this is XUSTAR
        if (isInvalidPrefix(header)) {
            return false;
        }
        if (isInvalidXtarTime(header, XSTAR_ATIME_OFFSET, ATIMELEN_XSTAR)) {
            return false;
        }
        if (isInvalidXtarTime(header, XSTAR_CTIME_OFFSET, CTIMELEN_XSTAR)) {
            return false;
        }

        return true;
    }

    private long parseOctalOrBinary(final byte[] header, final int offset, final int length, final boolean lenient) {
        if (lenient) {
            try {
                return TarUtils.parseOctalOrBinary(header, offset, length);
            } catch (final IllegalArgumentException ex) { //NOSONAR
                return UNKNOWN;
            }
        }
        return TarUtils.parseOctalOrBinary(header, offset, length);
    }

    /**
     * Parse an entry's header information from a header buffer.
     *
     * @param header The tar entry header buffer to get information from.
     * @throws IllegalArgumentException if any of the numeric fields have an invalid format
     */
    public void parseTarHeader(final byte[] header) {
        try {
            parseTarHeader(header, TarUtils.DEFAULT_ENCODING);
        } catch (final IOException ex) { // NOSONAR
            try {
                parseTarHeader(header, TarUtils.DEFAULT_ENCODING, true, false);
            } catch (final IOException ex2) {
                // not really possible
                throw new UncheckedIOException(ex2); //NOSONAR
            }
        }
    }

    /**
     * Parse an entry's header information from a header buffer.
     *
     * @param header The tar entry header buffer to get information from.
     * @param encoding encoding to use for file names
     * @since 1.4
     * @throws IllegalArgumentException if any of the numeric fields
     * have an invalid format
     * @throws IOException on error
     */
    public void parseTarHeader(final byte[] header, final ZipEncoding encoding)
        throws IOException {
        parseTarHeader(header, encoding, false, false);
    }

    private void parseTarHeader(final byte[] header, final ZipEncoding encoding,
                                final boolean oldStyle, final boolean lenient)
        throws IOException {
        parseTarHeader(Collections.emptyMap(), header, encoding, oldStyle, lenient);
    }

    private void parseTarHeader(final Map<String, String> globalPaxHeaders, final byte[] header,
                                final ZipEncoding encoding, final boolean oldStyle, final boolean lenient)
        throws IOException {
        try {
            parseTarHeaderUnwrapped(globalPaxHeaders, header, encoding, oldStyle, lenient);
        } catch (final IllegalArgumentException ex) {
            throw new IOException("Corrupted TAR archive.", ex);
        }
    }

    private void parseTarHeaderUnwrapped(final Map<String, String> globalPaxHeaders, final byte[] header,
                                         final ZipEncoding encoding, final boolean oldStyle, final boolean lenient)
        throws IOException {
        int offset = 0;

        name = oldStyle ? TarUtils.parseName(header, offset, NAMELEN)
            : TarUtils.parseName(header, offset, NAMELEN, encoding);
        offset += NAMELEN;
        mode = (int) parseOctalOrBinary(header, offset, MODELEN, lenient);
        offset += MODELEN;
        userId = (int) parseOctalOrBinary(header, offset, UIDLEN, lenient);
        offset += UIDLEN;
        groupId = (int) parseOctalOrBinary(header, offset, GIDLEN, lenient);
        offset += GIDLEN;
        size = TarUtils.parseOctalOrBinary(header, offset, SIZELEN);
        if (size < 0) {
            throw new IOException("broken archive, entry with negative size");
        }
        offset += SIZELEN;
        mTime = TimeUtils.unixTimeToFileTime(parseOctalOrBinary(header, offset, MODTIMELEN, lenient));
        offset += MODTIMELEN;
        checkSumOK = TarUtils.verifyCheckSum(header);
        offset += CHKSUMLEN;
        linkFlag = header[offset++];
        linkName = oldStyle ? TarUtils.parseName(header, offset, NAMELEN)
            : TarUtils.parseName(header, offset, NAMELEN, encoding);
        offset += NAMELEN;
        magic = TarUtils.parseName(header, offset, MAGICLEN);
        offset += MAGICLEN;
        version = TarUtils.parseName(header, offset, VERSIONLEN);
        offset += VERSIONLEN;
        userName = oldStyle ? TarUtils.parseName(header, offset, UNAMELEN)
            : TarUtils.parseName(header, offset, UNAMELEN, encoding);
        offset += UNAMELEN;
        groupName = oldStyle ? TarUtils.parseName(header, offset, GNAMELEN)
            : TarUtils.parseName(header, offset, GNAMELEN, encoding);
        offset += GNAMELEN;
        if (linkFlag == LF_CHR || linkFlag == LF_BLK) {
            devMajor = (int) parseOctalOrBinary(header, offset, DEVLEN, lenient);
            offset += DEVLEN;
            devMinor = (int) parseOctalOrBinary(header, offset, DEVLEN, lenient);
            offset += DEVLEN;
        } else {
            offset += 2 * DEVLEN;
        }

        final int type = evaluateType(globalPaxHeaders, header);
        switch (type) {
        case FORMAT_OLDGNU: {
            aTime = fileTimeFromOptionalSeconds(parseOctalOrBinary(header, offset, ATIMELEN_GNU, lenient));
            offset += ATIMELEN_GNU;
            cTime = fileTimeFromOptionalSeconds(parseOctalOrBinary(header, offset, CTIMELEN_GNU, lenient));
            offset += CTIMELEN_GNU;
            offset += OFFSETLEN_GNU;
            offset += LONGNAMESLEN_GNU;
            offset += PAD2LEN_GNU;
            sparseHeaders =
                new ArrayList<>(TarUtils.readSparseStructs(header, offset, SPARSE_HEADERS_IN_OLDGNU_HEADER));
            offset += SPARSELEN_GNU;
            isExtended = TarUtils.parseBoolean(header, offset);
            offset += ISEXTENDEDLEN_GNU;
            realSize = TarUtils.parseOctal(header, offset, REALSIZELEN_GNU);
            offset += REALSIZELEN_GNU; // NOSONAR - assignment as documentation
            break;
        }
        case FORMAT_XSTAR: {
            final String xstarPrefix = oldStyle
                ? TarUtils.parseName(header, offset, PREFIXLEN_XSTAR)
                : TarUtils.parseName(header, offset, PREFIXLEN_XSTAR, encoding);
            offset += PREFIXLEN_XSTAR;
            if (!xstarPrefix.isEmpty()) {
                name = xstarPrefix + "/" + name;
            }
            aTime = fileTimeFromOptionalSeconds(parseOctalOrBinary(header, offset, ATIMELEN_XSTAR, lenient));
            offset += ATIMELEN_XSTAR;
            cTime = fileTimeFromOptionalSeconds(parseOctalOrBinary(header, offset, CTIMELEN_XSTAR, lenient));
            offset += CTIMELEN_XSTAR; // NOSONAR - assignment as documentation
            break;
        }
        case FORMAT_POSIX:
        default: {
            final String prefix = oldStyle
                ? TarUtils.parseName(header, offset, PREFIXLEN)
                : TarUtils.parseName(header, offset, PREFIXLEN, encoding);
            offset += PREFIXLEN; // NOSONAR - assignment as documentation
            // SunOS tar -E does not add / to directory names, so fix
            // up to be consistent
            if (isDirectory() && !name.endsWith("/")){
                name = name + "/";
            }
            if (!prefix.isEmpty()){
                name = prefix + "/" + name;
            }
        }
        }
    }

    /**
     * process one pax header, using the entries extraPaxHeaders map as source for extra headers
     * used when handling entries for sparse files.
     * @param key
     * @param val
     * @since 1.15
     */
    private void processPaxHeader(final String key, final String val) throws IOException {
        processPaxHeader(key, val, extraPaxHeaders);
    }

    /**
     * Process one pax header, using the supplied map as source for extra headers to be used when handling
     * entries for sparse files
     *
     * @param key  the header name.
     * @param val  the header value.
     * @param headers  map of headers used for dealing with sparse file.
     * @throws NumberFormatException  if encountered errors when parsing the numbers
     * @since 1.15
     */
    private void processPaxHeader(final String key, final String val, final Map<String, String> headers)
        throws IOException {
    /*
     * The following headers are defined for Pax.
     * charset: cannot use these without changing TarArchiveEntry fields
     * mtime
     * atime
     * ctime
     * LIBARCHIVE.creationtime
     * comment
     * gid, gname
     * linkpath
     * size
     * uid,uname
     * SCHILY.devminor, SCHILY.devmajor: don't have setters/getters for those
     *
     * GNU sparse files use additional members, we use
     * GNU.sparse.size to detect the 0.0 and 0.1 versions and
     * GNU.sparse.realsize for 1.0.
     *
     * star files use additional members of which we use
     * SCHILY.filetype in order to detect star sparse files.
     *
     * If called from addExtraPaxHeader, these additional headers must be already present .
     */
        switch (key) {
            case "path":
                setName(val);
                break;
            case "linkpath":
                setLinkName(val);
                break;
            case "gid":
                setGroupId(Long.parseLong(val));
                break;
            case "gname":
                setGroupName(val);
                break;
            case "uid":
                setUserId(Long.parseLong(val));
                break;
            case "uname":
                setUserName(val);
                break;
            case "size":
                final long size = Long.parseLong(val);
                if (size < 0) {
                    throw new IOException("Corrupted TAR archive. Entry size is negative");
                }
                setSize(size);
                break;
            case "mtime":
                setLastModifiedTime(FileTime.from(parseInstantFromDecimalSeconds(val)));
                break;
            case "atime":
                setLastAccessTime(FileTime.from(parseInstantFromDecimalSeconds(val)));
                break;
            case "ctime":
                setStatusChangeTime(FileTime.from(parseInstantFromDecimalSeconds(val)));
                break;
            case "LIBARCHIVE.creationtime":
                setCreationTime(FileTime.from(parseInstantFromDecimalSeconds(val)));
                break;
            case "SCHILY.devminor":
                final int devMinor = Integer.parseInt(val);
                if (devMinor < 0) {
                    throw new IOException("Corrupted TAR archive. Dev-Minor is negative");
                }
                setDevMinor(devMinor);
                break;
            case "SCHILY.devmajor":
                final int devMajor = Integer.parseInt(val);
                if (devMajor < 0) {
                    throw new IOException("Corrupted TAR archive. Dev-Major is negative");
                }
                setDevMajor(devMajor);
                break;
            case TarGnuSparseKeys.SIZE:
                fillGNUSparse0xData(headers);
                break;
            case TarGnuSparseKeys.REALSIZE:
                fillGNUSparse1xData(headers);
                break;
            case "SCHILY.filetype":
                if ("sparse".equals(val)) {
                    fillStarSparseData(headers);
                }
                break;
            default:
                extraPaxHeaders.put(key,val);
        }
    }

    private void readFileMode(final Path file, final String normalizedName, final LinkOption... options) throws IOException {
        if (Files.isDirectory(file, options)) {
            this.mode = DEFAULT_DIR_MODE;
            this.linkFlag = LF_DIR;

            final int nameLength = normalizedName.length();
            if (nameLength == 0 || normalizedName.charAt(nameLength - 1) != '/') {
                this.name = normalizedName + "/";
            } else {
                this.name = normalizedName;
            }
        } else {
            this.mode = DEFAULT_FILE_MODE;
            this.linkFlag = LF_NORMAL;
            this.name = normalizedName;
            this.size = Files.size(file);
        }
    }

    private void readOsSpecificProperties(final Path file, final LinkOption... options) throws IOException {
        final Set<String> availableAttributeViews = file.getFileSystem().supportedFileAttributeViews();
        if (availableAttributeViews.contains("posix")) {
            final PosixFileAttributes posixFileAttributes = Files.readAttributes(file, PosixFileAttributes.class, options);
            setLastModifiedTime(posixFileAttributes.lastModifiedTime());
            setCreationTime(posixFileAttributes.creationTime());
            setLastAccessTime(posixFileAttributes.lastAccessTime());
            this.userName = posixFileAttributes.owner().getName();
            this.groupName = posixFileAttributes.group().getName();
            if (availableAttributeViews.contains("unix")) {
                this.userId = ((Number) Files.getAttribute(file, "unix:uid", options)).longValue();
                this.groupId = ((Number) Files.getAttribute(file, "unix:gid", options)).longValue();
                try {
                    setStatusChangeTime((FileTime) Files.getAttribute(file, "unix:ctime", options));
                } catch (final IllegalArgumentException ex) { // NOSONAR
                    // ctime is not supported
                }
            }
        } else if (availableAttributeViews.contains("dos")) {
            final DosFileAttributes dosFileAttributes = Files.readAttributes(file, DosFileAttributes.class, options);
            setLastModifiedTime(dosFileAttributes.lastModifiedTime());
            setCreationTime(dosFileAttributes.creationTime());
            setLastAccessTime(dosFileAttributes.lastAccessTime());
            this.userName = Files.getOwner(file, options).getName();
        } else {
            final BasicFileAttributes basicFileAttributes = Files.readAttributes(file, BasicFileAttributes.class, options);
            setLastModifiedTime(basicFileAttributes.lastModifiedTime());
            setCreationTime(basicFileAttributes.creationTime());
            setLastAccessTime(basicFileAttributes.lastAccessTime());
            this.userName = Files.getOwner(file, options).getName();
        }
    }

    /**
     * Set this entry's creation time.
     *
     * @param time This entry's new creation time.
     * @since 1.22
     */
    public void setCreationTime(final FileTime time) {
        birthTime = time;
    }

    /**
     * Set the offset of the data for the tar entry.
     * @param dataOffset the position of the data in the tar.
     * @since 1.21
     */
    public void setDataOffset(final long dataOffset) {
        if (dataOffset < 0) {
            throw new IllegalArgumentException("The offset can not be smaller than 0");
        }
        this.dataOffset = dataOffset;
    }

    /**
     * Set this entry's major device number.
     *
     * @param devNo This entry's major device number.
     * @throws IllegalArgumentException if the devNo is &lt; 0.
     * @since 1.4
     */
    public void setDevMajor(final int devNo) {
        if (devNo < 0){
            throw new IllegalArgumentException("Major device number is out of "
                                               + "range: " + devNo);
        }
        this.devMajor = devNo;
    }

    /**
     * Set this entry's minor device number.
     *
     * @param devNo This entry's minor device number.
     * @throws IllegalArgumentException if the devNo is &lt; 0.
     * @since 1.4
     */
    public void setDevMinor(final int devNo) {
        if (devNo < 0){
            throw new IllegalArgumentException("Minor device number is out of "
                                               + "range: " + devNo);
        }
        this.devMinor = devNo;
    }

    /**
     * Set this entry's group id.
     *
     * @param groupId This entry's new group id.
     */
    public void setGroupId(final int groupId) {
        setGroupId((long) groupId);
    }

    /**
     * Set this entry's group id.
     *
     * @since 1.10
     * @param groupId This entry's new group id.
     */
    public void setGroupId(final long groupId) {
        this.groupId = groupId;
    }

    /**
     * Set this entry's group name.
     *
     * @param groupName This entry's new group name.
     */
    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    /**
     * Convenience method to set this entry's group and user ids.
     *
     * @param userId This entry's new user id.
     * @param groupId This entry's new group id.
     */
    public void setIds(final int userId, final int groupId) {
        setUserId(userId);
        setGroupId(groupId);
    }

    /**
     * Set this entry's last access time.
     *
     * @param time This entry's new last access time.
     * @since 1.22
     */
    public void setLastAccessTime(final FileTime time) {
        aTime = time;
    }

    /**
     * Set this entry's modification time.
     *
     * @param time This entry's new modification time.
     * @since 1.22
     */
    public void setLastModifiedTime(final FileTime time) {
        mTime = Objects.requireNonNull(time, "Time must not be null");
    }

    /**
     * Set this entry's link name.
     *
     * @param link the link name to use.
     *
     * @since 1.1
     */
    public void setLinkName(final String link) {
        this.linkName = link;
    }

    /**
     * Set the mode for this entry
     *
     * @param mode the mode for this entry
     */
    public void setMode(final int mode) {
        this.mode = mode;
    }

    /**
     * Set this entry's modification time.
     *
     * @param time This entry's new modification time.
     * @see TarArchiveEntry#setLastModifiedTime(FileTime)
     */
    public void setModTime(final Date time) {
        setLastModifiedTime(TimeUtils.toFileTime(time));
    }

    /**
     * Set this entry's modification time.
     *
     * @param time This entry's new modification time.
     * @since 1.21
     * @see TarArchiveEntry#setLastModifiedTime(FileTime)
     */
    public void setModTime(final FileTime time) {
        setLastModifiedTime(time);
    }

    /**
     * Set this entry's modification time. The parameter passed
     * to this method is in "Java time".
     *
     * @param time This entry's new modification time.
     * @see TarArchiveEntry#setLastModifiedTime(FileTime)
     */
    public void setModTime(final long time) {
        setLastModifiedTime(FileTime.fromMillis(time));
    }

    /**
     * Set this entry's name.
     *
     * @param name This entry's new name.
     */
    public void setName(final String name) {
        this.name = normalizeFileName(name, this.preserveAbsolutePath);
    }

    /**
     * Convenience method to set this entry's group and user names.
     *
     * @param userName This entry's new user name.
     * @param groupName This entry's new group name.
     */
    public void setNames(final String userName, final String groupName) {
        setUserName(userName);
        setGroupName(groupName);
    }

    /**
     * Set this entry's file size.
     *
     * @param size This entry's new file size.
     * @throws IllegalArgumentException if the size is &lt; 0.
     */
    public void setSize(final long size) {
        if (size < 0){
            throw new IllegalArgumentException("Size is out of range: " + size);
        }
        this.size = size;
    }

    /**
     * Set this entry's sparse headers
     * @param sparseHeaders The new sparse headers
     * @since 1.20
     */
    public void setSparseHeaders(final List<TarArchiveStructSparse> sparseHeaders) {
        this.sparseHeaders = sparseHeaders;
    }

    /**
     * Set this entry's status change time.
     *
     * @param time This entry's new status change time.
     * @since 1.22
     */
    public void setStatusChangeTime(final FileTime time) {
        cTime = time;
    }

    /**
     * Set this entry's user id.
     *
     * @param userId This entry's new user id.
     */
    public void setUserId(final int userId) {
        setUserId((long) userId);
    }

    /**
     * Set this entry's user id.
     *
     * @param userId This entry's new user id.
     * @since 1.10
     */
    public void setUserId(final long userId) {
        this.userId = userId;
    }

    /**
     * Set this entry's user name.
     *
     * @param userName This entry's new user name.
     */
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /**
     * Update the entry using a map of pax headers.
     * @param headers
     * @since 1.15
     */
    void updateEntryFromPaxHeaders(final Map<String, String> headers) throws IOException {
        for (final Map.Entry<String, String> ent : headers.entrySet()) {
            processPaxHeader(ent.getKey(), ent.getValue(), headers);
        }
    }

    /**
     * Write an entry's header information to a header buffer.
     *
     * <p>This method does not use the star/GNU tar/BSD tar extensions.</p>
     *
     * @param outbuf The tar entry header buffer to fill in.
     */
    public void writeEntryHeader(final byte[] outbuf) {
        try {
            writeEntryHeader(outbuf, TarUtils.DEFAULT_ENCODING, false);
        } catch (final IOException ex) { // NOSONAR
            try {
                writeEntryHeader(outbuf, TarUtils.FALLBACK_ENCODING, false);
            } catch (final IOException ex2) {
                // impossible
                throw new UncheckedIOException(ex2); //NOSONAR
            }
        }
    }

    /**
     * Write an entry's header information to a header buffer.
     *
     * @param outbuf The tar entry header buffer to fill in.
     * @param encoding encoding to use when writing the file name.
     * @param starMode whether to use the star/GNU tar/BSD tar
     * extension for numeric fields if their value doesn't fit in the
     * maximum size of standard tar archives
     * @since 1.4
     * @throws IOException on error
     */
    public void writeEntryHeader(final byte[] outbuf, final ZipEncoding encoding,
                                 final boolean starMode) throws IOException {
        int offset = 0;

        offset = TarUtils.formatNameBytes(name, outbuf, offset, NAMELEN,
                                          encoding);
        offset = writeEntryHeaderField(mode, outbuf, offset, MODELEN, starMode);
        offset = writeEntryHeaderField(userId, outbuf, offset, UIDLEN,
                                       starMode);
        offset = writeEntryHeaderField(groupId, outbuf, offset, GIDLEN,
                                       starMode);
        offset = writeEntryHeaderField(size, outbuf, offset, SIZELEN, starMode);
        offset = writeEntryHeaderField(TimeUtils.toUnixTime(mTime), outbuf, offset,
                                       MODTIMELEN, starMode);

        final int csOffset = offset;

        offset = fill((byte) ' ', offset, outbuf, CHKSUMLEN);

        outbuf[offset++] = linkFlag;
        offset = TarUtils.formatNameBytes(linkName, outbuf, offset, NAMELEN,
                                          encoding);
        offset = TarUtils.formatNameBytes(magic, outbuf, offset, MAGICLEN);
        offset = TarUtils.formatNameBytes(version, outbuf, offset, VERSIONLEN);
        offset = TarUtils.formatNameBytes(userName, outbuf, offset, UNAMELEN,
                                          encoding);
        offset = TarUtils.formatNameBytes(groupName, outbuf, offset, GNAMELEN,
                                          encoding);
        offset = writeEntryHeaderField(devMajor, outbuf, offset, DEVLEN,
                                       starMode);
        offset = writeEntryHeaderField(devMinor, outbuf, offset, DEVLEN,
                                       starMode);

        if (starMode) {
            // skip prefix
            offset = fill(0, offset, outbuf, PREFIXLEN_XSTAR);
            offset = writeEntryHeaderOptionalTimeField(aTime, offset, outbuf, ATIMELEN_XSTAR);
            offset = writeEntryHeaderOptionalTimeField(cTime, offset, outbuf, CTIMELEN_XSTAR);
            // 8-byte fill
            offset = fill(0, offset, outbuf, 8);
            // Do not write MAGIC_XSTAR because it causes issues with some TAR tools
            // This makes it effectively XUSTAR, which guarantees compatibility with USTAR
            offset = fill(0, offset, outbuf, XSTAR_MAGIC_LEN);
        }

        offset = fill(0, offset, outbuf, outbuf.length - offset); // NOSONAR - assignment as documentation

        final long chk = TarUtils.computeCheckSum(outbuf);

        TarUtils.formatCheckSumOctalBytes(chk, outbuf, csOffset, CHKSUMLEN);
    }

    private int writeEntryHeaderField(final long value, final byte[] outbuf, final int offset,
                                      final int length, final boolean starMode) {
        if (!starMode && (value < 0
                          || value >= 1L << 3 * (length - 1))) {
            // value doesn't fit into field when written as octal
            // number, will be written to PAX header or causes an
            // error
            return TarUtils.formatLongOctalBytes(0, outbuf, offset, length);
        }
        return TarUtils.formatLongOctalOrBinaryBytes(value, outbuf, offset,
                                                     length);
    }

    private int writeEntryHeaderOptionalTimeField(final FileTime time, int offset, final byte[] outbuf, final int fieldLength) {
        if (time != null) {
            offset = writeEntryHeaderField(TimeUtils.toUnixTime(time), outbuf, offset, fieldLength, true);
        } else {
            offset = fill(0, offset, outbuf, fieldLength);
        }
        return offset;
    }
}


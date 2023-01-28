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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.EntryStreamOffsets;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.TimeUtils;

/**
 * Extension that adds better handling of extra fields and provides
 * access to the internal and external file attributes.
 *
 * <p>The extra data is expected to follow the recommendation of
 * <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">APPNOTE.TXT</a>:</p>
 * <ul>
 *   <li>the extra byte array consists of a sequence of extra fields</li>
 *   <li>each extra fields starts by a two byte header id followed by
 *   a two byte sequence holding the length of the remainder of
 *   data.</li>
 * </ul>
 *
 * <p>Any extra data that cannot be parsed by the rules above will be
 * consumed as "unparseable" extra data and treated differently by the
 * methods of this class.  Versions prior to Apache Commons Compress
 * 1.1 would have thrown an exception if any attempt was made to read
 * or write extra data not conforming to the recommendation.</p>
 *
 * @NotThreadSafe
 */
public class ZipArchiveEntry extends java.util.zip.ZipEntry implements ArchiveEntry, EntryStreamOffsets {

    /**
     * Indicates how the comment of this entry has been determined.
     * @since 1.16
     */
    public enum CommentSource {
        /**
         * The comment has been read from the archive using the encoding
         * of the archive specified when creating the {@link
         * ZipArchiveInputStream} or {@link ZipFile} (defaults to the
         * platform's default encoding).
         */
        COMMENT,
        /**
         * The comment has been read from an {@link UnicodeCommentExtraField
         * Unicode Extra Field}.
         */
        UNICODE_EXTRA_FIELD
    }

    /**
     * How to try to parse the extra fields.
     *
     * <p>Configures the behavior for:</p>
     * <ul>
     *   <li>What shall happen if the extra field content doesn't
     *   follow the recommended pattern of two-byte id followed by a
     *   two-byte length?</li>
     *  <li>What shall happen if an extra field is generally supported
     *  by Commons Compress but its content cannot be parsed
     *  correctly? This may for example happen if the archive is
     *  corrupt, it triggers a bug in Commons Compress or the extra
     *  field uses a version not (yet) supported by Commons
     *  Compress.</li>
     * </ul>
     *
     * @since 1.19
     */
    public enum ExtraFieldParsingMode implements ExtraFieldParsingBehavior {
        /**
         * Try to parse as many extra fields as possible and wrap
         * unknown extra fields as well as supported extra fields that
         * cannot be parsed in {@link UnrecognizedExtraField}.
         *
         * <p>Wrap extra data that doesn't follow the recommended
         * pattern in an {@link UnparseableExtraFieldData}
         * instance.</p>
         *
         * <p>This is the default behavior starting with Commons Compress 1.19.</p>
         */
        BEST_EFFORT(ExtraFieldUtils.UnparseableExtraField.READ) {
            @Override
            public ZipExtraField fill(final ZipExtraField field, final byte[] data, final int off, final int len, final boolean local) {
                return fillAndMakeUnrecognizedOnError(field, data, off, len, local);
            }
        },
        /**
         * Try to parse as many extra fields as possible and wrap
         * unknown extra fields in {@link UnrecognizedExtraField}.
         *
         * <p>Wrap extra data that doesn't follow the recommended
         * pattern in an {@link UnparseableExtraFieldData}
         * instance.</p>
         *
         * <p>Throw an exception if an extra field that is generally
         * supported cannot be parsed.</p>
         *
         * <p>This used to be the default behavior prior to Commons
         * Compress 1.19.</p>
         */
        STRICT_FOR_KNOW_EXTRA_FIELDS(ExtraFieldUtils.UnparseableExtraField.READ),
        /**
         * Try to parse as many extra fields as possible and wrap
         * unknown extra fields as well as supported extra fields that
         * cannot be parsed in {@link UnrecognizedExtraField}.
         *
         * <p>Ignore extra data that doesn't follow the recommended
         * pattern.</p>
         */
        ONLY_PARSEABLE_LENIENT(ExtraFieldUtils.UnparseableExtraField.SKIP) {
            @Override
            public ZipExtraField fill(final ZipExtraField field, final byte[] data, final int off, final int len, final boolean local) {
                return fillAndMakeUnrecognizedOnError(field, data, off, len, local);
            }
        },
        /**
         * Try to parse as many extra fields as possible and wrap
         * unknown extra fields in {@link UnrecognizedExtraField}.
         *
         * <p>Ignore extra data that doesn't follow the recommended
         * pattern.</p>
         *
         * <p>Throw an exception if an extra field that is generally
         * supported cannot be parsed.</p>
         */
        ONLY_PARSEABLE_STRICT(ExtraFieldUtils.UnparseableExtraField.SKIP),
        /**
         * Throw an exception if any of the recognized extra fields
         * cannot be parsed or any extra field violates the
         * recommended pattern.
         */
        DRACONIC(ExtraFieldUtils.UnparseableExtraField.THROW);

        private static ZipExtraField fillAndMakeUnrecognizedOnError(final ZipExtraField field, final byte[] data, final int off,
            final int len, final boolean local) {
            try {
                return ExtraFieldUtils.fillExtraField(field, data, off, len, local);
            } catch (final ZipException ex) {
                final UnrecognizedExtraField u = new UnrecognizedExtraField();
                u.setHeaderId(field.getHeaderId());
                if (local) {
                    u.setLocalFileDataData(Arrays.copyOfRange(data, off, off + len));
                } else {
                    u.setCentralDirectoryData(Arrays.copyOfRange(data, off, off + len));
                }
                return u;
            }
        }

        private final ExtraFieldUtils.UnparseableExtraField onUnparseableData;

        ExtraFieldParsingMode(final ExtraFieldUtils.UnparseableExtraField onUnparseableData) {
            this.onUnparseableData = onUnparseableData;
        }

        @Override
        public ZipExtraField createExtraField(final ZipShort headerId)
            throws ZipException, InstantiationException, IllegalAccessException {
            return ExtraFieldUtils.createExtraField(headerId);
        }

        @Override
        public ZipExtraField fill(final ZipExtraField field, final byte[] data, final int off, final int len, final boolean local)
            throws ZipException {
            return ExtraFieldUtils.fillExtraField(field, data, off, len, local);
        }

        @Override
        public ZipExtraField onUnparseableExtraField(final byte[] data, final int off, final int len, final boolean local,
            final int claimedLength) throws ZipException {
            return onUnparseableData.onUnparseableExtraField(data, off, len, local, claimedLength);
        }
    }
    /**
     * Indicates how the name of this entry has been determined.
     * @since 1.16
     */
    public enum NameSource {
        /**
         * The name has been read from the archive using the encoding
         * of the archive specified when creating the {@link
         * ZipArchiveInputStream} or {@link ZipFile} (defaults to the
         * platform's default encoding).
         */
        NAME,
        /**
         * The name has been read from the archive and the archive
         * specified the EFS flag which indicates the name has been
         * encoded as UTF-8.
         */
        NAME_WITH_EFS_FLAG,
        /**
         * The name has been read from an {@link UnicodePathExtraField
         * Unicode Extra Field}.
         */
        UNICODE_EXTRA_FIELD
    }
    static final ZipArchiveEntry[] EMPTY_ARRAY = {};
    public static final int PLATFORM_UNIX = 3;
    public static final int PLATFORM_FAT  = 0;
    public static final int CRC_UNKNOWN = -1;

    private static final int SHORT_MASK = 0xFFFF;

    private static final int SHORT_SHIFT = 16;

    private static boolean canConvertToInfoZipExtendedTimestamp(
            final FileTime lastModifiedTime,
            final FileTime lastAccessTime,
            final FileTime creationTime) {
        return TimeUtils.isUnixTime(lastModifiedTime)
                && TimeUtils.isUnixTime(lastAccessTime)
                && TimeUtils.isUnixTime(creationTime);
    }

    /**
     * The {@link java.util.zip.ZipEntry} base class only supports
     * the compression methods STORED and DEFLATED. We override the
     * field so that any compression methods can be used.
     * <p>
     * The default value -1 means that the method has not been specified.
     *
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-93"
     *        >COMPRESS-93</a>
     */
    private int method = ZipMethod.UNKNOWN_CODE;
    /**
     * The {@link java.util.zip.ZipEntry#setSize} method in the base
     * class throws an IllegalArgumentException if the size is bigger
     * than 2GB for Java versions &lt; 7 and even in Java 7+ if the
     * implementation in java.util.zip doesn't support Zip64 itself
     * (it is an optional feature).
     *
     * <p>We need to keep our own size information for Zip64 support.</p>
     */
    private long size = SIZE_UNKNOWN;
    private int internalAttributes;
    private int versionRequired;
    private int versionMadeBy;
    private int platform = PLATFORM_FAT;
    private int rawFlag;
    private long externalAttributes;
    private int alignment;
    private ZipExtraField[] extraFields;
    private UnparseableExtraFieldData unparseableExtra;
    private String name;
    private byte[] rawName;
    private GeneralPurposeBit gpb = new GeneralPurposeBit();
    private long localHeaderOffset = OFFSET_UNKNOWN;
    private long dataOffset = OFFSET_UNKNOWN;
    private boolean isStreamContiguous;
    private NameSource nameSource = NameSource.NAME;

    private CommentSource commentSource = CommentSource.COMMENT;

    private long diskNumberStart;

    private boolean lastModifiedDateSet = false;

    private long time = -1;

    /**
     */
    protected ZipArchiveEntry() {
        this("");
    }

    /**
     * Creates a new ZIP entry taking some information from the given
     * file and using the provided name.
     *
     * <p>The name will be adjusted to end with a forward slash "/" if
     * the file is a directory.  If the file is not a directory a
     * potential trailing forward slash will be stripped from the
     * entry name.</p>
     * @param inputFile file to create the entry from
     * @param entryName name of the entry
     */
    public ZipArchiveEntry(final File inputFile, final String entryName) {
        this(inputFile.isDirectory() && !entryName.endsWith("/") ?
             entryName + "/" : entryName);
        try {
            setAttributes(inputFile.toPath());
        } catch (IOException e) { // NOSONAR
            if (inputFile.isFile()){
                setSize(inputFile.length());
            }
            setTime(inputFile.lastModified());
        }
    }

    /**
     * Creates a new ZIP entry with fields taken from the specified ZIP entry.
     *
     * <p>Assumes the entry represents a directory if and only if the
     * name ends with a forward slash "/".</p>
     *
     * @param entry the entry to get fields from
     * @throws ZipException on error
     */
    public ZipArchiveEntry(final java.util.zip.ZipEntry entry) throws ZipException {
        super(entry);
        setName(entry.getName());
        final byte[] extra = entry.getExtra();
        if (extra != null) {
            setExtraFields(ExtraFieldUtils.parse(extra, true, ExtraFieldParsingMode.BEST_EFFORT));
        } else {
            // initializes extra data to an empty byte array
            setExtra();
        }
        setMethod(entry.getMethod());
        this.size = entry.getSize();
    }

    /**
     * Creates a new ZIP entry taking some information from the given
     * path and using the provided name.
     *
     * <p>The name will be adjusted to end with a forward slash "/" if
     * the file is a directory.  If the file is not a directory a
     * potential trailing forward slash will be stripped from the
     * entry name.</p>
     * @param inputPath path to create the entry from.
     * @param entryName name of the entry.
     * @param options options indicating how symbolic links are handled.
     * @throws IOException if an I/O error occurs.
     * @since 1.21
     */
    public ZipArchiveEntry(final Path inputPath, final String entryName, final LinkOption... options) throws IOException {
        this(Files.isDirectory(inputPath, options) && !entryName.endsWith("/") ?
             entryName + "/" : entryName);
        setAttributes(inputPath, options);
    }

    /**
     * Creates a new ZIP entry with the specified name.
     *
     * <p>Assumes the entry represents a directory if and only if the
     * name ends with a forward slash "/".</p>
     *
     * @param name the name of the entry
     */
    public ZipArchiveEntry(final String name) {
        super(name);
        setName(name);
    }

    /**
     * Creates a new ZIP entry with fields taken from the specified ZIP entry.
     *
     * <p>Assumes the entry represents a directory if and only if the
     * name ends with a forward slash "/".</p>
     *
     * @param entry the entry to get fields from
     * @throws ZipException on error
     */
    public ZipArchiveEntry(final ZipArchiveEntry entry) throws ZipException {
        this((java.util.zip.ZipEntry) entry);
        setInternalAttributes(entry.getInternalAttributes());
        setExternalAttributes(entry.getExternalAttributes());
        setExtraFields(entry.getAllExtraFieldsNoCopy());
        setPlatform(entry.getPlatform());
        final GeneralPurposeBit other = entry.getGeneralPurposeBit();
        setGeneralPurposeBit(other == null ? null :
                             (GeneralPurposeBit) other.clone());
    }

    /**
     * Adds an extra field - replacing an already present extra field
     * of the same type.
     *
     * <p>The new extra field will be the first one.</p>
     * @param ze an extra field
     */
    public void addAsFirstExtraField(final ZipExtraField ze) {
        if (ze instanceof UnparseableExtraFieldData) {
            unparseableExtra = (UnparseableExtraFieldData) ze;
        } else {
            if (getExtraField(ze.getHeaderId()) != null) {
                internalRemoveExtraField(ze.getHeaderId());
            }
            final ZipExtraField[] copy = extraFields;
            final int newLen = extraFields != null ? extraFields.length + 1 : 1;
            extraFields = new ZipExtraField[newLen];
            extraFields[0] = ze;
            if (copy != null){
                System.arraycopy(copy, 0, extraFields, 1, extraFields.length - 1);
            }
        }
        setExtra();
    }

    /**
     * Adds an extra field - replacing an already present extra field
     * of the same type.
     *
     * <p>If no extra field of the same type exists, the field will be
     * added as last field.</p>
     * @param ze an extra field
     */
    public void addExtraField(final ZipExtraField ze) {
        internalAddExtraField(ze);
        setExtra();
    }

    private void addInfoZipExtendedTimestamp(
            final FileTime lastModifiedTime,
            final FileTime lastAccessTime,
            final FileTime creationTime) {
        final X5455_ExtendedTimestamp infoZipTimestamp = new X5455_ExtendedTimestamp();
        if (lastModifiedTime != null) {
            infoZipTimestamp.setModifyFileTime(lastModifiedTime);
        }
        if (lastAccessTime != null) {
            infoZipTimestamp.setAccessFileTime(lastAccessTime);
        }
        if (creationTime != null) {
            infoZipTimestamp.setCreateFileTime(creationTime);
        }
        internalAddExtraField(infoZipTimestamp);
    }

    private void addNTFSTimestamp(
            final FileTime lastModifiedTime,
            final FileTime lastAccessTime,
            final FileTime creationTime) {
        final X000A_NTFS ntfsTimestamp = new X000A_NTFS();
        if (lastModifiedTime != null) {
            ntfsTimestamp.setModifyFileTime(lastModifiedTime);
        }
        if (lastAccessTime != null) {
            ntfsTimestamp.setAccessFileTime(lastAccessTime);
        }
        if (creationTime != null) {
            ntfsTimestamp.setCreateFileTime(creationTime);
        }
        internalAddExtraField(ntfsTimestamp);
    }

    /**
     * Overwrite clone.
     * @return a cloned copy of this ZipArchiveEntry
     */
    @Override
    public Object clone() {
        final ZipArchiveEntry e = (ZipArchiveEntry) super.clone();

        e.setInternalAttributes(getInternalAttributes());
        e.setExternalAttributes(getExternalAttributes());
        e.setExtraFields(getAllExtraFieldsNoCopy());
        return e;
    }

    private ZipExtraField[] copyOf(final ZipExtraField[] src, final int length) {
        return Arrays.copyOf(src, length);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ZipArchiveEntry other = (ZipArchiveEntry) obj;
        final String myName = getName();
        final String otherName = other.getName();
        if (!Objects.equals(myName, otherName)) {
            return false;
        }
        String myComment = getComment();
        String otherComment = other.getComment();
        if (myComment == null) {
            myComment = "";
        }
        if (otherComment == null) {
            otherComment = "";
        }
        return Objects.equals(getLastModifiedTime(), other.getLastModifiedTime())
            && Objects.equals(getLastAccessTime(), other.getLastAccessTime())
            && Objects.equals(getCreationTime(), other.getCreationTime())
            && myComment.equals(otherComment)
            && getInternalAttributes() == other.getInternalAttributes()
            && getPlatform() == other.getPlatform()
            && getExternalAttributes() == other.getExternalAttributes()
            && getMethod() == other.getMethod()
            && getSize() == other.getSize()
            && getCrc() == other.getCrc()
            && getCompressedSize() == other.getCompressedSize()
            && Arrays.equals(getCentralDirectoryExtra(),
                             other.getCentralDirectoryExtra())
            && Arrays.equals(getLocalFileDataExtra(),
                             other.getLocalFileDataExtra())
            && localHeaderOffset == other.localHeaderOffset
            && dataOffset == other.dataOffset
            && gpb.equals(other.gpb);
    }

    private ZipExtraField findMatching(final ZipShort headerId, final List<ZipExtraField> fs) {
        return fs.stream().filter(f -> headerId.equals(f.getHeaderId())).findFirst().orElse(null);
    }

    private ZipExtraField findUnparseable(final List<ZipExtraField> fs) {
        return fs.stream().filter(UnparseableExtraFieldData.class::isInstance).findFirst().orElse(null);
    }

    /**
     * Gets currently configured alignment.
     *
     * @return
     *      alignment for this entry.
     * @since 1.14
     */
    protected int getAlignment() {
        return this.alignment;
    }

    private ZipExtraField[] getAllExtraFields() {
        final ZipExtraField[] allExtraFieldsNoCopy = getAllExtraFieldsNoCopy();
        return (allExtraFieldsNoCopy == extraFields) ? copyOf(allExtraFieldsNoCopy, allExtraFieldsNoCopy.length)
            : allExtraFieldsNoCopy;
    }

    /**
     * Get all extra fields, including unparseable ones.
     * @return An array of all extra fields. Not necessarily a copy of internal data structures, hence private method
     */
    private ZipExtraField[] getAllExtraFieldsNoCopy() {
        if (extraFields == null) {
            return getUnparseableOnly();
        }
        return unparseableExtra != null ? getMergedFields() : extraFields;
    }

    /**
     * Retrieves the extra data for the central directory.
     * @return the central directory extra data
     */
    public byte[] getCentralDirectoryExtra() {
        return ExtraFieldUtils.mergeCentralDirectoryData(getAllExtraFieldsNoCopy());
    }

    /**
     * The source of the comment field value.
     * @return source of the comment field value
     * @since 1.16
     */
    public CommentSource getCommentSource() {
        return commentSource;
    }

    @Override
    public long getDataOffset() {
        return dataOffset;
    }

    /**
     * The number of the split segment this entry starts at.
     *
     * @return the number of the split segment this entry starts at.
     * @since 1.20
     */
    public long getDiskNumberStart() {
        return diskNumberStart;
    }

    /**
     * Retrieves the external file attributes.
     *
     * <p><b>Note</b>: {@link ZipArchiveInputStream} is unable to fill
     * this field, you must use {@link ZipFile} if you want to read
     * entries using this attribute.</p>
     *
     * @return the external file attributes
     */
    public long getExternalAttributes() {
        return externalAttributes;
    }

    /**
     * Looks up an extra field by its header id.
     *
     * @param type the header id
     * @return null if no such field exists.
     */
    public ZipExtraField getExtraField(final ZipShort type) {
        if (extraFields != null) {
            for (final ZipExtraField extraField : extraFields) {
                if (type.equals(extraField.getHeaderId())) {
                    return extraField;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all extra fields that have been parsed successfully.
     *
     * <p><b>Note</b>: The set of extra fields may be incomplete when
     * {@link ZipArchiveInputStream} has been used as some extra
     * fields use the central directory to store additional
     * information.</p>
     *
     * @return an array of the extra fields
     */
    public ZipExtraField[] getExtraFields() {
        return getParseableExtraFields();
    }

    /**
     * Retrieves extra fields.
     * @param includeUnparseable whether to also return unparseable
     * extra fields as {@link UnparseableExtraFieldData} if such data
     * exists.
     * @return an array of the extra fields
     *
     * @since 1.1
     */
    public ZipExtraField[] getExtraFields(final boolean includeUnparseable) {
        return includeUnparseable ?
                getAllExtraFields() :
                getParseableExtraFields();
    }

    /**
     * Retrieves extra fields.
     * @param parsingBehavior controls parsing of extra fields.
     * @return an array of the extra fields
     *
     * @throws ZipException if parsing fails, can not happen if {@code
     * parsingBehavior} is {@link ExtraFieldParsingMode#BEST_EFFORT}.
     *
     * @since 1.19
     */
    public ZipExtraField[] getExtraFields(final ExtraFieldParsingBehavior parsingBehavior)
        throws ZipException {
        if (parsingBehavior == ExtraFieldParsingMode.BEST_EFFORT) {
            return getExtraFields(true);
        }
        if (parsingBehavior == ExtraFieldParsingMode.ONLY_PARSEABLE_LENIENT) {
            return getExtraFields(false);
        }
        final byte[] local = getExtra();
        final List<ZipExtraField> localFields = new ArrayList<>(Arrays.asList(ExtraFieldUtils.parse(local, true,
            parsingBehavior)));
        final byte[] central = getCentralDirectoryExtra();
        final List<ZipExtraField> centralFields = new ArrayList<>(Arrays.asList(ExtraFieldUtils.parse(central, false,
            parsingBehavior)));
        final List<ZipExtraField> merged = new ArrayList<>();
        for (final ZipExtraField l : localFields) {
            ZipExtraField c = null;
            if (l instanceof UnparseableExtraFieldData) {
                c = findUnparseable(centralFields);
            } else {
                c = findMatching(l.getHeaderId(), centralFields);
            }
            if (c != null) {
                final byte[] cd = c.getCentralDirectoryData();
                if (cd != null && cd.length > 0) {
                    l.parseFromCentralDirectoryData(cd, 0, cd.length);
                }
                centralFields.remove(c);
            }
            merged.add(l);
        }
        merged.addAll(centralFields);
        return merged.toArray(ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY);
    }

    /**
     * The "general purpose bit" field.
     * @return the general purpose bit
     * @since 1.1
     */
    public GeneralPurposeBit getGeneralPurposeBit() {
        return gpb;
    }

    /**
     * Retrieves the internal file attributes.
     *
     * <p><b>Note</b>: {@link ZipArchiveInputStream} is unable to fill
     * this field, you must use {@link ZipFile} if you want to read
     * entries using this attribute.</p>
     *
     * @return the internal file attributes
     */
    public int getInternalAttributes() {
        return internalAttributes;
    }

    /**
     * Wraps {@link java.util.zip.ZipEntry#getTime} with a {@link Date} as the
     * entry's last modified date.
     *
     * <p>Changes to the implementation of {@link java.util.zip.ZipEntry#getTime}
     * leak through and the returned value may depend on your local
     * time zone as well as your version of Java.</p>
     */
    @Override
    public Date getLastModifiedDate() {
        return new Date(getTime());
    }

    /**
     * Retrieves the extra data for the local file data.
     * @return the extra data for local file
     */
    public byte[] getLocalFileDataExtra() {
        final byte[] extra = getExtra();
        return extra != null ? extra : ByteUtils.EMPTY_BYTE_ARRAY;
    }

    protected long getLocalHeaderOffset() {
        return this.localHeaderOffset;
    }

    private ZipExtraField[] getMergedFields() {
        final ZipExtraField[] zipExtraFields = copyOf(extraFields, extraFields.length + 1);
        zipExtraFields[extraFields.length] = unparseableExtra;
        return zipExtraFields;
    }

    /**
     * Returns the compression method of this entry, or -1 if the
     * compression method has not been specified.
     *
     * @return compression method
     *
     * @since 1.1
     */
    @Override
    public int getMethod() {
        return method;
    }

    /**
     * Get the name of the entry.
     *
     * <p>This method returns the raw name as it is stored inside of the archive.</p>
     *
     * @return the entry name
     */
    @Override
    public String getName() {
        return name == null ? super.getName() : name;
    }

    /**
     * The source of the name field value.
     * @return source of the name field value
     * @since 1.16
     */
    public NameSource getNameSource() {
        return nameSource;
    }

    private ZipExtraField[] getParseableExtraFields() {
        final ZipExtraField[] parseableExtraFields = getParseableExtraFieldsNoCopy();
        return (parseableExtraFields == extraFields) ? copyOf(parseableExtraFields, parseableExtraFields.length)
            : parseableExtraFields;
    }

    private ZipExtraField[] getParseableExtraFieldsNoCopy() {
        if (extraFields == null) {
            return ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY;
        }
        return extraFields;
    }

    /**
     * Platform specification to put into the &quot;version made
     * by&quot; part of the central file header.
     *
     * @return PLATFORM_FAT unless {@link #setUnixMode setUnixMode}
     * has been called, in which case PLATFORM_UNIX will be returned.
     */
    public int getPlatform() {
        return platform;
    }

    /**
     * The content of the flags field.
     * @return content of the flags field
     * @since 1.11
     */
    public int getRawFlag() {
        return rawFlag;
    }

    /**
     * Returns the raw bytes that made up the name before it has been
     * converted using the configured or guessed encoding.
     *
     * <p>This method will return null if this instance has not been
     * read from an archive.</p>
     *
     * @return the raw name bytes
     * @since 1.2
     */
    public byte[] getRawName() {
        if (rawName != null) {
            return Arrays.copyOf(rawName, rawName.length);
        }
        return null;
    }

    /**
     * Gets the uncompressed size of the entry data.
     *
     * <p><b>Note</b>: {@link ZipArchiveInputStream} may create
     * entries that return {@link #SIZE_UNKNOWN SIZE_UNKNOWN} as long
     * as the entry hasn't been read completely.</p>
     *
     * @return the entry size
     */
    @Override
    public long getSize() {
        return size;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Override to work around bug <a href="https://bugs.openjdk.org/browse/JDK-8130914">JDK-8130914</a></p>
     *
     * @return  The last modification time of the entry in milliseconds
     *          since the epoch, or -1 if not specified
     *
     * @see #setTime(long)
     * @see #setLastModifiedTime(FileTime)
     */
    @Override
    public long getTime() {
        if (lastModifiedDateSet) {
            return getLastModifiedTime().toMillis();
        }
        return time != -1 ? time : super.getTime();
    }

    /**
     * Unix permission.
     * @return the unix permissions
     */
    public int getUnixMode() {
        return platform != PLATFORM_UNIX ? 0 :
            (int) ((getExternalAttributes() >> SHORT_SHIFT) & SHORT_MASK);
    }

    /**
     * Looks up extra field data that couldn't be parsed correctly.
     *
     * @return null if no such field exists.
     *
     * @since 1.1
     */
    public UnparseableExtraFieldData getUnparseableExtraFieldData() {
        return unparseableExtra;
    }

    private ZipExtraField[] getUnparseableOnly() {
        return unparseableExtra == null ? ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY : new ZipExtraField[] { unparseableExtra };
    }

    /**
     * The "version made by" field.
     * @return "version made by" field
     * @since 1.11
     */
    public int getVersionMadeBy() {
        return versionMadeBy;
    }

    /**
     * The "version required to expand" field.
     * @return "version required to expand" field
     * @since 1.11
     */
    public int getVersionRequired() {
        return versionRequired;
    }

    /**
     * Get the hash code of the entry.
     * This uses the name as the hash code.
     * @return a hash code.
     */
    @Override
    public int hashCode() {
        // this method has severe consequences on performance. We cannot rely
        // on the super.hashCode() method since super.getName() always return
        // the empty string in the current implementation (there's no setter)
        // so it is basically draining the performance of a hashmap lookup
        return getName().hashCode();
    }

    private void internalAddExtraField(final ZipExtraField ze) {
        if (ze instanceof UnparseableExtraFieldData) {
            unparseableExtra = (UnparseableExtraFieldData) ze;
        } else if (extraFields == null) {
            extraFields = new ZipExtraField[]{ze};
        } else {
            if (getExtraField(ze.getHeaderId()) != null) {
                internalRemoveExtraField(ze.getHeaderId());
            }
            final ZipExtraField[] zipExtraFields = copyOf(extraFields, extraFields.length + 1);
            zipExtraFields[zipExtraFields.length - 1] = ze;
            extraFields = zipExtraFields;
        }
    }

    private void internalRemoveExtraField(final ZipShort type) {
        if (extraFields == null) {
            return;
        }
        final List<ZipExtraField> newResult = new ArrayList<>();
        for (final ZipExtraField extraField : extraFields) {
            if (!type.equals(extraField.getHeaderId())) {
                newResult.add(extraField);
            }
        }
        if (extraFields.length == newResult.size()) {
            return;
        }
        extraFields = newResult.toArray(ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY);
    }

    private void internalSetLastModifiedTime(final FileTime time) {
        super.setLastModifiedTime(time);
        this.time = time.toMillis();
        lastModifiedDateSet = true;
    }

    /**
     * Is this entry a directory?
     * @return true if the entry is a directory
     */
    @Override
    public boolean isDirectory() {
        return getName().endsWith("/");
    }

    @Override
    public boolean isStreamContiguous() {
        return isStreamContiguous;
    }

    /**
     * Returns true if this entry represents a unix symlink,
     * in which case the entry's content contains the target path
     * for the symlink.
     *
     * @since 1.5
     * @return true if the entry represents a unix symlink, false otherwise.
     */
    public boolean isUnixSymlink() {
        return (getUnixMode() & UnixStat.FILE_TYPE_FLAG) == UnixStat.LINK_FLAG;
    }

    /**
     * If there are no extra fields, use the given fields as new extra
     * data - otherwise merge the fields assuming the existing fields
     * and the new fields stem from different locations inside the
     * archive.
     * @param f the extra fields to merge
     * @param local whether the new fields originate from local data
     */
    private void mergeExtraFields(final ZipExtraField[] f, final boolean local) {
        if (extraFields == null) {
            setExtraFields(f);
        } else {
            for (final ZipExtraField element : f) {
                final ZipExtraField existing;
                if (element instanceof UnparseableExtraFieldData) {
                    existing = unparseableExtra;
                } else {
                    existing = getExtraField(element.getHeaderId());
                }
                if (existing == null) {
                    internalAddExtraField(element);
                } else {
                    final byte[] b = local ? element.getLocalFileDataData()
                        : element.getCentralDirectoryData();
                    try {
                        if (local) {
                            existing.parseFromLocalFileData(b, 0, b.length);
                        } else {
                            existing.parseFromCentralDirectoryData(b, 0, b.length);
                        }
                    } catch (final ZipException ex) {
                        // emulate ExtraFieldParsingMode.fillAndMakeUnrecognizedOnError
                        final UnrecognizedExtraField u = new UnrecognizedExtraField();
                        u.setHeaderId(existing.getHeaderId());
                        if (local) {
                            u.setLocalFileDataData(b);
                            u.setCentralDirectoryData(existing.getCentralDirectoryData());
                        } else {
                            u.setLocalFileDataData(existing.getLocalFileDataData());
                            u.setCentralDirectoryData(b);
                        }
                        internalRemoveExtraField(existing.getHeaderId());
                        internalAddExtraField(u);
                    }
                }
            }
            setExtra();
        }
    }

    /**
     * Remove an extra field.
     * @param type the type of extra field to remove
     */
    public void removeExtraField(final ZipShort type) {
        if (getExtraField(type) == null) {
            throw new NoSuchElementException();
        }
        internalRemoveExtraField(type);
        setExtra();
    }

    /**
     * Removes unparseable extra field data.
     *
     * @since 1.1
     */
    public void removeUnparseableExtraFieldData() {
        if (unparseableExtra == null) {
            throw new NoSuchElementException();
        }
        unparseableExtra = null;
        setExtra();
    }

    private boolean requiresExtraTimeFields() {
        if (getLastAccessTime() != null || getCreationTime() != null) {
            return true;
        }
        return lastModifiedDateSet;
    }

    /**
     * Sets alignment for this entry.
     *
     * @param alignment
     *      requested alignment, 0 for default.
     * @since 1.14
     */
    public void setAlignment(final int alignment) {
        if ((alignment & (alignment - 1)) != 0 || alignment > 0xffff) {
            throw new IllegalArgumentException("Invalid value for alignment, must be power of two and no bigger than "
                + 0xffff + " but is " + alignment);
        }
        this.alignment = alignment;
    }

    private void setAttributes(final Path inputPath, final LinkOption... options) throws IOException {
        final BasicFileAttributes attributes = Files.readAttributes(inputPath, BasicFileAttributes.class, options);
        if (attributes.isRegularFile()) {
            setSize(attributes.size());
        }
        super.setLastModifiedTime(attributes.lastModifiedTime());
        super.setCreationTime(attributes.creationTime());
        super.setLastAccessTime(attributes.lastAccessTime());
        setExtraTimeFields();
    }

    /**
     * Sets the central directory part of extra fields.
     * @param b an array of bytes to be parsed into extra fields
     */
	public void setCentralDirectoryExtra(final byte[] b) {
		try {
			mergeExtraFields(ExtraFieldUtils.parse(b, false, ExtraFieldParsingMode.BEST_EFFORT), false);
		} catch (final ZipException e) {
			// actually this is not possible as of Commons Compress 1.19
			throw new IllegalArgumentException(e.getMessage(), e); // NOSONAR
		}
	}

    /**
     * Sets the source of the comment field value.
     * @param commentSource source of the comment field value
     * @since 1.16
     */
    public void setCommentSource(final CommentSource commentSource) {
        this.commentSource = commentSource;
    }

    @Override
    public ZipEntry setCreationTime(final FileTime time) {
        super.setCreationTime(time);
        setExtraTimeFields();
        return this;
    }
    /* (non-Javadoc)
     * @see Object#equals(Object)
     */

    /**
     * Sets the data offset.
     *
     * @param dataOffset
     *      new value of data offset.
     */
    protected void setDataOffset(final long dataOffset) {
        this.dataOffset = dataOffset;
    }

    /**
     * The number of the split segment this entry starts at.
     *
     * @param diskNumberStart the number of the split segment this entry starts at.
     * @since 1.20
     */
    public void setDiskNumberStart(final long diskNumberStart) {
        this.diskNumberStart = diskNumberStart;
    }

    /**
     * Sets the external file attributes.
     * @param value an {@code long} value
     */
    public void setExternalAttributes(final long value) {
        externalAttributes = value;
    }

    /**
     * Unfortunately {@link java.util.zip.ZipOutputStream} seems to
     * access the extra data directly, so overriding getExtra doesn't
     * help - we need to modify super's data directly and on every update.
     */
    protected void setExtra() {
        // ZipEntry will update the time fields here, so we need to reprocess them afterwards
        super.setExtra(ExtraFieldUtils.mergeLocalFileDataData(getAllExtraFieldsNoCopy()));
        // Reprocess and overwrite the modifications made by ZipEntry#setExtra(byte[])
        updateTimeFieldsFromExtraFields();
    }

    /**
     * Parses the given bytes as extra field data and consumes any
     * unparseable data as an {@link UnparseableExtraFieldData}
     * instance.
     * @param extra an array of bytes to be parsed into extra fields
     * @throws RuntimeException if the bytes cannot be parsed
     * @throws RuntimeException on error
     */
    @Override
	public void setExtra(final byte[] extra) throws RuntimeException {
		try {
			mergeExtraFields(ExtraFieldUtils.parse(extra, true, ExtraFieldParsingMode.BEST_EFFORT), true);
		} catch (final ZipException e) {
			// actually this is not possible as of Commons Compress 1.1
			throw new IllegalArgumentException("Error parsing extra fields for entry: " // NOSONAR
					+ getName() + " - " + e.getMessage(), e);
		}
	}

    /**
     * Replaces all currently attached extra fields with the new array.
     * @param fields an array of extra fields
     */
    public void setExtraFields(final ZipExtraField[] fields) {
        unparseableExtra = null;
        final List<ZipExtraField> newFields = new ArrayList<>();
        if (fields != null) {
            for (final ZipExtraField field : fields) {
                if (field instanceof UnparseableExtraFieldData) {
                    unparseableExtra = (UnparseableExtraFieldData) field;
                } else {
                    newFields.add(field);
                }
            }
        }
        extraFields = newFields.toArray(ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY);
        setExtra();
    }

    private void setExtraTimeFields() {
        if (getExtraField(X5455_ExtendedTimestamp.HEADER_ID) != null) {
            internalRemoveExtraField(X5455_ExtendedTimestamp.HEADER_ID);
        }
        if (getExtraField(X000A_NTFS.HEADER_ID) != null) {
            internalRemoveExtraField(X000A_NTFS.HEADER_ID);
        }
        if (requiresExtraTimeFields()) {
            final FileTime lastModifiedTime = getLastModifiedTime();
            final FileTime lastAccessTime = getLastAccessTime();
            final FileTime creationTime = getCreationTime();
            if (canConvertToInfoZipExtendedTimestamp(lastModifiedTime, lastAccessTime, creationTime)) {
                addInfoZipExtendedTimestamp(lastModifiedTime, lastAccessTime, creationTime);
            }
            addNTFSTimestamp(lastModifiedTime, lastAccessTime, creationTime);
        }
        setExtra();
    }

    /**
     * The "general purpose bit" field.
     * @param b the general purpose bit
     * @since 1.1
     */
    public void setGeneralPurposeBit(final GeneralPurposeBit b) {
        gpb = b;
    }

    /**
     * Sets the internal file attributes.
     * @param value an {@code int} value
     */
    public void setInternalAttributes(final int value) {
        internalAttributes = value;
    }

    @Override
    public ZipEntry setLastAccessTime(final FileTime time) {
        super.setLastAccessTime(time);
        setExtraTimeFields();
        return this;
    }

    @Override
    public ZipEntry setLastModifiedTime(final FileTime time) {
        internalSetLastModifiedTime(time);
        setExtraTimeFields();
        return this;
    }

    protected void setLocalHeaderOffset(final long localHeaderOffset) {
        this.localHeaderOffset = localHeaderOffset;
    }

    /**
     * Sets the compression method of this entry.
     *
     * @param method compression method
     *
     * @since 1.1
     */
    @Override
    public void setMethod(final int method) {
        if (method < 0) {
            throw new IllegalArgumentException(
                    "ZIP compression method can not be negative: " + method);
        }
        this.method = method;
    }

    /**
     * Set the name of the entry.
     * @param name the name to use
     */
    protected void setName(String name) {
        if (name != null && getPlatform() == PLATFORM_FAT
            && !name.contains("/")) {
            name = name.replace('\\', '/');
        }
        this.name = name;
    }

    /**
     * Sets the name using the raw bytes and the string created from
     * it by guessing or using the configured encoding.
     * @param name the name to use created from the raw bytes using
     * the guessed or configured encoding
     * @param rawName the bytes originally read as name from the
     * archive
     * @since 1.2
     */
    protected void setName(final String name, final byte[] rawName) {
        setName(name);
        this.rawName = rawName;
    }

    /**
     * Sets the source of the name field value.
     * @param nameSource source of the name field value
     * @since 1.16
     */
    public void setNameSource(final NameSource nameSource) {
        this.nameSource = nameSource;
    }

    /**
     * Set the platform (UNIX or FAT).
     * @param platform an {@code int} value - 0 is FAT, 3 is UNIX
     */
    protected void setPlatform(final int platform) {
        this.platform = platform;
    }

    /**
     * Sets the content of the flags field.
     * @param rawFlag content of the flags field
     * @since 1.11
     */
    public void setRawFlag(final int rawFlag) {
        this.rawFlag = rawFlag;
    }

    /**
     * Sets the uncompressed size of the entry data.
     * @param size the uncompressed size in bytes
     * @throws IllegalArgumentException if the specified size is less
     *            than 0
     */
    @Override
    public void setSize(final long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Invalid entry size");
        }
        this.size = size;
    }

    protected void setStreamContiguous(final boolean isStreamContiguous) {
        this.isStreamContiguous = isStreamContiguous;
    }

    /**
     * Sets the modification time of the entry.
     * @param fileTime the entry modification time.
     * @since 1.21
     */
    public void setTime(final FileTime fileTime) {
        setTime(fileTime.toMillis());
    }

    /**
     *
     * {@inheritDoc}
     *
     * <p>Override to work around bug <a href="https://bugs.openjdk.org/browse/JDK-8130914">JDK-8130914</a></p>
     *
     * @param time
     *         The last modification time of the entry in milliseconds
     *         since the epoch
     * @see #getTime()
     * @see #getLastModifiedTime()
     */
    @Override
    public void setTime(final long time) {
        if (ZipUtil.isDosTime(time)) {
            super.setTime(time);
            this.time = time;
            lastModifiedDateSet = false;
            setExtraTimeFields();
        } else {
            setLastModifiedTime(FileTime.fromMillis(time));
        }
    }

    /**
     * Sets Unix permissions in a way that is understood by Info-Zip's
     * unzip command.
     * @param mode an {@code int} value
     */
    public void setUnixMode(final int mode) {
        // CheckStyle:MagicNumberCheck OFF - no point
        setExternalAttributes((mode << SHORT_SHIFT)
                              // MS-DOS read-only attribute
                              | ((mode & 0200) == 0 ? 1 : 0)
                              // MS-DOS directory flag
                              | (isDirectory() ? 0x10 : 0));
        // CheckStyle:MagicNumberCheck ON
        platform = PLATFORM_UNIX;
    }

    /**
     * Sets the "version made by" field.
     * @param versionMadeBy "version made by" field
     * @since 1.11
     */
    public void setVersionMadeBy(final int versionMadeBy) {
        this.versionMadeBy = versionMadeBy;
    }

    /**
     * Sets the "version required to expand" field.
     * @param versionRequired "version required to expand" field
     * @since 1.11
     */
    public void setVersionRequired(final int versionRequired) {
        this.versionRequired = versionRequired;
    }

    private void updateTimeFieldsFromExtraFields() {
        // Update times from X5455_ExtendedTimestamp field
        updateTimeFromExtendedTimestampField();
        // Update times from X000A_NTFS field, overriding X5455_ExtendedTimestamp if both are present
        updateTimeFromNtfsField();
    }

    /**
     * Workaround for the fact that, as of Java 17, {@link java.util.zip.ZipEntry} does not properly modify
     * the entry's {@code xdostime} field, only setting {@code mtime}. While this is not strictly necessary,
     * it's better to maintain the same behavior between this and the NTFS field.
     */
    private void updateTimeFromExtendedTimestampField() {
        final ZipExtraField extraField = getExtraField(X5455_ExtendedTimestamp.HEADER_ID);
        if (extraField instanceof X5455_ExtendedTimestamp) {
            final X5455_ExtendedTimestamp extendedTimestamp = (X5455_ExtendedTimestamp) extraField;
            if (extendedTimestamp.isBit0_modifyTimePresent()) {
                final FileTime modifyTime = extendedTimestamp.getModifyFileTime();
                if (modifyTime != null) {
                    internalSetLastModifiedTime(modifyTime);
                }
            }
            if (extendedTimestamp.isBit1_accessTimePresent()) {
                final FileTime accessTime = extendedTimestamp.getAccessFileTime();
                if (accessTime != null) {
                    super.setLastAccessTime(accessTime);
                }
            }
            if (extendedTimestamp.isBit2_createTimePresent()) {
                final FileTime creationTime = extendedTimestamp.getCreateFileTime();
                if (creationTime != null) {
                    super.setCreationTime(creationTime);
                }
            }
        }
    }

    /**
     * Workaround for the fact that, as of Java 17, {@link java.util.zip.ZipEntry} parses NTFS
     * timestamps with a maximum precision of microseconds, which is lower than the 100ns precision
     * provided by this extra field.
     */
    private void updateTimeFromNtfsField() {
        final ZipExtraField extraField = getExtraField(X000A_NTFS.HEADER_ID);
        if (extraField instanceof X000A_NTFS) {
            final X000A_NTFS ntfsTimestamp = (X000A_NTFS) extraField;
            final FileTime modifyTime = ntfsTimestamp.getModifyFileTime();
            if (modifyTime != null) {
                internalSetLastModifiedTime(modifyTime);
            }
            final FileTime accessTime = ntfsTimestamp.getAccessFileTime();
            if (accessTime != null) {
                super.setLastAccessTime(accessTime);
            }
            final FileTime creationTime = ntfsTimestamp.getCreateFileTime();
            if (creationTime != null) {
                super.setCreationTime(creationTime);
            }
        }
    }
}

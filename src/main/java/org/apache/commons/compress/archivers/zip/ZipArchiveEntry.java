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
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.EntryStreamOffsets;
import org.apache.commons.compress.utils.ByteUtils;

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
public class ZipArchiveEntry extends java.util.zip.ZipEntry
    implements ArchiveEntry, EntryStreamOffsets
{

    public static final int PLATFORM_UNIX = 3;
    public static final int PLATFORM_FAT  = 0;
    public static final int CRC_UNKNOWN = -1;
    private static final int SHORT_MASK = 0xFFFF;
    private static final int SHORT_SHIFT = 16;
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
    static final ZipArchiveEntry[] EMPTY_ZIP_ARCHIVE_ENTRY_ARRAY = new ZipArchiveEntry[0];

    /**
     * Creates a new zip entry with the specified name.
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
     * Creates a new zip entry with fields taken from the specified zip entry.
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
            setExtraFields(ExtraFieldUtils.parse(extra, true, ExtraFieldParsingMode.BEST_EFFORT));        } else {
            // initializes extra data to an empty byte array
            setExtra();
        }
        setMethod(entry.getMethod());
        this.size = entry.getSize();
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
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
        setExtraFields(getAllExtraFieldsNoCopy());
        setPlatform(entry.getPlatform());
        final GeneralPurposeBit other = entry.getGeneralPurposeBit();
        setGeneralPurposeBit(other == null ? null :
                             (GeneralPurposeBit) other.clone());
    }

    /**
     */
    protected ZipArchiveEntry() {
        this("");
    }

    /**
     * Creates a new zip entry taking some information from the given
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
        if (inputFile.isFile()){
            setSize(inputFile.length());
        }
        setTime(inputFile.lastModified());
        // TODO are there any other fields we can set here?
    }

    /**
     * Creates a new zip entry taking some information from the given
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
        if (Files.isRegularFile(inputPath, options)){
            setSize(Files.size(inputPath));
        }
        setTime(Files.getLastModifiedTime(inputPath, options));
        // TODO are there any other fields we can set here?
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
     * Sets the internal file attributes.
     * @param value an {@code int} value
     */
    public void setInternalAttributes(final int value) {
        internalAttributes = value;
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
     * Sets the external file attributes.
     * @param value an {@code long} value
     */
    public void setExternalAttributes(final long value) {
        externalAttributes = value;
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
     * Unix permission.
     * @return the unix permissions
     */
    public int getUnixMode() {
        return platform != PLATFORM_UNIX ? 0 :
            (int) ((getExternalAttributes() >> SHORT_SHIFT) & SHORT_MASK);
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
     * Set the platform (UNIX or FAT).
     * @param platform an {@code int} value - 0 is FAT, 3 is UNIX
     */
    protected void setPlatform(final int platform) {
        this.platform = platform;
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

    private ZipExtraField[] getParseableExtraFieldsNoCopy() {
        if (extraFields == null) {
            return ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY;
        }
        return extraFields;
    }

    private ZipExtraField[] getParseableExtraFields() {
        final ZipExtraField[] parseableExtraFields = getParseableExtraFieldsNoCopy();
        return (parseableExtraFields == extraFields) ? copyOf(parseableExtraFields, parseableExtraFields.length)
            : parseableExtraFields;
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

    private ZipExtraField[] getMergedFields() {
        final ZipExtraField[] zipExtraFields = copyOf(extraFields, extraFields.length + 1);
        zipExtraFields[extraFields.length] = unparseableExtra;
        return zipExtraFields;
    }

    private ZipExtraField[] getUnparseableOnly() {
        return unparseableExtra == null ? ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY : new ZipExtraField[] { unparseableExtra };
    }

    private ZipExtraField[] getAllExtraFields() {
        final ZipExtraField[] allExtraFieldsNoCopy = getAllExtraFieldsNoCopy();
        return (allExtraFieldsNoCopy == extraFields) ? copyOf(allExtraFieldsNoCopy, allExtraFieldsNoCopy.length)
            : allExtraFieldsNoCopy;
    }

    private ZipExtraField findUnparseable(final List<ZipExtraField> fs) {
        for (final ZipExtraField f : fs) {
            if (f instanceof UnparseableExtraFieldData) {
                return f;
            }
        }
        return null;
    }

    private ZipExtraField findMatching(final ZipShort headerId, final List<ZipExtraField> fs) {
        for (final ZipExtraField f : fs) {
            if (headerId.equals(f.getHeaderId())) {
                return f;
            }
        }
        return null;
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
        if (ze instanceof UnparseableExtraFieldData) {
            unparseableExtra = (UnparseableExtraFieldData) ze;
        } else if (extraFields == null) {
            extraFields = new ZipExtraField[]{ ze };
        } else {
            if (getExtraField(ze.getHeaderId()) != null) {
                removeExtraField(ze.getHeaderId());
            }
            final ZipExtraField[] zipExtraFields = copyOf(extraFields, extraFields.length + 1);
            zipExtraFields[zipExtraFields.length - 1] = ze;
            extraFields = zipExtraFields;
        }
        setExtra();
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
                removeExtraField(ze.getHeaderId());
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
     * Remove an extra field.
     * @param type the type of extra field to remove
     */
    public void removeExtraField(final ZipShort type) {
        if (extraFields == null) {
            throw new java.util.NoSuchElementException();
        }

        final List<ZipExtraField> newResult = new ArrayList<>();
        for (final ZipExtraField extraField : extraFields) {
            if (!type.equals(extraField.getHeaderId())) {
                newResult.add(extraField);
            }
        }
        if (extraFields.length == newResult.size()) {
            throw new java.util.NoSuchElementException();
        }
        extraFields = newResult.toArray(ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY);
        setExtra();
    }

    /**
     * Removes unparseable extra field data.
     *
     * @since 1.1
     */
    public void removeUnparseableExtraFieldData() {
        if (unparseableExtra == null) {
            throw new java.util.NoSuchElementException();
        }
        unparseableExtra = null;
        setExtra();
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
     * Looks up extra field data that couldn't be parsed correctly.
     *
     * @return null if no such field exists.
     *
     * @since 1.1
     */
    public UnparseableExtraFieldData getUnparseableExtraFieldData() {
        return unparseableExtra;
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
            final ZipExtraField[] local = ExtraFieldUtils.parse(extra, true, ExtraFieldParsingMode.BEST_EFFORT);
            mergeExtraFields(local, true);
        } catch (final ZipException e) {
            // actually this is not possible as of Commons Compress 1.1
            throw new RuntimeException("Error parsing extra fields for entry: " //NOSONAR
                                       + getName() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Unfortunately {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} seems to access the extra data
     * directly, so overriding getExtra doesn't help - we need to
     * modify super's data directly.
     */
    protected void setExtra() {
        super.setExtra(ExtraFieldUtils.mergeLocalFileDataData(getAllExtraFieldsNoCopy()));
    }

    /**
     * Sets the central directory part of extra fields.
     * @param b an array of bytes to be parsed into extra fields
     */
    public void setCentralDirectoryExtra(final byte[] b) {
        try {
            final ZipExtraField[] central = ExtraFieldUtils.parse(b, false, ExtraFieldParsingMode.BEST_EFFORT);
            mergeExtraFields(central, false);
        } catch (final ZipException e) {
            // actually this is not possible as of Commons Compress 1.19
            throw new RuntimeException(e.getMessage(), e); //NOSONAR
        }
    }

    /**
     * Retrieves the extra data for the local file data.
     * @return the extra data for local file
     */
    public byte[] getLocalFileDataExtra() {
        final byte[] extra = getExtra();
        return extra != null ? extra : ByteUtils.EMPTY_BYTE_ARRAY;
    }

    /**
     * Retrieves the extra data for the central directory.
     * @return the central directory extra data
     */
    public byte[] getCentralDirectoryExtra() {
        return ExtraFieldUtils.mergeCentralDirectoryData(getAllExtraFieldsNoCopy());
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
     * Is this entry a directory?
     * @return true if the entry is a directory
     */
    @Override
    public boolean isDirectory() {
        final String n = getName();
        return n != null && n.endsWith("/");
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

    protected long getLocalHeaderOffset() {
        return this.localHeaderOffset;
    }

    protected void setLocalHeaderOffset(final long localHeaderOffset) {
        this.localHeaderOffset = localHeaderOffset;
    }

    @Override
    public long getDataOffset() {
        return dataOffset;
    }

    /**
     * Sets the data offset.
     *
     * @param dataOffset
     *      new value of data offset.
     */
    protected void setDataOffset(final long dataOffset) {
        this.dataOffset = dataOffset;
    }

    @Override
    public boolean isStreamContiguous() {
        return isStreamContiguous;
    }

    protected void setStreamContiguous(final boolean isStreamContiguous) {
        this.isStreamContiguous = isStreamContiguous;
    }

    /**
     * Get the hashCode of the entry.
     * This uses the name as the hashcode.
     * @return a hashcode.
     */
    @Override
    public int hashCode() {
        // this method has severe consequences on performance. We cannot rely
        // on the super.hashCode() method since super.getName() always return
        // the empty string in the current implementation (there's no setter)
        // so it is basically draining the performance of a hashmap lookup
        final String n = getName();
        return (n == null ? "" : n).hashCode();
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
     * The "general purpose bit" field.
     * @param b the general purpose bit
     * @since 1.1
     */
    public void setGeneralPurposeBit(final GeneralPurposeBit b) {
        gpb = b;
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
                    addExtraField(element);
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
                        removeExtraField(existing.getHeaderId());
                        addExtraField(u);
                    }
                }
            }
            setExtra();
        }
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

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        return getTime() == other.getTime()
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

    /**
     * The "version required to expand" field.
     * @return "version required to expand" field
     * @since 1.11
     */
    public int getVersionRequired() {
        return versionRequired;
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
     * The content of the flags field.
     * @return content of the flags field
     * @since 1.11
     */
    public int getRawFlag() {
        return rawFlag;
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
     * The source of the name field value.
     * @return source of the name field value
     * @since 1.16
     */
    public NameSource getNameSource() {
        return nameSource;
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
     * The source of the comment field value.
     * @return source of the comment field value
     * @since 1.16
     */
    public CommentSource getCommentSource() {
        return commentSource;
    }

    /**
     * Sets the source of the comment field value.
     * @param commentSource source of the comment field value
     * @since 1.16
     */
    public void setCommentSource(final CommentSource commentSource) {
        this.commentSource = commentSource;
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
     * The number of the split segment this entry starts at.
     *
     * @param diskNumberStart the number of the split segment this entry starts at.
     * @since 1.20
     */
    public void setDiskNumberStart(final long diskNumberStart) {
        this.diskNumberStart = diskNumberStart;
    }

    private ZipExtraField[] copyOf(final ZipExtraField[] src, final int length) {
        final ZipExtraField[] cpy = new ZipExtraField[length];
        System.arraycopy(src, 0, cpy, 0, Math.min(src.length, length));
        return cpy;
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

        private final ExtraFieldUtils.UnparseableExtraField onUnparseableData;

        ExtraFieldParsingMode(final ExtraFieldUtils.UnparseableExtraField onUnparseableData) {
            this.onUnparseableData = onUnparseableData;
        }

        @Override
        public ZipExtraField onUnparseableExtraField(final byte[] data, final int off, final int len, final boolean local,
            final int claimedLength) throws ZipException {
            return onUnparseableData.onUnparseableExtraField(data, off, len, local, claimedLength);
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
    }
}

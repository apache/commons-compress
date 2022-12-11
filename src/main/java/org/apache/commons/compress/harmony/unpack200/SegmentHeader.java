/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.unpack200;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 * SegmentHeader is the header band of a {@link Segment}
 */
public class SegmentHeader {

    /**
     * The magic header for a Pack200 Segment is 0xCAFED00D. I wonder where they get their inspiration from ...
     */
    private static final int[] magic = {0xCA, 0xFE, 0xD0, 0x0D};

    private int archiveMajor;

    private int archiveMinor;

    private long archiveModtime;

    private long archiveSize;

    private int attributeDefinitionCount;

    private InputStream bandHeadersInputStream;

    private int bandHeadersSize;

    private int classCount;

    private int cpClassCount;

    private int cpDescriptorCount;

    private int cpDoubleCount;

    private int cpFieldCount;

    private int cpFloatCount;

    private int cpIMethodCount;

    private int cpIntCount;

    private int cpLongCount;

    private int cpMethodCount;

    private int cpSignatureCount;

    private int cpStringCount;

    private int cpUTF8Count;

    private int defaultClassMajorVersion;

    private int defaultClassMinorVersion;

    private int innerClassCount;

    private int numberOfFiles;

    private int segmentsRemaining;

    private SegmentOptions options;

    private final Segment segment;

    private int archiveSizeOffset;

    public SegmentHeader(final Segment segment) {
        this.segment = segment;
    }

    /**
     * Decode a scalar from the band file. A scalar is like a band, but does not perform any band code switching.
     *
     * @param name the name of the scalar (primarily for logging/debugging purposes)
     * @param in the input stream to read from
     * @param codec the codec for this scalar
     * @return the decoded value
     * @throws IOException if there is a problem reading from the underlying input stream
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid
     */
    private int decodeScalar(final String name, final InputStream in, final BHSDCodec codec)
        throws IOException, Pack200Exception {
        final int ret = codec.decode(in);
        segment.log(Segment.LOG_LEVEL_VERBOSE, "Parsed #" + name + " as " + ret);
        return ret;
    }

    /**
     * Decode a number of scalars from the band file. A scalar is like a band, but does not perform any band code
     * switching.
     *
     * @param name the name of the scalar (primarily for logging/debugging purposes)
     * @param in the input stream to read from
     * @param codec the codec for this scalar
     * @return an array of decoded {@code long[]} values
     * @throws IOException if there is a problem reading from the underlying input stream
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid
     */
    private int[] decodeScalar(final String name, final InputStream in, final BHSDCodec codec, final int n)
        throws IOException, Pack200Exception {
        segment.log(Segment.LOG_LEVEL_VERBOSE, "Parsed #" + name + " (" + n + ")");
        return codec.decodeInts(n, in);
    }

    public long getArchiveModtime() {
        return archiveModtime;
    }

    public long getArchiveSize() {
        return archiveSize;
    }

    public int getArchiveSizeOffset() {
        return archiveSizeOffset;
    }

    public int getAttributeDefinitionCount() {
        return attributeDefinitionCount;
    }

    /**
     * Obtain the band headers data as an input stream. If no band headers are present, this will return an empty input
     * stream to prevent any further reads taking place.
     *
     * Note that as a stream, data consumed from this input stream can't be re-used. Data is only read from this stream
     * if the encoding is such that additional information needs to be decoded from the stream itself.
     *
     * @return the band headers input stream
     */
    public InputStream getBandHeadersInputStream() {
        if (bandHeadersInputStream == null) {
            bandHeadersInputStream = new ByteArrayInputStream(new byte[0]);
        }
        return bandHeadersInputStream;

    }

    public int getBandHeadersSize() {
        return bandHeadersSize;
    }

    public int getClassCount() {
        return classCount;
    }

    public int getCpClassCount() {
        return cpClassCount;
    }

    public int getCpDescriptorCount() {
        return cpDescriptorCount;
    }

    public int getCpDoubleCount() {
        return cpDoubleCount;
    }

    public int getCpFieldCount() {
        return cpFieldCount;
    }

    public int getCpFloatCount() {
        return cpFloatCount;
    }

    public int getCpIMethodCount() {
        return cpIMethodCount;
    }

    public int getCpIntCount() {
        return cpIntCount;
    }

    public int getCpLongCount() {
        return cpLongCount;
    }

    public int getCpMethodCount() {
        return cpMethodCount;
    }

    public int getCpSignatureCount() {
        return cpSignatureCount;
    }

    public int getCpStringCount() {
        return cpStringCount;
    }

    public int getCpUTF8Count() {
        return cpUTF8Count;
    }

    public int getDefaultClassMajorVersion() {
        return defaultClassMajorVersion;
    }

    public int getDefaultClassMinorVersion() {
        return defaultClassMinorVersion;
    }

    public int getInnerClassCount() {
        return innerClassCount;
    }

    public int getNumberOfFiles() {
        return numberOfFiles;
    }

    public SegmentOptions getOptions() {
        return options;
    }

    public int getSegmentsRemaining() {
        return segmentsRemaining;
    }

    private void parseArchiveFileCounts(final InputStream in) throws IOException, Pack200Exception {
        if (options.hasArchiveFileCounts()) {
            setArchiveSize((long) decodeScalar("archive_size_hi", in, Codec.UNSIGNED5) << 32 |
                decodeScalar("archive_size_lo", in, Codec.UNSIGNED5));
            archiveSizeOffset = in.available();
            setSegmentsRemaining(decodeScalar("archive_next_count", in, Codec.UNSIGNED5));
            setArchiveModtime(decodeScalar("archive_modtime", in, Codec.UNSIGNED5));
            numberOfFiles = decodeScalar("file_count", in, Codec.UNSIGNED5);
        }
    }

    private void parseArchiveSpecialCounts(final InputStream in) throws IOException, Pack200Exception {
        if (getOptions().hasSpecialFormats()) {
            bandHeadersSize = decodeScalar("band_headers_size", in, Codec.UNSIGNED5);
            setAttributeDefinitionCount(decodeScalar("attr_definition_count", in, Codec.UNSIGNED5));
        }
    }

    private void parseClassCounts(final InputStream in) throws IOException, Pack200Exception {
        innerClassCount = decodeScalar("ic_count", in, Codec.UNSIGNED5);
        defaultClassMinorVersion = decodeScalar("default_class_minver", in, Codec.UNSIGNED5);
        defaultClassMajorVersion = decodeScalar("default_class_majver", in, Codec.UNSIGNED5);
        classCount = decodeScalar("class_count", in, Codec.UNSIGNED5);
    }

    private void parseCpCounts(final InputStream in) throws IOException, Pack200Exception {
        cpUTF8Count = decodeScalar("cp_Utf8_count", in, Codec.UNSIGNED5);
        if (getOptions().hasCPNumberCounts()) {
            cpIntCount = decodeScalar("cp_Int_count", in, Codec.UNSIGNED5);
            cpFloatCount = decodeScalar("cp_Float_count", in, Codec.UNSIGNED5);
            cpLongCount = decodeScalar("cp_Long_count", in, Codec.UNSIGNED5);
            cpDoubleCount = decodeScalar("cp_Double_count", in, Codec.UNSIGNED5);
        }
        cpStringCount = decodeScalar("cp_String_count", in, Codec.UNSIGNED5);
        cpClassCount = decodeScalar("cp_Class_count", in, Codec.UNSIGNED5);
        cpSignatureCount = decodeScalar("cp_Signature_count", in, Codec.UNSIGNED5);
        cpDescriptorCount = decodeScalar("cp_Descr_count", in, Codec.UNSIGNED5);
        cpFieldCount = decodeScalar("cp_Field_count", in, Codec.UNSIGNED5);
        cpMethodCount = decodeScalar("cp_Method_count", in, Codec.UNSIGNED5);
        cpIMethodCount = decodeScalar("cp_Imethod_count", in, Codec.UNSIGNED5);
    }

    public void read(final InputStream in) throws IOException, Error, Pack200Exception {

        final int[] word = decodeScalar("archive_magic_word", in, Codec.BYTE1, magic.length);
        for (int m = 0; m < magic.length; m++) {
            if (word[m] != magic[m]) {
                throw new Error("Bad header");
            }
        }
        setArchiveMinorVersion(decodeScalar("archive_minver", in, Codec.UNSIGNED5));
        setArchiveMajorVersion(decodeScalar("archive_majver", in, Codec.UNSIGNED5));
        options = new SegmentOptions(decodeScalar("archive_options", in, Codec.UNSIGNED5));
        parseArchiveFileCounts(in);
        parseArchiveSpecialCounts(in);
        parseCpCounts(in);
        parseClassCounts(in);

        if (getBandHeadersSize() > 0) {
            final byte[] bandHeaders = new byte[getBandHeadersSize()];
            readFully(in, bandHeaders);
            setBandHeadersData(bandHeaders);
        }

        archiveSizeOffset = archiveSizeOffset - in.available();
    }

    /**
     * Completely reads in a byte array, akin to the implementation in {@link java.lang.DataInputStream}. TODO Refactor
     * out into a separate InputStream handling class
     *
     * @param in the input stream to read from
     * @param data the byte array to read into
     * @throws IOException if a problem occurs during reading from the underlying stream
     */
    private void readFully(final InputStream in, final byte[] data) throws IOException {
        int total = in.read(data);
        if (total == -1) {
            throw new EOFException("Failed to read any data from input stream");
        }
        while (total < data.length) {
            final int delta = in.read(data, total, data.length - total);
            if (delta == -1) {
                throw new EOFException("Failed to read some data from input stream");
            }
            total += delta;
        }
    }

    /**
     * Sets the major version of this archive.
     *
     * @param version the minor version of the archive
     * @throws Pack200Exception if the major version is not 150
     */
    private void setArchiveMajorVersion(final int version) throws Pack200Exception {
        if (version != 150) {
            throw new Pack200Exception("Invalid segment major version: " + version);
        }
        archiveMajor = version;
    }

    /**
     * Sets the minor version of this archive
     *
     * @param version the minor version of the archive
     * @throws Pack200Exception if the minor version is not 7
     */
    private void setArchiveMinorVersion(final int version) throws Pack200Exception {
        if (version != 7) {
            throw new Pack200Exception("Invalid segment minor version");
        }
        archiveMinor = version;
    }

    public void setArchiveModtime(final long archiveModtime) {
        this.archiveModtime = archiveModtime;
    }

    public void setArchiveSize(final long archiveSize) {
        this.archiveSize = archiveSize;
    }

    private void setAttributeDefinitionCount(final long valuie) {
        this.attributeDefinitionCount = (int) valuie;
    }

    private void setBandHeadersData(final byte[] bandHeaders) {
        this.bandHeadersInputStream = new ByteArrayInputStream(bandHeaders);
    }

    public void setSegmentsRemaining(final long value) {
        segmentsRemaining = (int) value;
    }

    public void unpack() {

    }
}

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
package org.apache.commons.compress.harmony.pack200;

import java.io.IOException;
import java.io.OutputStream;

/**
 * SegmentHeader is the header band of a {@link Segment}. Corresponds to {@code segment_header} in the pack200 specification.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class SegmentHeader extends BandSet {

    /**
     * Counter for major/minor class file numbers, so we can work out the default
     */
    private static final class Counter {

        private final int[] objs = new int[8];
        private final int[] counts = new int[8];
        private int length;

        public void add(final int obj) {
            boolean found = false;
            for (int i = 0; i < length; i++) {
                if (objs[i] == obj) {
                    counts[i]++;
                    found = true;
                }
            }
            if (!found) {
                objs[length] = obj;
                counts[length] = 1;
                length++;
                if (length > objs.length - 1) {
                    final Object[] newArray = new Object[objs.length + 8];
                    System.arraycopy(objs, 0, newArray, 0, length);
                }
            }
        }

        public int getMostCommon() {
            int returnIndex = 0;
            for (int i = 0; i < length; i++) {
                if (counts[i] > counts[returnIndex]) {
                    returnIndex = i;
                }
            }
            return objs[returnIndex];
        }
    }

    private static final int[] magic = { 0xCA, 0xFE, 0xD0, 0x0D };
    private static final int archive_minver = 7;
    private static final int archive_majver = 150;

    private int archive_options;

    private int cp_Utf8_count;
    private int cp_Int_count;
    private int cp_Float_count;
    private int cp_Long_count;
    private int cp_Double_count;
    private int cp_String_count;
    private int cp_Class_count;
    private int cp_Signature_count;
    private int cp_Descr_count;
    private int cp_Field_count;
    private int cp_Method_count;
    private int cp_Imethod_count;

    private int attribute_definition_count;
    private final IntList band_headers = new IntList();

    private boolean have_all_code_flags = true; // true by default

    private int archive_size_hi;
    private int archive_size_lo;
    private int archive_next_count;
    private int archive_modtime;
    private int file_count;

    private boolean deflate_hint;
    private final boolean have_file_modtime = true;
    private final boolean have_file_options = true;
    private boolean have_file_size_hi;
    private boolean have_class_flags_hi;
    private boolean have_field_flags_hi;
    private boolean have_method_flags_hi;
    private boolean have_code_flags_hi;

    private int ic_count;
    private int class_count;
    private final Counter majverCounter = new Counter();

    /**
     * Constructs a new SegmentHeader
     */
    public SegmentHeader() {
        super(1, null); // Pass 1 for effort because bands in the segment header
                        // should always use the default encoding
    }

    /**
     * Adds a major version.
     *
     * @param major the major version.
     */
    public void addMajorVersion(final int major) {
        majverCounter.add(major);
    }

    /**
     * Appends a band coding specifier.
     *
     * @param specifier the specifier.
     */
    public void appendBandCodingSpecifier(final int specifier) {
        band_headers.add(specifier);
    }

    private void calculateArchiveOptions() {
        if (attribute_definition_count > 0 || band_headers.size() > 0) {
            archive_options |= 1;
        }
        if (cp_Int_count > 0 || cp_Float_count > 0 || cp_Long_count > 0 || cp_Double_count > 0) {
            archive_options |= 1 << 1;
        }
        if (have_all_code_flags) {
            archive_options |= 1 << 2;
        }
        if (file_count > 0) {
            archive_options |= 1 << 4;
        }
        if (deflate_hint) {
            archive_options |= 1 << 5;
        }
        if (have_file_modtime) {
            archive_options |= 1 << 6;
        }
        if (have_file_options) {
            archive_options |= 1 << 7;
        }
        if (have_file_size_hi) {
            archive_options |= 1 << 8;
        }
        if (have_class_flags_hi) {
            archive_options |= 1 << 9;
        }
        if (have_field_flags_hi) {
            archive_options |= 1 << 10;
        }
        if (have_method_flags_hi) {
            archive_options |= 1 << 11;
        }
        if (have_code_flags_hi) {
            archive_options |= 1 << 12;
        }
    }

    /**
     * Gets the archive modification time.
     *
     * @return the archive modification time.
     */
    public int getArchive_modtime() {
        return archive_modtime;
    }

    /**
     * Gets the default major version.
     *
     * @return the default major version.
     */
    public int getDefaultMajorVersion() {
        return majverCounter.getMostCommon();
    }

    /**
     * Tests whether all code flags are present.
     *
     * @return true if all code flags are present.
     */
    public boolean have_all_code_flags() {
        return have_all_code_flags;
    }

    /**
     * Tests whether class flags hi are present.
     *
     * @return true if class flags hi are present.
     */
    public boolean have_class_flags_hi() {
        return have_class_flags_hi;
    }

    /**
     * Tests whether code flags hi are present.
     *
     * @return true if code flags hi are present.
     */
    public boolean have_code_flags_hi() {
        return have_code_flags_hi;
    }

    /**
     * Tests whether field flags hi are present.
     *
     * @return true if field flags hi are present.
     */
    public boolean have_field_flags_hi() {
        return have_field_flags_hi;
    }

    /**
     * Tests whether file modtime is present.
     *
     * @return true if file modtime is present.
     */
    public boolean have_file_modtime() {
        return have_file_modtime;
    }

    /**
     * Tests whether file options are present.
     *
     * @return true if file options are present.
     */
    public boolean have_file_options() {
        return have_file_options;
    }

    /**
     * Tests whether file size hi is present.
     *
     * @return true if file size hi is present.
     */
    public boolean have_file_size_hi() {
        return have_file_size_hi;
    }

    /**
     * Tests whether method flags hi are present.
     *
     * @return true if method flags hi are present.
     */
    public boolean have_method_flags_hi() {
        return have_method_flags_hi;
    }

    /**
     * Encode and write the SegmentHeader bands to the OutputStream.
     *
     * @param out the output stream.
     * @throws IOException if an I/O error occurs.
     * @throws Pack200Exception if a Pack200 error occurs.
     */
    @Override
    public void pack(final OutputStream out) throws IOException, Pack200Exception {
        out.write(encodeScalar(magic, Codec.BYTE1));
        out.write(encodeScalar(archive_minver, Codec.UNSIGNED5));
        out.write(encodeScalar(archive_majver, Codec.UNSIGNED5));
        calculateArchiveOptions();
        out.write(encodeScalar(archive_options, Codec.UNSIGNED5));
        writeArchiveFileCounts(out);
        writeArchiveSpecialCounts(out);
        writeCpCounts(out);
        writeClassCounts(out);
        if (band_headers.size() > 0) {
            out.write(encodeScalar(band_headers.toArray(), Codec.BYTE1));
        }
    }

    /**
     * Sets the attribute definition count.
     *
     * @param attribute_definition_count the count.
     */
    public void setAttribute_definition_count(final int attribute_definition_count) {
        this.attribute_definition_count = attribute_definition_count;
    }

    /**
     * Sets the class count.
     *
     * @param class_count the count.
     */
    public void setClass_count(final int class_count) {
        this.class_count = class_count;
    }

    /**
     * Sets the CP Class count.
     *
     * @param count the count.
     */
    public void setCp_Class_count(final int count) {
        cp_Class_count = count;
    }

    /**
     * Sets the CP Descr count.
     *
     * @param count the count.
     */
    public void setCp_Descr_count(final int count) {
        cp_Descr_count = count;
    }

    /**
     * Sets the CP Double count.
     *
     * @param count the count.
     */
    public void setCp_Double_count(final int count) {
        cp_Double_count = count;
    }

    /**
     * Sets the CP Field count.
     *
     * @param count the count.
     */
    public void setCp_Field_count(final int count) {
        cp_Field_count = count;
    }

    /**
     * Sets the CP Float count.
     *
     * @param count the count.
     */
    public void setCp_Float_count(final int count) {
        cp_Float_count = count;
    }

    /**
     * Sets the CP Imethod count.
     *
     * @param count the count.
     */
    public void setCp_Imethod_count(final int count) {
        cp_Imethod_count = count;
    }

    /**
     * Sets the CP Int count.
     *
     * @param count the count.
     */
    public void setCp_Int_count(final int count) {
        cp_Int_count = count;
    }

    /**
     * Sets the CP Long count.
     *
     * @param count the count.
     */
    public void setCp_Long_count(final int count) {
        cp_Long_count = count;
    }

    /**
     * Sets the CP Method count.
     *
     * @param count the count.
     */
    public void setCp_Method_count(final int count) {
        cp_Method_count = count;
    }

    /**
     * Sets the CP Signature count.
     *
     * @param count the count.
     */
    public void setCp_Signature_count(final int count) {
        cp_Signature_count = count;
    }

    /**
     * Sets the CP String count.
     *
     * @param count the count.
     */
    public void setCp_String_count(final int count) {
        cp_String_count = count;
    }

    /**
     * Sets the CP UTF8 count.
     *
     * @param count the count.
     */
    public void setCp_Utf8_count(final int count) {
        cp_Utf8_count = count;
    }

    /**
     * Sets the deflate hint.
     *
     * @param deflate_hint the deflate hint.
     */
    public void setDeflate_hint(final boolean deflate_hint) {
        this.deflate_hint = deflate_hint;
    }

    /**
     * Sets the file count.
     *
     * @param file_count the count.
     */
    public void setFile_count(final int file_count) {
        this.file_count = file_count;
    }

    /**
     * Sets whether all code flags are present.
     *
     * @param have_all_code_flags true if all code flags are present.
     */
    public void setHave_all_code_flags(final boolean have_all_code_flags) {
        this.have_all_code_flags = have_all_code_flags;
    }

    /**
     * Sets whether class flags hi are present.
     *
     * @param have_class_flags_hi true if class flags hi are present.
     */
    public void setHave_class_flags_hi(final boolean have_class_flags_hi) {
        this.have_class_flags_hi = have_class_flags_hi;
    }

    /**
     * Sets whether code flags hi are present.
     *
     * @param have_code_flags_hi true if code flags hi are present.
     */
    public void setHave_code_flags_hi(final boolean have_code_flags_hi) {
        this.have_code_flags_hi = have_code_flags_hi;
    }

    /**
     * Sets whether field flags hi are present.
     *
     * @param have_field_flags_hi true if field flags hi are present.
     */
    public void setHave_field_flags_hi(final boolean have_field_flags_hi) {
        this.have_field_flags_hi = have_field_flags_hi;
    }

    /**
     * Sets whether method flags hi are present.
     *
     * @param have_method_flags_hi true if method flags hi are present.
     */
    public void setHave_method_flags_hi(final boolean have_method_flags_hi) {
        this.have_method_flags_hi = have_method_flags_hi;
    }

    /**
     * Sets the IC count.
     *
     * @param ic_count the count.
     */
    public void setIc_count(final int ic_count) {
        this.ic_count = ic_count;
    }

    private void writeArchiveFileCounts(final OutputStream out) throws IOException, Pack200Exception {
        if ((archive_options & 1 << 4) > 0) { // have_file_headers
            out.write(encodeScalar(archive_size_hi, Codec.UNSIGNED5));
            out.write(encodeScalar(archive_size_lo, Codec.UNSIGNED5));
            out.write(encodeScalar(archive_next_count, Codec.UNSIGNED5));
            out.write(encodeScalar(archive_modtime, Codec.UNSIGNED5));
            out.write(encodeScalar(file_count, Codec.UNSIGNED5));
        }
    }

    private void writeArchiveSpecialCounts(final OutputStream out) throws IOException, Pack200Exception {
        if ((archive_options & 1) > 0) { // have_special_formats
            out.write(encodeScalar(band_headers.size(), Codec.UNSIGNED5));
            out.write(encodeScalar(attribute_definition_count, Codec.UNSIGNED5));
        }
    }

    private void writeClassCounts(final OutputStream out) throws IOException, Pack200Exception {
        final int default_class_minver = 0;
        final int default_class_majver = majverCounter.getMostCommon();
        out.write(encodeScalar(ic_count, Codec.UNSIGNED5));
        out.write(encodeScalar(default_class_minver, Codec.UNSIGNED5));
        out.write(encodeScalar(default_class_majver, Codec.UNSIGNED5));
        out.write(encodeScalar(class_count, Codec.UNSIGNED5));
    }

    private void writeCpCounts(final OutputStream out) throws IOException, Pack200Exception {
        out.write(encodeScalar(cp_Utf8_count, Codec.UNSIGNED5));
        if ((archive_options & 1 << 1) != 0) { // have_cp_numbers
            out.write(encodeScalar(cp_Int_count, Codec.UNSIGNED5));
            out.write(encodeScalar(cp_Float_count, Codec.UNSIGNED5));
            out.write(encodeScalar(cp_Long_count, Codec.UNSIGNED5));
            out.write(encodeScalar(cp_Double_count, Codec.UNSIGNED5));
        }
        out.write(encodeScalar(cp_String_count, Codec.UNSIGNED5));
        out.write(encodeScalar(cp_Class_count, Codec.UNSIGNED5));
        out.write(encodeScalar(cp_Signature_count, Codec.UNSIGNED5));
        out.write(encodeScalar(cp_Descr_count, Codec.UNSIGNED5));
        out.write(encodeScalar(cp_Field_count, Codec.UNSIGNED5));
        out.write(encodeScalar(cp_Method_count, Codec.UNSIGNED5));
        out.write(encodeScalar(cp_Imethod_count, Codec.UNSIGNED5));
    }

}

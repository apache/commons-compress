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

package org.apache.commons.compress.compressors.gzip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.gzip.ExtraField.SubField;

/**
 * If the {@code FLG.FEXTRA} bit is set, an "extra field" is present in the header, with total length XLEN bytes.
 *
 * <pre>
 * +---+---+=================================+
 * | XLEN  |...XLEN bytes of "extra field"...| (more...)
 * +---+---+=================================+
 * </pre>
 * <p>
 * This class represents the extra field payload (excluding the XLEN 2 bytes). The ExtraField payload consists of a series of subfields, each of the form:
 * </p>
 *
 * <pre>
 * +---+---+---+---+==================================+
 * |SI1|SI2|  LEN  |... LEN bytes of subfield data ...|
 * +---+---+---+---+==================================+
 * </pre>
 * <p>
 * This class does not expose the internal subfields list to prevent adding subfields without total extra length validation. The class is iterable, but this
 * iterator is immutable.
 * </p>
 * <p>
 * Additional documentation on extra fields in use:
 * </p>
 * <ul>
 * <li><a href="https://samtools.github.io/hts-specs/SAMv1.pdf">Sequence Alignment/Map Format Specification</a>: The BGZF compression format defines the extra
 * field used by BGZF uses the two subfield ID values 66 and 67 (ASCII 'BC').</li>
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952 GZIP File Format Specification</a>
 * @since 1.28.0
 */
public final class ExtraField implements Iterable<SubField> {

    /**
     * If the {@code FLG.FEXTRA} bit is set, an "extra field" is present in the header, with total length XLEN bytes. It consists of a series of subfields, each
     * of the form:
     *
     * <pre>
     * +---+---+---+---+==================================+
     * |SI1|SI2|  LEN  |... LEN bytes of subfield data ...|
     * +---+---+---+---+==================================+
     * </pre>
     * <p>
     * The reserved IDs are:
     * </p>
     *
     * <pre>
     * SI1         SI2         Data
     * ----------  ----------  ----
     * 0x41 ('A')  0x70 ('P')  Apollo file type information
     * </pre>
     * <p>
     * Subfield IDs with {@code SI2 = 0} are reserved for future use.
     * </p>
     * <p>
     * LEN gives the length of the subfield data, excluding the 4 initial bytes.
     * </p>
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952 GZIP File Format Specification</a>
     */
    public static final class SubField {

        private final byte si1;
        private final byte si2;
        private final byte[] payload;

        SubField(final byte si1, final byte si2, final byte[] payload) {
            this.si1 = si1;
            this.si2 = si2;
            this.payload = payload;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SubField other = (SubField) obj;
            return Arrays.equals(payload, other.payload) && si1 == other.si1 && si2 == other.si2;
        }

        /**
         * The 2 character ISO-8859-1 string made from the si1 and si2 bytes of the sub field id.
         *
         * @return Two character ID.
         */
        public String getId() {
            return String.valueOf(new char[] { (char) (si1 & 0xff), (char) (si2 & 0xff) });
        }

        /**
         * The subfield payload.
         *
         * @return The payload.
         */
        public byte[] getPayload() {
            return payload;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(payload);
            result = prime * result + Objects.hash(si1, si2);
            return result;
        }
    }

    private static final int MAX_SIZE = 0xFFFF;

    private static final byte[] ZERO_BYTES = {};

    /**
     * Converts {@code XLEN} length bytes of "extra field" into an new instance.
     * <p>
     * The bytes for the {@code XLEN} field is not included in the input.
     * </p>
     * The ExtraField payload consists of a series of subfields, each of the form:
     *
     * <pre>
     * +---+---+---+---+==================================+
     * |SI1|SI2|  LEN  |... LEN bytes of subfield data ...| (repeat for other subfields)
     * +---+---+---+---+==================================+
     * </pre>
     *
     * @param bytes without the {@code XLEN} field.
     * @return a new instance.
     * @throws CompressorException Thrown for a formatting problem.
     */
    static ExtraField fromBytes(final byte[] bytes) throws CompressorException {
        if (bytes == null) {
            return null;
        }
        final ExtraField extra = new ExtraField();
        int pos = 0;
        while (pos <= bytes.length - 4) {
            final byte si1 = bytes[pos++];
            final byte si2 = bytes[pos++];
            final int sublen = bytes[pos++] & 0xff | (bytes[pos++] & 0xff) << 8;
            if (sublen > bytes.length - pos) {
                throw new CompressorException("Extra subfield length exceeds remaining bytes at subfield id = '%s%s': %,d > %,d", (char) (si1 & 0xff),
                        (char) (si2 & 0xff), sublen, bytes.length - pos);
            }
            final byte[] payload = new byte[sublen];
            System.arraycopy(bytes, pos, payload, 0, sublen);
            pos += sublen;
            extra.subFields.add(new SubField(si1, si2, payload));
            extra.totalSize = pos;
        }
        if (pos < bytes.length) {
            throw new CompressorException("%,d remaining bytes not used to parse an extra subfield.", bytes.length - pos);
        }
        return extra;
    }

    private final List<SubField> subFields = new ArrayList<>();

    private int totalSize;

    /**
     * Constructs a new instance.
     */
    public ExtraField() {
    }

    /**
     * Append a subfield by a 2-chars ISO-8859-1 string. The char at index 0 and 1 are respectively si1 and si2 (subfield id 1 and 2).
     *
     * @param id      The subfield ID.
     * @param payload The subfield payload.
     * @return this instance.
     * @throws NullPointerException     if {@code id} is {@code null}.
     * @throws NullPointerException     if {@code payload} is {@code null}.
     * @throws IllegalArgumentException if the subfield is not 2 characters or the payload is null
     * @throws IOException              if appending this subfield would exceed the max size 65535 of the extra header.
     */
    public ExtraField addSubField(final String id, final byte[] payload) throws IOException {
        Objects.requireNonNull(id, "payload");
        Objects.requireNonNull(payload, "payload");
        if (id.length() != 2) {
            throw new IllegalArgumentException("Subfield id must be a 2 character ISO-8859-1 string.");
        }
        final char si1 = id.charAt(0);
        final char si2 = id.charAt(1);
        if ((si1 & 0xff00) != 0 || (si2 & 0xff00) != 0) {
            throw new IllegalArgumentException("Subfield id must be a 2 character ISO-8859-1 string.");
        }
        final SubField f = new SubField((byte) (si1 & 0xff), (byte) (si2 & 0xff), payload);
        final int len = 4 + payload.length;
        if (totalSize + len > MAX_SIZE) {
            throw new CompressorException("Extra subfield '%s' too big (extras total size is already at %,d", f.getId(), totalSize);
        }
        subFields.add(f);
        totalSize += len;
        return this;
    }

    /**
     * Removes all subfields from this instance.
     */
    public void clear() {
        subFields.clear();
        totalSize = 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ExtraField other = (ExtraField) obj;
        return Objects.equals(subFields, other.subFields) && totalSize == other.totalSize;
    }

    /**
     * Finds the first subfield that matched the id if found, null otherwise.
     *
     * @param id The ID to find.
     * @return The first SubField that matched or null.
     */
    public SubField findFirstSubField(final String id) {
        return subFields.stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Gets the size in bytes of the encoded extra field. This does not include its own 16 bits size when embeded in the gzip header. For N sub fields,
     * the total is all subfields payloads bytes + 4N.
     *
     * @return the bytes count of this extra payload when encoded.
     */
    public int getEncodedSize() {
        return totalSize;
    }

    /**
     * Gets the subfield at the given index.
     *
     * @param index index of the element to return.
     * @return the subfield at the specified position in this list.
     * @throws IndexOutOfBoundsException if the index is out of range ({@code index &lt; 0 || index &gt;= size()}).
     */
    public SubField getSubField(final int index) {
        return subFields.get(index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subFields, totalSize);
    }

    /**
     * Tests is this extra field has no subfields.
     *
     * @return true if there are no subfields, false otherwise.
     */
    public boolean isEmpty() {
        return subFields.isEmpty();
    }

    /**
     * Returns an unmodifiable iterator over the elements in the SubField list in proper sequence.
     *
     * @return an unmodifiable naturally ordered iterator over the SubField elements.
     */
    @Override
    public Iterator<SubField> iterator() {
        return Collections.unmodifiableList(subFields).iterator();
    }

    /**
     * Gets the count of subfields currently in in this extra field.
     *
     * @return the count of subfields contained in this instance.
     */
    public int size() {
        return subFields.size();
    }

    byte[] toByteArray() {
        if (subFields.isEmpty()) {
            return ZERO_BYTES;
        }
        final byte[] ba = new byte[totalSize];
        int pos = 0;
        for (final SubField f : subFields) {
            ba[pos++] = f.si1;
            ba[pos++] = f.si2;
            ba[pos++] = (byte) (f.payload.length & 0xff); // little endian expected
            ba[pos++] = (byte) (f.payload.length >>> 8);
            System.arraycopy(f.payload, 0, ba, pos, f.payload.length);
            pos += f.payload.length;
        }
        return ba;
    }

}

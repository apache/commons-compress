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

package org.apache.commons.compress.compressors.gzip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * If the {@code FLG.FEXTRA} bit is set, an "extra field" is present in the header, with total length XLEN bytes. It consists of a series of subfields, each of
 * the form:
 *
 * <pre>
 * +---+---+---+---+==================================+
 * |SI1|SI2|  LEN  |... LEN bytes of subfield data ...|
 * +---+---+---+---+==================================+
 * </pre>
 *
 * This class does not expose the internal subfields list to prevent adding subfields without total extra length validation. However a copy of the list is
 * available.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952 GZIP File Format Specification</a>
 * @since 1.28.0
 */
public class HeaderExtraField {

    /**
     * If the {@code FLG.FEXTRA} bit is set, an "extra field" is present in the header, with total length XLEN bytes. It consists of a series of subfields, each
     * of the form:
     *
     * <pre>
     * +---+---+---+---+==================================+
     * |SI1|SI2|  LEN  |... LEN bytes of subfield data ...|
     * +---+---+---+---+==================================+
     * </pre>
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952 GZIP File Format Specification</a>
     */
    public static class SubField {

        private final byte si1;
        private final byte si2;
        private final byte[] payload;

        SubField(final byte si1, final byte si2, final byte[] payload) {
            this.si1 = si1;
            this.si2 = si2;
            this.payload = payload;
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
    }

    static final int MAX_SIZE = 0xFFFF;

    static final byte[] ZERO_BYTES = {};

    static HeaderExtraField fromBytes(final byte[] ba) throws IOException {
        if (ba == null) {
            return null;
        }
        final HeaderExtraField extra = new HeaderExtraField();
        int pos = 0;
        while (pos <= ba.length - 4) {
            final byte si1 = ba[pos++];
            final byte si2 = ba[pos++];
            final int sublen = ba[pos++] & 0xff | (ba[pos++] & 0xff) << 8;
            if (sublen > ba.length - pos) {
                throw new IOException("Extra subfield lenght exceeds remaining bytes in extra: " + sublen + " > " + (ba.length - pos));
            }
            final byte[] payload = new byte[sublen];
            System.arraycopy(ba, pos, payload, 0, sublen);
            pos += sublen;
            extra.subFields.add(new SubField(si1, si2, payload));
            extra.totalSize = pos;
        }
        if (pos < ba.length) {
            throw new IOException("" + (ba.length - pos) + " remaining bytes not used to parse an extra subfield.");
        }
        return extra;
    }

    private final List<SubField> subFields = new ArrayList<>();

    private int totalSize = 0;

    /**
     * Constructs a new instance.
     */
    public HeaderExtraField() {
    }

    /**
     * Append a subfield by a 2-chars ISO-8859-1 string. The char at index 0 and 1 are respectiovely si1 and si2 (subfield id 1 and 2).
     *
     * @param id The subfield ID.
     * @param payload The subfield payload.
     * @return this instance.
     * @throws NullPointerException if {@code id} is {@code null}.
     * @throws NullPointerException if {@code payload} is {@code null}.
     * @throws IllegalArgumentException if the subfield is not 2-chars or the payload is null
     * @throws IOException              if appending this subfield would exceed the max size 65535 of the extra header.
     */
    public HeaderExtraField addSubField(final String id, final byte[] payload) throws IOException {
        Objects.requireNonNull(id, "payload");
        Objects.requireNonNull(payload, "payload");
        if (id.length() != 2) {
            throw new IllegalArgumentException("Subfield id must be a 2-chars ISO-8859-1 string.");
        }
        final char si1 = id.charAt(0);
        final char si2 = id.charAt(1);
        if ((si1 & 0xff00) != 0 || (si2 & 0xff00) != 0) {
            throw new IllegalArgumentException("Subfield id must be a 2-chars ISO-8859-1 string.");
        }
        final SubField f = new SubField((byte) (si1 & 0xff), (byte) (si2 & 0xff), payload);
        final int len = 4 + payload.length;
        if (totalSize + len > MAX_SIZE) {
            throw new IOException("Extra subfield '" + f.getId() + "' too big (extras total size is already at " + totalSize + ")");
        }
        subFields.add(f);
        totalSize += len;
        return this;
    }

    /**
     * Gets the subfield at the given index.
     *
     * @param index index of the element to return
     * @return the subfield at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range ({@code index &lt; 0 || index &gt;= size()})
     */
    public SubField getSubFieldAt(final int index) {
        return subFields.get(index);
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

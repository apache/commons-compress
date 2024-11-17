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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Container for subfields.
 *
 * This class does not expose the internal subfields list to prevent adding
 * subfields without total extra length validation. However a copy of the list
 * is available.
 *
 * @since 1.28.0
 */
public class Extra {

    /**
     * The carrier for a subfield in the gzip extra.
     */
    public static class SubField {
        byte si1;
        byte si2;
        byte[] payload;

        SubField() {
        }

        SubField(byte si1, byte si2, byte[] payload) {
            this.si1 = si1;
            this.si2 = si2;
            this.payload = payload;
        }

        /**
         * The 2 char iso-8859-1 string made from the si1 and si2 bytes of the sub field
         * id.
         */
        public String getId() {
            return "" + ((char) (si1 & 0xff)) + ((char) (si2 & 0xff));
        }

        /**
         * The subfield payload.
         */
        public byte[] getPayload() {
            return payload;
        }
    }
    static final int MAX_SIZE = 0xFFFF;

    static final byte[] ZERO_BYTES = new byte[0];

    // --------------

    static Extra fromBytes(byte[] ba) throws IOException {
        if (ba == null) {
            return null;
        }

        Extra e = new Extra();

        int pos = 0;
        while (pos <= (ba.length - 4)) {
            SubField f = new SubField();
            f.si1 = ba[pos++];
            f.si2 = ba[pos++];

            int sublen = (ba[pos++] & 0xff) | ((ba[pos++] & 0xff) << 8);
            if (sublen > (ba.length - pos)) {
                throw new IOException("Extra subfield lenght exceeds remaining bytes in extra: " + sublen + " > " + (ba.length - pos));
            }

            f.payload = new byte[sublen];
            System.arraycopy(ba, pos, f.payload, 0, sublen);
            pos += sublen;

            e.fieldsList.add(f);
            e.totalSize = pos;
        }

        if (pos < ba.length) {
            throw new IOException("" + (ba.length - pos) + " remaining bytes not used to parse an extra subfield.");
        }

        return e;
    }
    private final List<SubField> fieldsList = new ArrayList<>();

    private int totalSize = 0;

    public Extra() {
    }

    /**
     * Append a subfield by a 2-chars ISO-8859-1 string. The char at index 0 and 1
     * are respectiovely si1 and si2 (subfield id 1 and 2).
     *
     * @throws IllegalArgumentException if the subfield is not 2-chars or the
     *                                  payload is null
     *
     * @throws IOException              if appending this subfield would exceed the
     *                                  max size 65535 of the extra header.
     */
    public Extra appendSubField(String subfieldId, byte[] payload) throws IOException {
        if (subfieldId.length() != 2) {
            throw new IllegalArgumentException("subfield id must be a 2-chars iso-8859-1 string.");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload was null");
        }

        char si1 = subfieldId.charAt(0);
        char si2 = subfieldId.charAt(1);
        if ((si1 & 0xff00) != 0 || (si2 & 0xff00) != 0) {
            throw new IllegalArgumentException("subfield id must be a 2-chars iso-8859-1 string.");
        }

        SubField f = new SubField((byte) (si1 & 0xff), (byte) (si2 & 0xff), payload);
        int len = 4 + payload.length;
        if (totalSize + len > MAX_SIZE) {
            throw new IOException("extra subfield '" + f.getId() + "' too big (extras total size is already at " + totalSize + ")");
        }

        fieldsList.add(f);
        totalSize += len;

        return this;
    }

    public void clear() {
        fieldsList.clear();
        totalSize = 0;
    }

    /**
     * Find the 1st subfield that matches the id.
     *
     * @return the SubField if found, null otherwise.
     */
    public SubField findFirstSubField(String subfieldId) {
        return fieldsList.stream().filter(f -> f.getId().equals(subfieldId)).findFirst().orElse(null);
    }

    /**
     * The bytes count of this extra payload when encoded. This does not include its
     * own 16 bits size. For N sub fields, the total is all subfields payloads + 4N.
     */
    public int getEncodedSize() {
        return totalSize;
    }

    /**
     * @return an unmodifiable copy of the subfields list.
     */
    public List<SubField> getFieldsList() {
        return Collections.unmodifiableList(fieldsList);
    }

    /**
     * The count of subfields contained in this extra.
     */
    public int getSize() {
        return fieldsList.size();
    }

    public boolean isEmpty() {
        return fieldsList.isEmpty();
    }

    /**
     * Give all 2-chars ISO-8859-1 strings denoting the subfields. Note that this is
     * imprecise as ids can repeat. Use the methods with indexes to find a specific
     * occurence.
     */
    public List<String> listIds() {
        return fieldsList.stream().map(SubField::getId).collect(Collectors.toList());
    }

    /**
     * Find the subfield at the given index.
     */
    public SubField subFieldAt(int i) {
        return fieldsList.get(i);
    }

    byte[] toBytes() {
        if (fieldsList.isEmpty()) {
            return ZERO_BYTES;
        }

        byte[] ba = new byte[totalSize];

        int pos = 0;
        for (SubField f : fieldsList) {
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

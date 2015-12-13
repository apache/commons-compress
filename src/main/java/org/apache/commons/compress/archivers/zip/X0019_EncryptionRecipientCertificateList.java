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
package org.apache.commons.compress.archivers.zip;

/**
 * PKCS#7 Encryption Recipient Certificate List (0x0019)
 *
 * This field MAY contain information about each of the certificates used in
 * encryption processing and it can be used to identify who is allowed to
 * decrypt encrypted files. This field should only appear in the archive extra
 * data record. This field is not required and serves only to aid archive
 * modifications by preserving public encryption key data. Individual security
 * requirements may dictate that this data be omitted to deter information
 * exposure.
 *
 * Note: all fields stored in Intel low-byte/high-byte order.
 *
 * <pre>
 *          Value     Size     Description
 *          -----     ----     -----------
 * (CStore) 0x0019    2 bytes  Tag for this "extra" block type
 *          TSize     2 bytes  Size of the store data
 *          Version   2 bytes  Format version number - must 0x0001 at this time
 *          CStore    (var)    PKCS#7 data blob
 * </pre>
 *
 * <b>See the section describing the Strong Encryption Specification for
 * details. Refer to the section in this document entitled
 * "Incorporating PKWARE Proprietary Technology into Your Product" for more
 * information.</b>
 *
 * @NotThreadSafe
 */
public class X0019_EncryptionRecipientCertificateList extends PKWareExtraHeader implements ZipExtraField {
    private static final ZipShort HEADER_ID = new ZipShort(0x0019);
    private static final long serialVersionUID = 1L;

    /**
     * Get the header id.
     *
     * @return the header id
     */
    public ZipShort getHeaderId() {
        return HEADER_ID;
    }

    /**
     * Extra field data in local file data - without Header-ID or length
     * specifier.
     */
    private byte[] localData;

    /**
     * Set the extra field data in the local file data - without Header-ID or
     * length specifier.
     *
     * @param data
     *            the field data to use
     */
    public void setLocalFileDataData(byte[] data) {
        localData = ZipUtil.copy(data);
    }

    /**
     * Get the length of the local data.
     *
     * @return the length of the local data
     */
    public ZipShort getLocalFileDataLength() {
        return new ZipShort(localData != null ? localData.length : 0);
    }

    /**
     * Get the local data.
     *
     * @return the local data
     */
    public byte[] getLocalFileDataData() {
        return ZipUtil.copy(localData);
    }

    /**
     * Extra field data in central directory - without Header-ID or length
     * specifier.
     */
    private byte[] centralData;

    /**
     * Set the extra field data in central directory.
     *
     * @param data
     *            the data to use
     */
    public void setCentralDirectoryData(byte[] data) {
        centralData = ZipUtil.copy(data);
    }

    /**
     * Get the central data length. If there is no central data, get the local
     * file data length.
     *
     * @return the central data length
     */
    public ZipShort getCentralDirectoryLength() {
        if (centralData != null) {
            return new ZipShort(centralData.length);
        }
        return getLocalFileDataLength();
    }

    /**
     * Get the central data.
     *
     * @return the central data if present, else return the local file data
     */
    public byte[] getCentralDirectoryData() {
        if (centralData != null) {
            return ZipUtil.copy(centralData);
        }
        return getLocalFileDataData();
    }

    /**
     * @param data
     *            the array of bytes.
     * @param offset
     *            the source location in the data array.
     * @param length
     *            the number of bytes to use in the data array.
     * @see ZipExtraField#parseFromLocalFileData(byte[], int, int)
     */
    public void parseFromLocalFileData(byte[] data, int offset, int length) {
        byte[] tmp = new byte[length];
        System.arraycopy(data, offset, tmp, 0, length);
        setLocalFileDataData(tmp);
    }

    /**
     * @param data
     *            the array of bytes.
     * @param offset
     *            the source location in the data array.
     * @param length
     *            the number of bytes to use in the data array.
     * @see ZipExtraField#parseFromCentralDirectoryData(byte[], int, int)
     */
    public void parseFromCentralDirectoryData(byte[] data, int offset, int length) {
        byte[] tmp = new byte[length];
        System.arraycopy(data, offset, tmp, 0, length);
        setCentralDirectoryData(tmp);
        if (localData == null) {
            setLocalFileDataData(tmp);
        }
    }
}

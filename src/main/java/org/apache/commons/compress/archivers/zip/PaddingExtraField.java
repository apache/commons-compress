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

import java.util.zip.ZipException;

/**
 * An extra field who's sole purpose is to pad the local file header
 * so that the entry's data starts at a certain position.
 *
 * <p>The actual content of the padding is ignored and not retained
 * when reading a padding field.</p>
 *
 * <p>This enables Commons Compress to create "aligned" archives
 * similar to Android's zipalign command line tool.</p>
 *
 * @since 1.14
 * @see "https://developer.android.com/studio/command-line/zipalign.html"
 * @see ZipArchiveEntry#setAlignment
 */
public class PaddingExtraField implements ZipExtraField {

    /**
     * Extra field id used for padding (there is no special value documented,
     * therefore USHORT_MAX seems to be good choice).
     */
    public static final ZipShort ID = new ZipShort(0xffff);

    private int len = 0;

    public PaddingExtraField() {
    }

    public PaddingExtraField(int len) {
        this.len = len;
    }

    @Override
    public ZipShort getHeaderId() {
        return ID;
    }

    @Override
    public ZipShort getLocalFileDataLength() {
        return new ZipShort(len);
    }

    @Override
    public ZipShort getCentralDirectoryLength() {
        return ZipShort.ZERO;
    }

    @Override
    public byte[] getLocalFileDataData() {
        return new byte[len];
    }

    @Override
    public byte[] getCentralDirectoryData() {
        return new byte[0];
    }

    @Override
    public void parseFromLocalFileData(byte[] buffer, int offset, int length) {
        len = length;
    }

    @Override
    public void parseFromCentralDirectoryData(byte[] buffer, int offset, int length) {
    }
}

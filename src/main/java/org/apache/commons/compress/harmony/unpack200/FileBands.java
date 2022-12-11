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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 * Parses the file band headers (not including the actual bits themselves). At the end of this parse call, the input
 * stream will be positioned at the start of the file_bits themselves, and there will be Sum(file_size) bits remaining
 * in the stream with BYTE1 compression. A decent implementation will probably just stream the bytes out to the
 * reconstituted Jar rather than caching them.
 */
public class FileBands extends BandSet {

    private byte[][] fileBits;

    private int[] fileModtime;

    private String[] fileName;

    private int[] fileOptions;

    private long[] fileSize;

    private final String[] cpUTF8;

    private InputStream in;

    /**
     * @param segment TODO
     */
    public FileBands(final Segment segment) {
        super(segment);
        this.cpUTF8 = segment.getCpBands().getCpUTF8();
    }

    public byte[][] getFileBits() {
        return fileBits;
    }

    public int[] getFileModtime() {
        return fileModtime;
    }

    public String[] getFileName() {
        return fileName;
    }

    public int[] getFileOptions() {
        return fileOptions;
    }

    public long[] getFileSize() {
        return fileSize;
    }

    // TODO: stream the file bits directly somehow
    public void processFileBits() throws IOException, Pack200Exception {
        // now read in the bytes
        final int numberOfFiles = header.getNumberOfFiles();
        fileBits = new byte[numberOfFiles][];
        for (int i = 0; i < numberOfFiles; i++) {
            final int size = (int) fileSize[i];
            // TODO This breaks if file_size > 2^32. Probably an array is
            // not the right choice, and we should just serialize it here?
            fileBits[i] = new byte[size];
            final int read = in.read(fileBits[i]);
            if (size != 0 && read < size) {
                throw new Pack200Exception("Expected to read " + size + " bytes but read " + read);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.BandSet#unpack(java.io.InputStream)
     */
    @Override
    public void read(final InputStream in) throws IOException, Pack200Exception {
        final int numberOfFiles = header.getNumberOfFiles();
        final SegmentOptions options = header.getOptions();

        fileName = parseReferences("file_name", in, Codec.UNSIGNED5, numberOfFiles, cpUTF8);
        fileSize = parseFlags("file_size", in, numberOfFiles, Codec.UNSIGNED5, options.hasFileSizeHi());
        if (options.hasFileModtime()) {
            fileModtime = decodeBandInt("file_modtime", in, Codec.DELTA5, numberOfFiles);
        } else {
            fileModtime = new int[numberOfFiles];
        }
        if (options.hasFileOptions()) {
            fileOptions = decodeBandInt("file_options", in, Codec.UNSIGNED5, numberOfFiles);
        } else {
            fileOptions = new int[numberOfFiles];
        }
        this.in = in; // store for use by processFileBits(), which is called
        // later
    }

    @Override
    public void unpack() {

    }

}

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
package org.apache.commons.compress.harmony.pack200;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.compress.harmony.pack200.Archive.PackingFile;
import org.apache.commons.compress.harmony.pack200.Archive.SegmentUnit;
import org.apache.commons.compress.utils.ExactMath;
import org.objectweb.asm.ClassReader;

/**
 * Bands containing information about files in the pack200 archive and the file contents for non-class-files.
 * Corresponds to the {@code file_bands} set of bands described in the specification.
 */
public class FileBands extends BandSet {

    private final CPUTF8[] fileName;
    private int[] file_name;
    private final int[] file_modtime;
    private final long[] file_size;
    private final int[] file_options;
    private final byte[][] file_bits;
    private final List<PackingFile> fileList;
    private final PackingOptions options;
    private final CpBands cpBands;

    public FileBands(final CpBands cpBands, final SegmentHeader segmentHeader, final PackingOptions options,
        final SegmentUnit segmentUnit, final int effort) {
        super(effort, segmentHeader);
        fileList = segmentUnit.getFileList();
        this.options = options;
        this.cpBands = cpBands;
        final int size = fileList.size();
        fileName = new CPUTF8[size];
        file_modtime = new int[size];
        file_size = new long[size];
        file_options = new int[size];
        int totalSize = 0;
        file_bits = new byte[size][];
        final int archiveModtime = segmentHeader.getArchive_modtime();

        final Set<String> classNames = new HashSet<>();
        for (final ClassReader reader : segmentUnit.getClassList()) {
            classNames.add(reader.getClassName());
        }
        final CPUTF8 emptyString = cpBands.getCPUtf8("");
        long modtime;
        int latestModtime = Integer.MIN_VALUE;
        final boolean isLatest = !PackingOptions.KEEP.equals(options.getModificationTime());
        for (int i = 0; i < size; i++) {
            final PackingFile packingFile = fileList.get(i);
            final String name = packingFile.getName();
            if (name.endsWith(".class") && !options.isPassFile(name)) {
                file_options[i] |= (1 << 1);
                if (classNames.contains(name.substring(0, name.length() - 6))) {
                    fileName[i] = emptyString;
                } else {
                    fileName[i] = cpBands.getCPUtf8(name);
                }
            } else {
                fileName[i] = cpBands.getCPUtf8(name);
            }
            // set deflate_hint for file element
            if (options.isKeepDeflateHint() && packingFile.isDefalteHint()) {
                file_options[i] |= 0x1;
            }
            final byte[] bytes = packingFile.getContents();
            file_size[i] = bytes.length;
            totalSize = ExactMath.add(totalSize, file_size[i]);

            // update modification time
            modtime = (packingFile.getModtime() + TimeZone.getDefault().getRawOffset()) / 1000L;
            file_modtime[i] = (int) (modtime - archiveModtime);
            if (isLatest && latestModtime < file_modtime[i]) {
                latestModtime = file_modtime[i];
            }

            file_bits[i] = packingFile.getContents();
        }

        if (isLatest) {
            Arrays.fill(file_modtime, latestModtime);
        }
    }

    /**
     * All input classes for the segment have now been read in, so this method is called so that this class can
     * calculate/complete anything it could not do while classes were being read.
     */
    public void finaliseBands() {
        file_name = new int[fileName.length];
        for (int i = 0; i < file_name.length; i++) {
            if (fileName[i].equals(cpBands.getCPUtf8(""))) {
                final PackingFile packingFile = fileList.get(i);
                final String name = packingFile.getName();
                if (options.isPassFile(name)) {
                    fileName[i] = cpBands.getCPUtf8(name);
                    file_options[i] &= (1 << 1) ^ 0xFFFFFFFF;
                }
            }
            file_name[i] = fileName[i].getIndex();
        }
    }

    private int[] flatten(final byte[][] bytes) {
        int total = 0;
        for (final byte[] element : bytes) {
            total += element.length;
        }
        final int[] band = new int[total];
        int index = 0;
        for (final byte[] element : bytes) {
            for (final byte element2 : element) {
                band[index++] = element2 & 0xFF;
            }
        }
        return band;
    }

    @Override
    public void pack(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing file bands...");
        byte[] encodedBand = encodeBandInt("file_name", file_name, Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from file_name[" + file_name.length + "]");

        encodedBand = encodeFlags("file_size", file_size, Codec.UNSIGNED5, Codec.UNSIGNED5,
            segmentHeader.have_file_size_hi());
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from file_size[" + file_size.length + "]");

        if (segmentHeader.have_file_modtime()) {
            encodedBand = encodeBandInt("file_modtime", file_modtime, Codec.DELTA5);
            out.write(encodedBand);
            PackingUtils.log("Wrote " + encodedBand.length + " bytes from file_modtime[" + file_modtime.length + "]");
        }
        if (segmentHeader.have_file_options()) {
            encodedBand = encodeBandInt("file_options", file_options, Codec.UNSIGNED5);
            out.write(encodedBand);
            PackingUtils.log("Wrote " + encodedBand.length + " bytes from file_options[" + file_options.length + "]");
        }

        encodedBand = encodeBandInt("file_bits", flatten(file_bits), Codec.BYTE1);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from file_bits[" + file_bits.length + "]");
    }

}

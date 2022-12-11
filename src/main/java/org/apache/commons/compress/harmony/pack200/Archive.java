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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;

/**
 * Archive is the main entry point to pack200 and represents a packed archive. An archive is constructed with either a
 * JarInputStream and an output stream or a JarFile as input and an OutputStream. Options can be set, then
 * {@code pack()} is called, to pack the Jar file into a pack200 archive.
 */
public class Archive {

    static class PackingFile {

        private final String name;
        private byte[] contents;
        private final long modtime;
        private final boolean deflateHint;
        private final boolean isDirectory;

        public PackingFile(final byte[] bytes, final JarEntry jarEntry) {
            name = jarEntry.getName();
            contents = bytes;
            modtime = jarEntry.getTime();
            deflateHint = jarEntry.getMethod() == ZipEntry.DEFLATED;
            isDirectory = jarEntry.isDirectory();
        }

        public PackingFile(final String name, final byte[] contents, final long modtime) {
            this.name = name;
            this.contents = contents;
            this.modtime = modtime;
            deflateHint = false;
            isDirectory = false;
        }

        public byte[] getContents() {
            return contents;
        }

        public long getModtime() {
            return modtime;
        }

        public String getName() {
            return name;
        }

        public boolean isDefalteHint() {
            return deflateHint;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public void setContents(final byte[] contents) {
            this.contents = contents;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    static class SegmentUnit {

        private final List<Pack200ClassReader> classList;

        private final List<PackingFile> fileList;

        private int byteAmount;

        private int packedByteAmount;

        public SegmentUnit(final List<Pack200ClassReader> classes, final List<PackingFile> files) {
            classList = classes;
            fileList = files;
            byteAmount = 0;
            // Calculate the amount of bytes in classes and files before packing
            byteAmount += classList.stream().mapToInt(element -> element.b.length).sum();
            byteAmount += fileList.stream().mapToInt(element -> element.contents.length).sum();
        }

        public void addPackedByteAmount(final int amount) {
            packedByteAmount += amount;
        }

        public int classListSize() {
            return classList.size();
        }

        public int fileListSize() {
            return fileList.size();
        }

        public int getByteAmount() {
            return byteAmount;
        }

        public List<Pack200ClassReader> getClassList() {
            return classList;
        }

        public List<PackingFile> getFileList() {
            return fileList;
        }

        public int getPackedByteAmount() {
            return packedByteAmount;
        }
    }
    private final JarInputStream jarInputStream;
    private final OutputStream outputStream;
    private JarFile jarFile;

    private long currentSegmentSize;

    private final PackingOptions options;

    /**
     * Creates an Archive with the given input file and a stream for the output
     *
     * @param jarFile - the input file
     * @param outputStream TODO
     * @param options - packing options (if null then defaults are used)
     * @throws IOException If an I/O error occurs.
     */
    public Archive(final JarFile jarFile, OutputStream outputStream, PackingOptions options) throws IOException {
        if (options == null) { // use all defaults
            options = new PackingOptions();
        }
        this.options = options;
        if (options.isGzip()) {
            outputStream = new GZIPOutputStream(outputStream);
        }
        this.outputStream = new BufferedOutputStream(outputStream);
        this.jarFile = jarFile;
        jarInputStream = null;
        PackingUtils.config(options);
    }

    /**
     * Creates an Archive with streams for the input and output.
     *
     * @param inputStream TODO
     * @param outputStream TODO
     * @param options - packing options (if null then defaults are used)
     * @throws IOException If an I/O error occurs.
     */
    public Archive(final JarInputStream inputStream, OutputStream outputStream, PackingOptions options)
        throws IOException {
        jarInputStream = inputStream;
        if (options == null) {
            // use all defaults
            options = new PackingOptions();
        }
        this.options = options;
        if (options.isGzip()) {
            outputStream = new GZIPOutputStream(outputStream);
        }
        this.outputStream = new BufferedOutputStream(outputStream);
        PackingUtils.config(options);
    }

	private boolean addJarEntry(final PackingFile packingFile, final List<Pack200ClassReader> javaClasses, final List<PackingFile> files) {
        final long segmentLimit = options.getSegmentLimit();
        if (segmentLimit != -1 && segmentLimit != 0) {
            // -1 is a special case where only one segment is created and
            // 0 is a special case where one segment is created for each file
            // except for files in "META-INF"
            final long packedSize = estimateSize(packingFile);
            if (packedSize + currentSegmentSize > segmentLimit && currentSegmentSize > 0) {
                // don't add this JarEntry to the current segment
                return false;
            }
            // do add this JarEntry
            currentSegmentSize += packedSize;
        }

        final String name = packingFile.getName();
        if (name.endsWith(".class") && !options.isPassFile(name)) {
            final Pack200ClassReader classParser = new Pack200ClassReader(packingFile.contents);
            classParser.setFileName(name);
            javaClasses.add(classParser);
            packingFile.contents = new byte[0];
        }
        files.add(packingFile);
        return true;
    }

    private void doNormalPack() throws IOException, Pack200Exception {
		PackingUtils.log("Start to perform a normal packing");
		List<PackingFile> packingFileList;
		if (jarInputStream != null) {
			packingFileList = PackingUtils.getPackingFileListFromJar(jarInputStream, options.isKeepFileOrder());
		} else {
			packingFileList = PackingUtils.getPackingFileListFromJar(jarFile, options.isKeepFileOrder());
		}

		final List<SegmentUnit> segmentUnitList = splitIntoSegments(packingFileList);
		int previousByteAmount = 0;
		int packedByteAmount = 0;

		final int segmentSize = segmentUnitList.size();
		SegmentUnit segmentUnit;
		for (int index = 0; index < segmentSize; index++) {
			segmentUnit = segmentUnitList.get(index);
			new Segment().pack(segmentUnit, outputStream, options);
			previousByteAmount += segmentUnit.getByteAmount();
			packedByteAmount += segmentUnit.getPackedByteAmount();
		}

		PackingUtils.log("Total: Packed " + previousByteAmount + " input bytes of " + packingFileList.size()
				+ " files into " + packedByteAmount + " bytes in " + segmentSize + " segments");

		outputStream.close();
	}

    private void doZeroEffortPack() throws IOException {
        PackingUtils.log("Start to perform a zero-effort packing");
        if (jarInputStream != null) {
            PackingUtils.copyThroughJar(jarInputStream, outputStream);
        } else {
            PackingUtils.copyThroughJar(jarFile, outputStream);
        }
    }

    private long estimateSize(final PackingFile packingFile) {
        // The heuristic used here is for compatibility with the RI and should
        // not be changed
        final String name = packingFile.getName();
        if (name.startsWith("META-INF") || name.startsWith("/META-INF")) {
            return 0;
        }
        long fileSize = packingFile.contents.length;
        if (fileSize < 0) {
            fileSize = 0;
        }
        return name.length() + fileSize + 5;
    }

    /**
     * Pack the archive
     *
     * @throws Pack200Exception TODO
     * @throws IOException If an I/O error occurs.
     */
    public void pack() throws Pack200Exception, IOException {
        if (0 == options.getEffort()) {
            doZeroEffortPack();
        } else {
            doNormalPack();
        }
    }

    private List<SegmentUnit> splitIntoSegments(final List<PackingFile> packingFileList) {
        final List<SegmentUnit> segmentUnitList = new ArrayList<>();
        List<Pack200ClassReader> classes = new ArrayList<>();
        List<PackingFile> files = new ArrayList<>();
        final long segmentLimit = options.getSegmentLimit();

        final int size = packingFileList.size();
        PackingFile packingFile;
        for (int index = 0; index < size; index++) {
            packingFile = packingFileList.get(index);
            if (!addJarEntry(packingFile, classes, files)) {
                // not added because segment has reached maximum size
                segmentUnitList.add(new SegmentUnit(classes, files));
                classes = new ArrayList<>();
                files = new ArrayList<>();
                currentSegmentSize = 0;
                // add the jar to a new segment
                addJarEntry(packingFile, classes, files);
                // ignore the size of first entry for compatibility with RI
                currentSegmentSize = 0;
            } else if (segmentLimit == 0 && estimateSize(packingFile) > 0) {
                // create a new segment for each class unless size is 0
                segmentUnitList.add(new SegmentUnit(classes, files));
                classes = new ArrayList<>();
                files = new ArrayList<>();
            }
        }
        // Change for Apache Commons Compress based on Apache Harmony.
        // if (classes.size() > 0 && files.size() > 0) {
        if (classes.size() > 0 || files.size() > 0) {
            segmentUnitList.add(new SegmentUnit(classes, files));
        }
        return segmentUnitList;
    }

}

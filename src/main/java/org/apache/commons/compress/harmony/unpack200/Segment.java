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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.Attribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPClass;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPField;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPMethod;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassConstantPool;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFile;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.commons.compress.harmony.unpack200.bytecode.InnerClassesAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.SourceFileAttribute;

/**
 * A Pack200 archive consists of one or more segments. Each segment is stand-alone, in the sense that every segment has
 * the magic number header; thus, every segment is also a valid archive. However, it is possible to combine
 * (non-GZipped) archives into a single large archive by concatenation alone. Thus all the hard work in unpacking an
 * archive falls to understanding a segment.
 *
 * The first component of a segment is the header; this contains (amongst other things) the expected counts of constant
 * pool entries, which in turn defines how many values need to be read from the stream. Because values are variable
 * width (see {@link Codec}), it is not possible to calculate the start of the next segment, although one of the header
 * values does hint at the size of the segment if non-zero, which can be used for buffering purposes.
 *
 * Note that this does not perform any buffering of the input stream; each value will be read on a byte-by-byte basis.
 * It does not perform GZip decompression automatically; both of these are expected to be done by the caller if the
 * stream has the magic header for GZip streams ({@link GZIPInputStream#GZIP_MAGIC}). In any case, if GZip decompression
 * is being performed the input stream will be buffered at a higher level, and thus this can read on a byte-oriented
 * basis.
 */
public class Segment {

    public static final int LOG_LEVEL_VERBOSE = 2;

    public static final int LOG_LEVEL_STANDARD = 1;

    public static final int LOG_LEVEL_QUIET = 0;

    private SegmentHeader header;

    private CpBands cpBands;

    private AttrDefinitionBands attrDefinitionBands;

    private IcBands icBands;

    private ClassBands classBands;

    private BcBands bcBands;

    private FileBands fileBands;

    private boolean overrideDeflateHint;

    private boolean deflateHint;

    private boolean doPreRead;

    private int logLevel;

    private PrintWriter logStream;

    private byte[][] classFilesContents;

    private boolean[] fileDeflate;

    private boolean[] fileIsClass;

    private InputStream internalBuffer;

    private ClassFile buildClassFile(final int classNum) {
        final ClassFile classFile = new ClassFile();
        final int[] major = classBands.getClassVersionMajor();
        final int[] minor = classBands.getClassVersionMinor();
        if (major != null) {
            classFile.major = major[classNum];
            classFile.minor = minor[classNum];
        } else {
            classFile.major = header.getDefaultClassMajorVersion();
            classFile.minor = header.getDefaultClassMinorVersion();
        }
        // build constant pool
        final ClassConstantPool cp = classFile.pool;
        final int fullNameIndexInCpClass = classBands.getClassThisInts()[classNum];
        final String fullName = cpBands.getCpClass()[fullNameIndexInCpClass];
        // SourceFile attribute
        int i = fullName.lastIndexOf("/") + 1; // if lastIndexOf==-1, then
        // -1+1=0, so str.substring(0)
        // == str

        // Get the source file attribute
        final List<Attribute> classAttributes = classBands.getClassAttributes()[classNum];
        SourceFileAttribute sourceFileAttribute = null;
        for (final Attribute classAttribute : classAttributes) {
            if (classAttribute.isSourceFileAttribute()) {
                sourceFileAttribute = ((SourceFileAttribute) classAttribute);
            }
        }

        if (sourceFileAttribute == null) {
            // If we don't have a source file attribute yet, we need
            // to infer it from the class.
            final AttributeLayout SOURCE_FILE = attrDefinitionBands.getAttributeDefinitionMap()
                .getAttributeLayout(AttributeLayout.ATTRIBUTE_SOURCE_FILE, AttributeLayout.CONTEXT_CLASS);
            if (SOURCE_FILE.matches(classBands.getRawClassFlags()[classNum])) {
                int firstDollar = -1;
                for (int index = 0; index < fullName.length(); index++) {
                    if (fullName.charAt(index) <= '$') {
                        firstDollar = index;
                    }
                }
                String fileName = null;

                if (firstDollar > -1 && (i <= firstDollar)) {
                    fileName = fullName.substring(i, firstDollar) + ".java";
                } else {
                    fileName = fullName.substring(i) + ".java";
                }
                sourceFileAttribute = new SourceFileAttribute(cpBands.cpUTF8Value(fileName, false));
                classFile.attributes = new Attribute[] {(Attribute) cp.add(sourceFileAttribute)};
            } else {
                classFile.attributes = new Attribute[] {};
            }
        } else {
            classFile.attributes = new Attribute[] {(Attribute) cp.add(sourceFileAttribute)};
        }

        // If we see any class attributes, add them to the class's attributes
        // that will
        // be written out. Keep SourceFileAttributes out since we just
        // did them above.
        final List<Attribute> classAttributesWithoutSourceFileAttribute = new ArrayList<>(classAttributes.size());
        for (int index = 0; index < classAttributes.size(); index++) {
            final Attribute attrib = classAttributes.get(index);
            if (!attrib.isSourceFileAttribute()) {
                classAttributesWithoutSourceFileAttribute.add(attrib);
            }
        }
        final Attribute[] originalAttributes = classFile.attributes;
        classFile.attributes = new Attribute[originalAttributes.length
            + classAttributesWithoutSourceFileAttribute.size()];
        System.arraycopy(originalAttributes, 0, classFile.attributes, 0, originalAttributes.length);
        for (int index = 0; index < classAttributesWithoutSourceFileAttribute.size(); index++) {
            final Attribute attrib = (classAttributesWithoutSourceFileAttribute.get(index));
            cp.add(attrib);
            classFile.attributes[originalAttributes.length + index] = attrib;
        }

        // this/superclass
        final ClassFileEntry cfThis = cp.add(cpBands.cpClassValue(fullNameIndexInCpClass));
        final ClassFileEntry cfSuper = cp.add(cpBands.cpClassValue(classBands.getClassSuperInts()[classNum]));
        // add interfaces
        final ClassFileEntry[] cfInterfaces = new ClassFileEntry[classBands.getClassInterfacesInts()[classNum].length];
        for (i = 0; i < cfInterfaces.length; i++) {
            cfInterfaces[i] = cp.add(cpBands.cpClassValue(classBands.getClassInterfacesInts()[classNum][i]));
        }
        // add fields
        final ClassFileEntry[] cfFields = new ClassFileEntry[classBands.getClassFieldCount()[classNum]];
        // fieldDescr and fieldFlags used to create this
        for (i = 0; i < cfFields.length; i++) {
            final int descriptorIndex = classBands.getFieldDescrInts()[classNum][i];
            final int nameIndex = cpBands.getCpDescriptorNameInts()[descriptorIndex];
            final int typeIndex = cpBands.getCpDescriptorTypeInts()[descriptorIndex];
            final CPUTF8 name = cpBands.cpUTF8Value(nameIndex);
            final CPUTF8 descriptor = cpBands.cpSignatureValue(typeIndex);
            cfFields[i] = cp.add(new CPField(name, descriptor, classBands.getFieldFlags()[classNum][i],
                classBands.getFieldAttributes()[classNum][i]));
        }
        // add methods
        final ClassFileEntry[] cfMethods = new ClassFileEntry[classBands.getClassMethodCount()[classNum]];
        // methodDescr and methodFlags used to create this
        for (i = 0; i < cfMethods.length; i++) {
            final int descriptorIndex = classBands.getMethodDescrInts()[classNum][i];
            final int nameIndex = cpBands.getCpDescriptorNameInts()[descriptorIndex];
            final int typeIndex = cpBands.getCpDescriptorTypeInts()[descriptorIndex];
            final CPUTF8 name = cpBands.cpUTF8Value(nameIndex);
            final CPUTF8 descriptor = cpBands.cpSignatureValue(typeIndex);
            cfMethods[i] = cp.add(new CPMethod(name, descriptor, classBands.getMethodFlags()[classNum][i],
                classBands.getMethodAttributes()[classNum][i]));
        }
        cp.addNestedEntries();

        // add inner class attribute (if required)
        boolean addInnerClassesAttr = false;
        final IcTuple[] icLocal = getClassBands().getIcLocal()[classNum];
        final boolean icLocalSent = icLocal != null;
        final InnerClassesAttribute innerClassesAttribute = new InnerClassesAttribute("InnerClasses");
        final IcTuple[] icRelevant = getIcBands().getRelevantIcTuples(fullName, cp);
        final List<IcTuple> ic_stored = computeIcStored(icLocal, icRelevant);
        for (final IcTuple icStored : ic_stored) {
            final int innerClassIndex = icStored.thisClassIndex();
            final int outerClassIndex = icStored.outerClassIndex();
            final int simpleClassNameIndex = icStored.simpleClassNameIndex();

            final String innerClassString = icStored.thisClassString();
            final String outerClassString = icStored.outerClassString();
            final String simpleClassName = icStored.simpleClassName();

            CPClass innerClass = null;
            CPUTF8 innerName = null;
            CPClass outerClass = null;

            innerClass = innerClassIndex != -1 ? cpBands.cpClassValue(innerClassIndex)
                : cpBands.cpClassValue(innerClassString);
            if (!icStored.isAnonymous()) {
                innerName = simpleClassNameIndex != -1 ? cpBands.cpUTF8Value(simpleClassNameIndex)
                    : cpBands.cpUTF8Value(simpleClassName);
            }

            if (icStored.isMember()) {
                outerClass = outerClassIndex != -1 ? cpBands.cpClassValue(outerClassIndex)
                    : cpBands.cpClassValue(outerClassString);
            }
            final int flags = icStored.F;
            innerClassesAttribute.addInnerClassesEntry(innerClass, outerClass, innerName, flags);
            addInnerClassesAttr = true;
        }
        // If ic_local is sent and it's empty, don't add
        // the inner classes attribute.
        if (icLocalSent && (icLocal.length == 0)) {
            addInnerClassesAttr = false;
        }

        // If ic_local is not sent and ic_relevant is empty,
        // don't add the inner class attribute.
        if (!icLocalSent && (icRelevant.length == 0)) {
            addInnerClassesAttr = false;
        }

        if (addInnerClassesAttr) {
            // Need to add the InnerClasses attribute to the
            // existing classFile attributes.
            final Attribute[] originalAttrs = classFile.attributes;
            final Attribute[] newAttrs = new Attribute[originalAttrs.length + 1];
            System.arraycopy(originalAttrs, 0, newAttrs, 0, originalAttrs.length);
            newAttrs[newAttrs.length - 1] = innerClassesAttribute;
            classFile.attributes = newAttrs;
            cp.addWithNestedEntries(innerClassesAttribute);
        }
        // sort CP according to cp_All
        cp.resolve(this);
        // NOTE the indexOf is only valid after the cp.resolve()
        // build up remainder of file
        classFile.accessFlags = (int) classBands.getClassFlags()[classNum];
        classFile.thisClass = cp.indexOf(cfThis);
        classFile.superClass = cp.indexOf(cfSuper);
        // TODO placate format of file for writing purposes
        classFile.interfaces = new int[cfInterfaces.length];
        for (i = 0; i < cfInterfaces.length; i++) {
            classFile.interfaces[i] = cp.indexOf(cfInterfaces[i]);
        }
        classFile.fields = cfFields;
        classFile.methods = cfMethods;
        return classFile;
    }

    /**
     * Given an ic_local and an ic_relevant, use them to calculate what should be added as ic_stored.
     *
     * @param icLocal IcTuple[] array of local transmitted tuples
     * @param icRelevant IcTuple[] array of relevant tuples
     * @return List of tuples to be stored. If ic_local is null or empty, the values returned may not be correct. The
     *         caller will have to determine if this is the case.
     */
    private List<IcTuple> computeIcStored(final IcTuple[] icLocal, final IcTuple[] icRelevant) {
        final List<IcTuple> result = new ArrayList<>(icRelevant.length);
        final List<IcTuple> duplicates = new ArrayList<>(icRelevant.length);
        final Set<IcTuple> isInResult = new HashSet<>(icRelevant.length);

        // need to compute:
        // result = ic_local XOR ic_relevant

        // add ic_local
        if (icLocal != null) {
            for (final IcTuple element : icLocal) {
                if (isInResult.add(element)) {
                    result.add(element);
                }
            }
        }

        // add ic_relevant
        for (final IcTuple element : icRelevant) {
            if (isInResult.add(element)) {
                result.add(element);
            } else {
                duplicates.add(element);
            }
        }

        // eliminate "duplicates"
        duplicates.forEach(result::remove);

        return result;
    }

    protected AttrDefinitionBands getAttrDefinitionBands() {
        return attrDefinitionBands;
    }

    protected ClassBands getClassBands() {
        return classBands;
    }

    public SegmentConstantPool getConstantPool() {
        return cpBands.getConstantPool();
    }

    protected CpBands getCpBands() {
        return cpBands;
    }

    protected IcBands getIcBands() {
        return icBands;
    }

    public SegmentHeader getSegmentHeader() {
        return header;
    }

    public void log(final int logLevel, final String message) {
        if (this.logLevel >= logLevel) {
            logStream.println(message);
        }
    }

    /**
     * Override the archive's deflate hint with the given boolean
     *
     * @param deflateHint - the deflate hint to use
     */
    public void overrideDeflateHint(final boolean deflateHint) {
        this.overrideDeflateHint = true;
        this.deflateHint = deflateHint;
    }

    /**
     * This performs the actual work of parsing against a non-static instance of Segment. This method is intended to run
     * concurrently for multiple segments.
     *
     * @throws IOException if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported codec
     */
    private void parseSegment() throws IOException, Pack200Exception {

        header.unpack();
        cpBands.unpack();
        attrDefinitionBands.unpack();
        icBands.unpack();
        classBands.unpack();
        bcBands.unpack();
        fileBands.unpack();

        int classNum = 0;
        final int numberOfFiles = header.getNumberOfFiles();
        final String[] fileName = fileBands.getFileName();
        final int[] fileOptions = fileBands.getFileOptions();
        final SegmentOptions options = header.getOptions();

        classFilesContents = new byte[numberOfFiles][];
        fileDeflate = new boolean[numberOfFiles];
        fileIsClass = new boolean[numberOfFiles];

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);

        for (int i = 0; i < numberOfFiles; i++) {
            String name = fileName[i];

            final boolean nameIsEmpty = (name == null) || name.equals("");
            final boolean isClass = (fileOptions[i] & 2) == 2 || nameIsEmpty;
            if (isClass && nameIsEmpty) {
                name = cpBands.getCpClass()[classBands.getClassThisInts()[classNum]] + ".class";
                fileName[i] = name;
            }

            if (!overrideDeflateHint) {
                fileDeflate[i] = (fileOptions[i] & 1) == 1 || options.shouldDeflate();
            } else {
                fileDeflate[i] = deflateHint;
            }

            fileIsClass[i] = isClass;

            if (isClass) {
                final ClassFile classFile = buildClassFile(classNum);
                classFile.write(dos);
                dos.flush();

                classFilesContents[classNum] = bos.toByteArray();
                bos.reset();

                classNum++;
            }
        }
    }

    /**
     * This performs reading the data from the stream into non-static instance of Segment. After the completion of this
     * method stream can be freed.
     *
     * @param in the input stream to read from
     * @throws IOException if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported codec
     */
    private void readSegment(final InputStream in) throws IOException, Pack200Exception {
        log(LOG_LEVEL_VERBOSE, "-------");
        cpBands = new CpBands(this);
        cpBands.read(in);
        attrDefinitionBands = new AttrDefinitionBands(this);
        attrDefinitionBands.read(in);
        icBands = new IcBands(this);
        icBands.read(in);
        classBands = new ClassBands(this);
        classBands.read(in);
        bcBands = new BcBands(this);
        bcBands.read(in);
        fileBands = new FileBands(this);
        fileBands.read(in);

        fileBands.processFileBits();
    }

    public void setLogLevel(final int logLevel) {
        this.logLevel = logLevel;
    }

    public void setLogStream(final OutputStream logStream) {
        this.logStream = new PrintWriter(new OutputStreamWriter(logStream, Charset.defaultCharset()), false);
    }

    public void setPreRead(final boolean value) {
        doPreRead = value;
    }

    /**
     * Unpacks a packed stream (either .pack. or .pack.gz) into a corresponding JarOuputStream.
     *
     * @param in a packed stream.
     * @param out output stream.
     * @throws Pack200Exception if there is a problem unpacking
     * @throws IOException if there is a problem with I/O during unpacking
     */
    public void unpack(final InputStream in, final JarOutputStream out) throws IOException, Pack200Exception {
        unpackRead(in);
        unpackProcess();
        unpackWrite(out);
    }

    void unpackProcess() throws IOException, Pack200Exception {
        if (internalBuffer != null) {
            readSegment(internalBuffer);
        }
        parseSegment();
    }

    /*
     * Package-private accessors for unpacking stages
     */
    void unpackRead(InputStream in) throws IOException, Pack200Exception {
        if (!in.markSupported()) {
            in = new BufferedInputStream(in);
        }

        header = new SegmentHeader(this);
        header.read(in);

        final int size = (int) header.getArchiveSize() - header.getArchiveSizeOffset();

        if (doPreRead && header.getArchiveSize() != 0) {
            final byte[] data = new byte[size];
            in.read(data);
            internalBuffer = new BufferedInputStream(new ByteArrayInputStream(data));
        } else {
            readSegment(in);
        }
    }

    void unpackWrite(final JarOutputStream out) throws IOException {
        writeJar(out);
        if (logStream != null) {
            logStream.close();
        }
    }

    /**
     * Writes the segment to an output stream. The output stream should be pre-buffered for efficiency. Also takes the
     * same input stream for reading, since the file bits may not be loaded and thus just copied from one stream to
     * another. Doesn't close the output stream when finished, in case there are more entries (e.g. further segments) to
     * be written.
     *
     * @param out the JarOutputStream to write data to
     * @throws IOException if an error occurs while reading or writing to the streams
     */
    public void writeJar(final JarOutputStream out) throws IOException {
        final String[] fileName = fileBands.getFileName();
        final int[] fileModtime = fileBands.getFileModtime();
        final long[] fileSize = fileBands.getFileSize();
        final byte[][] fileBits = fileBands.getFileBits();

        // now write the files out
        int classNum = 0;
        final int numberOfFiles = header.getNumberOfFiles();
        final long archiveModtime = header.getArchiveModtime();

        for (int i = 0; i < numberOfFiles; i++) {
            final String name = fileName[i];
            // For Pack200 archives, modtime is in seconds
            // from the epoch. JarEntries need it to be in
            // milliseconds from the epoch.
            // Even though we're adding two longs and multiplying
            // by 1000, we won't overflow because both longs are
            // always under 2^32.
            final long modtime = 1000 * (archiveModtime + fileModtime[i]);
            final boolean deflate = fileDeflate[i];

            final JarEntry entry = new JarEntry(name);
            if (deflate) {
                entry.setMethod(ZipEntry.DEFLATED);
            } else {
                entry.setMethod(ZipEntry.STORED);
                final CRC32 crc = new CRC32();
                if (fileIsClass[i]) {
                    crc.update(classFilesContents[classNum]);
                    entry.setSize(classFilesContents[classNum].length);
                } else {
                    crc.update(fileBits[i]);
                    entry.setSize(fileSize[i]);
                }
                entry.setCrc(crc.getValue());
            }
            // On Windows at least, need to correct for timezone
            entry.setTime(modtime - TimeZone.getDefault().getRawOffset());
            out.putNextEntry(entry);

            // write to output stream
            if (fileIsClass[i]) {
                entry.setSize(classFilesContents[classNum].length);
                out.write(classFilesContents[classNum]);
                classNum++;
            } else {
                entry.setSize(fileSize[i]);
                out.write(fileBits[i]);
            }
        }
    }

}
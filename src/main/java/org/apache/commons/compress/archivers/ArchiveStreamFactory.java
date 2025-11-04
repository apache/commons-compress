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
package org.apache.commons.compress.archivers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.lha.LhaArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;

/**
 * Creates an Archive[In|Out]putStreams from names or the first bytes of the InputStream. In order to add other implementations, you should extend
 * ArchiveStreamFactory and override the appropriate methods (and call their implementation from super of course).
 *
 * Compressing a ZIP-File:
 *
 * <pre>
 * final OutputStream out = Files.newOutputStream(output.toPath());
 * ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, out);
 *
 * os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
 * IOUtils.copy(Files.newInputStream(file1.toPath()), os);
 * os.closeArchiveEntry();
 *
 * os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
 * IOUtils.copy(Files.newInputStream(file2.toPath()), os);
 * os.closeArchiveEntry();
 * os.close();
 * </pre>
 *
 * Decompressing a ZIP-File:
 *
 * <pre>
 * final InputStream is = Files.newInputStream(input.toPath());
 * ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, is);
 * ZipArchiveEntry entry = (ZipArchiveEntry) in.getNextEntry();
 * OutputStream out = Files.newOutputStream(dir.toPath().resolve(entry.getName()));
 * IOUtils.copy(in, out);
 * out.close();
 * in.close();
 * </pre>
 *
 * @Immutable provided that the deprecated method setEntryEncoding is not used.
 * @ThreadSafe even if the deprecated method setEntryEncoding is used
 */
public class ArchiveStreamFactory implements ArchiveStreamProvider {

    private static final int TAR_HEADER_SIZE = 512;

    private static final int TAR_TEST_ENTRY_COUNT = 10;

    private static final int DUMP_SIGNATURE_SIZE = 32;

    private static final int LHA_SIGNATURE_SIZE = 22;

    private static final int SIGNATURE_SIZE = 12;

    /**
     * The singleton instance using the platform default encoding.
     *
     * @since 1.21
     */
    public static final ArchiveStreamFactory DEFAULT = new ArchiveStreamFactory();

    /**
     * Constant (value {@value}) used to identify the APK archive format.
     * <p>
     * APK file extensions are .apk, .xapk, .apks, .apkm
     * </p>
     *
     * @since 1.22
     */
    public static final String APK = "apk";

    /**
     * Constant (value {@value}) used to identify the XAPK archive format.
     * <p>
     * APK file extensions are .apk, .xapk, .apks, .apkm
     * </p>
     *
     * @since 1.22
     */
    public static final String XAPK = "xapk";

    /**
     * Constant (value {@value}) used to identify the APKS archive format.
     * <p>
     * APK file extensions are .apk, .xapk, .apks, .apkm
     * </p>
     *
     * @since 1.22
     */
    public static final String APKS = "apks";

    /**
     * Constant (value {@value}) used to identify the APKM archive format.
     * <p>
     * APK file extensions are .apk, .xapk, .apks, .apkm
     * </p>
     *
     * @since 1.22
     */
    public static final String APKM = "apkm";

    /**
     * Constant (value {@value}) used to identify the AR archive format.
     *
     * @since 1.1
     */
    public static final String AR = "ar";

    /**
     * Constant (value {@value}) used to identify the ARJ archive format. Not supported as an output stream type.
     *
     * @since 1.6
     */
    public static final String ARJ = "arj";

    /**
     * Constant (value {@value}) used to identify the CPIO archive format.
     *
     * @since 1.1
     */
    public static final String CPIO = "cpio";

    /**
     * Constant (value {@value}) used to identify the Unix DUMP archive format. Not supported as an output stream type.
     *
     * @since 1.3
     */
    public static final String DUMP = "dump";

    /**
     * Constant (value {@value}) used to identify the JAR archive format.
     *
     * @since 1.1
     */
    public static final String JAR = "jar";

    /**
     * Constant (value {@value}) used to identify the LHA archive format.
     * Not supported as an output stream type.
     * @since 1.29.0
     */
    public static final String LHA = "lha";

    /**
     * Constant used to identify the TAR archive format.
     *
     * @since 1.1
     */
    public static final String TAR = "tar";

    /**
     * Constant (value {@value}) used to identify the ZIP archive format.
     *
     * @since 1.1
     */
    public static final String ZIP = "zip";

    /**
     * Constant (value {@value}) used to identify the 7z archive format.
     *
     * @since 1.8
     */
    public static final String SEVEN_Z = "7z";

    private static Iterable<ArchiveStreamProvider> archiveStreamProviderIterable() {
        return ServiceLoader.load(ArchiveStreamProvider.class, ClassLoader.getSystemClassLoader());
    }

    /**
     * Try to determine the type of Archiver
     *
     * @param in input stream
     * @return type of archiver if found
     * @throws ArchiveException if an archiver cannot be detected in the stream
     * @since 1.14
     */
    public static String detect(final InputStream in) throws ArchiveException {
        if (in == null) {
            throw new IllegalArgumentException("Stream must not be null.");
        }
        if (!in.markSupported()) {
            throw new IllegalArgumentException("Mark is not supported.");
        }
        final byte[] signature = new byte[SIGNATURE_SIZE];
        in.mark(signature.length);
        int signatureLength = -1;
        try {
            signatureLength = IOUtils.readFully(in, signature);
            in.reset();
        } catch (final IOException e) {
            throw new ArchiveException("Failure reading signature.", (Throwable) e);
        }
        // For now JAR files are detected as ZIP files.
        if (ZipArchiveInputStream.matches(signature, signatureLength)) {
            return ZIP;
        }
        // For now JAR files are detected as ZIP files.
        if (JarArchiveInputStream.matches(signature, signatureLength)) {
            return JAR;
        }
        if (ArArchiveInputStream.matches(signature, signatureLength)) {
            return AR;
        }
        if (CpioArchiveInputStream.matches(signature, signatureLength)) {
            return CPIO;
        }
        if (ArjArchiveInputStream.matches(signature, signatureLength)) {
            return ARJ;
        }
        if (SevenZFile.matches(signature, signatureLength)) {
            return SEVEN_Z;
        }
        // Dump needs a bigger buffer to check the signature;
        final byte[] dumpsig = new byte[DUMP_SIGNATURE_SIZE];
        in.mark(dumpsig.length);
        try {
            signatureLength = IOUtils.readFully(in, dumpsig);
            in.reset();
        } catch (final IOException e) {
            throw new ArchiveException("IOException while reading dump signature", (Throwable) e);
        }
        if (DumpArchiveInputStream.matches(dumpsig, signatureLength)) {
            return DUMP;
        }
        // LHA needs a bigger buffer to check the signature
        final byte[] lhasig = new byte[LHA_SIGNATURE_SIZE];
        in.mark(lhasig.length);
        try {
            signatureLength = IOUtils.readFully(in, lhasig);
            in.reset();
        } catch (final IOException e) {
            throw new ArchiveException("IOException while reading LHA signature", (Throwable) e);
        }
        if (LhaArchiveInputStream.matches(lhasig, signatureLength)) {
            return LHA;
        }
        // Tar needs an even bigger buffer to check the signature; read the first block
        final byte[] tarHeader = new byte[TAR_HEADER_SIZE];
        in.mark(tarHeader.length);
        try {
            signatureLength = IOUtils.readFully(in, tarHeader);
            in.reset();
        } catch (final IOException e) {
            throw new ArchiveException("IOException while reading tar signature", (Throwable) e);
        }
        if (TarArchiveInputStream.matches(tarHeader, signatureLength)) {
            return TAR;
        }
        // COMPRESS-117
        if (signatureLength >= TAR_HEADER_SIZE) {
            try (TarArchiveInputStream inputStream =
                    TarArchiveInputStream.builder().setByteArray(tarHeader).get()) {
                // COMPRESS-191 - verify the header checksum
                TarArchiveEntry entry = inputStream.getNextEntry();
                // try to find the first non-directory entry within the first 10 entries.
                int count = 0;
                while (entry != null && entry.isDirectory() && entry.isCheckSumOK() && count++ < TAR_TEST_ENTRY_COUNT) {
                    entry = inputStream.getNextEntry();
                }
                if (entry != null && entry.isCheckSumOK() && !entry.isDirectory() && isName(entry.getGroupName()) && isName(entry.getName())
                        && isName(entry.getUserName()) || count > 0) {
                    return TAR;
                }
            } catch (final Exception ignored) {
                // can generate IllegalArgumentException as well as IOException auto-detection, simply not a TAR ignored
            }
        }
        throw new ArchiveException("No Archiver found for the stream signature");
    }

    /**
     * Constructs a new sorted map from input stream provider names to provider objects.
     *
     * <p>
     * The map returned by this method will have one entry for each provider for which support is available in the current Java virtual machine. If two or more
     * supported provider have the same name then the resulting map will contain just one of them; which one it will contain is not specified.
     * </p>
     *
     * <p>
     * The invocation of this method, and the subsequent use of the resulting map, may cause time-consuming disk or network I/O operations to occur. This method
     * is provided for applications that need to enumerate all of the available providers, for example to allow user provider selection.
     * </p>
     *
     * <p>
     * This method may return different results at different times if new providers are dynamically made available to the current Java virtual machine.
     * </p>
     *
     * @return An immutable, map from names to provider objects
     * @since 1.13
     */
    public static SortedMap<String, ArchiveStreamProvider> findAvailableArchiveInputStreamProviders() {
        return AccessController.doPrivileged((PrivilegedAction<SortedMap<String, ArchiveStreamProvider>>) () -> {
            final TreeMap<String, ArchiveStreamProvider> map = new TreeMap<>();
            putAll(DEFAULT.getInputStreamArchiveNames(), DEFAULT, map);
            archiveStreamProviderIterable().forEach(provider -> putAll(provider.getInputStreamArchiveNames(), provider, map));
            return map;
        });
    }

    /**
     * Constructs a new sorted map from output stream provider names to provider objects.
     *
     * <p>
     * The map returned by this method will have one entry for each provider for which support is available in the current Java virtual machine. If two or more
     * supported provider have the same name then the resulting map will contain just one of them; which one it will contain is not specified.
     * </p>
     *
     * <p>
     * The invocation of this method, and the subsequent use of the resulting map, may cause time-consuming disk or network I/O operations to occur. This method
     * is provided for applications that need to enumerate all of the available providers, for example to allow user provider selection.
     * </p>
     *
     * <p>
     * This method may return different results at different times if new providers are dynamically made available to the current Java virtual machine.
     * </p>
     *
     * @return An immutable, map from names to provider objects
     * @since 1.13
     */
    public static SortedMap<String, ArchiveStreamProvider> findAvailableArchiveOutputStreamProviders() {
        return AccessController.doPrivileged((PrivilegedAction<SortedMap<String, ArchiveStreamProvider>>) () -> {
            final TreeMap<String, ArchiveStreamProvider> map = new TreeMap<>();
            putAll(DEFAULT.getOutputStreamArchiveNames(), DEFAULT, map);
            archiveStreamProviderIterable().forEach(provider -> putAll(provider.getOutputStreamArchiveNames(), provider, map));
            return map;
        });
    }

    private static boolean isName(final String value) {
        // Expect ASCII https://www.mkssoftware.com/docs/man4/tar.4.asp
        return value.isEmpty() || value.chars().allMatch(ch -> ch > 31 && ch < 128);
    }

    static void putAll(final Set<String> names, final ArchiveStreamProvider provider, final TreeMap<String, ArchiveStreamProvider> map) {
        names.forEach(name -> map.put(toKey(name), provider));
    }

    private static String toKey(final String name) {
        return StringUtils.toRootUpperCase(name);
    }

    /**
     * Entry encoding, null for the default.
     */
    private volatile String entryEncoding;

    private SortedMap<String, ArchiveStreamProvider> archiveInputStreamProviders;

    private SortedMap<String, ArchiveStreamProvider> archiveOutputStreamProviders;

    /**
     * Constructs an instance using the platform default encoding.
     */
    public ArchiveStreamFactory() {
        this(null);
    }

    /**
     * Constructs an instance using the specified encoding.
     *
     * @param entryEncoding the encoding to be used.
     * @since 1.10
     */
    public ArchiveStreamFactory(final String entryEncoding) {
        this.entryEncoding = entryEncoding;
    }

    /**
     * Creates an archive input stream from an input stream, autodetecting the archive type from the first few bytes of the stream. The InputStream must support
     * marks, like BufferedInputStream.
     *
     * @param <I> The {@link ArchiveInputStream} type.
     * @param in  the input stream
     * @return the archive input stream
     * @throws ArchiveException               if the archiver name is not known
     * @throws StreamingNotSupportedException if the format cannot be read from a stream
     * @throws IllegalArgumentException       if the stream is null or does not support mark
     */
    public <I extends ArchiveInputStream<? extends ArchiveEntry>> I createArchiveInputStream(final InputStream in) throws ArchiveException {
        return createArchiveInputStream(detect(in), in);
    }

    /**
     * Creates an archive input stream from an archiver name and an input stream.
     *
     * @param <I>          The {@link ArchiveInputStream} type.
     * @param archiverName the archive name, i.e. {@value #AR}, {@value #ARJ}, {@value #ZIP}, {@value #TAR}, {@value #JAR}, {@value #CPIO}, {@value #DUMP} or
     *                     {@value #SEVEN_Z}
     * @param in           the input stream
     * @return the archive input stream
     * @throws ArchiveException               if the archiver name is not known
     * @throws StreamingNotSupportedException if the format cannot be read from a stream
     * @throws IllegalArgumentException       if the archiver name or stream is null
     */
    public <I extends ArchiveInputStream<? extends ArchiveEntry>> I createArchiveInputStream(final String archiverName, final InputStream in)
            throws ArchiveException {
        return createArchiveInputStream(archiverName, in, entryEncoding);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I extends ArchiveInputStream<? extends ArchiveEntry>> I createArchiveInputStream(final String archiverName, final InputStream in,
            final String actualEncoding) throws ArchiveException {
        if (archiverName == null) {
            throw new IllegalArgumentException("Archiver name must not be null.");
        }
        if (in == null) {
            throw new IllegalArgumentException("InputStream must not be null.");
        }
        try {
            if (AR.equalsIgnoreCase(archiverName)) {
                return (I) ArArchiveInputStream.builder().setInputStream(in).get();
            }
            if (ARJ.equalsIgnoreCase(archiverName)) {
                final ArjArchiveInputStream.Builder arjBuilder = ArjArchiveInputStream.builder().setInputStream(in);
                if (actualEncoding != null) {
                    arjBuilder.setCharset(actualEncoding);
                }
                return (I) arjBuilder.get();
            }
            if (LHA.equalsIgnoreCase(archiverName)) {
                final LhaArchiveInputStream.Builder lhaBuilder = LhaArchiveInputStream.builder().setInputStream(in);
                if (actualEncoding != null) {
                    lhaBuilder.setCharset(actualEncoding);
                }
                return (I) lhaBuilder.get();
            }
            if (ZIP.equalsIgnoreCase(archiverName)) {
                final ZipArchiveInputStream.Builder zipBuilder = ZipArchiveInputStream.builder().setInputStream(in);
                if (actualEncoding != null) {
                    zipBuilder.setCharset(actualEncoding);
                }
                return (I) zipBuilder.get();
            }
            if (TAR.equalsIgnoreCase(archiverName)) {
                final TarArchiveInputStream.Builder tarBuilder = TarArchiveInputStream.builder().setInputStream(in);
                if (actualEncoding != null) {
                    tarBuilder.setCharset(actualEncoding);
                }
                return (I) tarBuilder.get();
            }
            if (JAR.equalsIgnoreCase(archiverName) || APK.equalsIgnoreCase(archiverName)) {
                final JarArchiveInputStream.Builder jarBuilder =
                        JarArchiveInputStream.jarInputStreamBuilder().setInputStream(in);
                if (actualEncoding != null) {
                    jarBuilder.setCharset(actualEncoding);
                }
                return (I) jarBuilder.get();
            }
            if (CPIO.equalsIgnoreCase(archiverName)) {
                final CpioArchiveInputStream.Builder cpioBuilder = CpioArchiveInputStream.builder().setInputStream(in);
                if (actualEncoding != null) {
                    cpioBuilder.setCharset(actualEncoding);
                }
                return (I) cpioBuilder.get();
            }
            if (DUMP.equalsIgnoreCase(archiverName)) {
                final DumpArchiveInputStream.Builder dumpBuilder = DumpArchiveInputStream.builder().setInputStream(in);
                if (actualEncoding != null) {
                    dumpBuilder.setCharset(actualEncoding);
                }
                return (I) dumpBuilder.get();
            }
            if (SEVEN_Z.equalsIgnoreCase(archiverName)) {
                throw new StreamingNotSupportedException(SEVEN_Z);
            }
            final ArchiveStreamProvider archiveStreamProvider = getArchiveInputStreamProviders().get(toKey(archiverName));
            if (archiveStreamProvider != null) {
                return archiveStreamProvider.createArchiveInputStream(archiverName, in, actualEncoding);
            }
            throw new ArchiveException("Archiver: %s not found.", archiverName);
        } catch (final ArchiveException e) {
            throw e;
        } catch (final IOException e) {
            throw new ArchiveException(
                    "IOException while creating " + archiverName + " input stream: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an archive output stream from an archiver name and an output stream.
     *
     * @param <O>          The {@link ArchiveOutputStream} type.
     * @param archiverName the archive name, i.e. {@value #AR}, {@value #ZIP}, {@value #TAR}, {@value #JAR} or {@value #CPIO}
     * @param out          the output stream
     * @return the archive output stream
     * @throws ArchiveException               if the archiver name is not known
     * @throws StreamingNotSupportedException if the format cannot be written to a stream
     * @throws IllegalArgumentException       if the archiver name or stream is null
     */
    public <O extends ArchiveOutputStream<? extends ArchiveEntry>> O createArchiveOutputStream(final String archiverName, final OutputStream out)
            throws ArchiveException {
        return createArchiveOutputStream(archiverName, out, entryEncoding);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O extends ArchiveOutputStream<? extends ArchiveEntry>> O createArchiveOutputStream(final String archiverName, final OutputStream out,
            final String actualEncoding) throws ArchiveException {
        if (archiverName == null) {
            throw new IllegalArgumentException("Archiver name must not be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("OutputStream must not be null.");
        }
        if (AR.equalsIgnoreCase(archiverName)) {
            return (O) new ArArchiveOutputStream(out);
        }
        if (ZIP.equalsIgnoreCase(archiverName)) {
            final ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
            if (actualEncoding != null) {
                zip.setEncoding(actualEncoding);
            }
            return (O) zip;
        }
        if (TAR.equalsIgnoreCase(archiverName)) {
            if (actualEncoding != null) {
                return (O) new TarArchiveOutputStream(out, actualEncoding);
            }
            return (O) new TarArchiveOutputStream(out);
        }
        if (JAR.equalsIgnoreCase(archiverName)) {
            if (actualEncoding != null) {
                return (O) new JarArchiveOutputStream(out, actualEncoding);
            }
            return (O) new JarArchiveOutputStream(out);
        }
        if (CPIO.equalsIgnoreCase(archiverName)) {
            if (actualEncoding != null) {
                return (O) new CpioArchiveOutputStream(out, actualEncoding);
            }
            return (O) new CpioArchiveOutputStream(out);
        }
        if (SEVEN_Z.equalsIgnoreCase(archiverName)) {
            throw new StreamingNotSupportedException(SEVEN_Z);
        }
        final ArchiveStreamProvider archiveStreamProvider = getArchiveOutputStreamProviders().get(toKey(archiverName));
        if (archiveStreamProvider != null) {
            return archiveStreamProvider.createArchiveOutputStream(archiverName, out, actualEncoding);
        }
        throw new ArchiveException("Archiver: %s not found.", archiverName);
    }

    /**
     * Gets an unmodifiable sorted map from input stream provider names to provider objects.
     *
     * @return an unmodifiable sorted map of from input stream provider names to provider objects.
     * @see #findAvailableArchiveInputStreamProviders()
     */
    public SortedMap<String, ArchiveStreamProvider> getArchiveInputStreamProviders() {
        if (archiveInputStreamProviders == null) {
            archiveInputStreamProviders = Collections.unmodifiableSortedMap(findAvailableArchiveInputStreamProviders());
        }
        return archiveInputStreamProviders;
    }

    /**
     * Gets an unmodifiable sorted map from output stream provider names to provider objects.
     *
     * @return an unmodifiable sorted map of from input stream provider names to provider objects.
     * @see #findAvailableArchiveInputStreamProviders()
     */
    public SortedMap<String, ArchiveStreamProvider> getArchiveOutputStreamProviders() {
        if (archiveOutputStreamProviders == null) {
            archiveOutputStreamProviders = Collections.unmodifiableSortedMap(findAvailableArchiveOutputStreamProviders());
        }
        return archiveOutputStreamProviders;
    }

    /**
     * Gets the encoding to use for arj, jar, ZIP, dump, cpio and tar files, or null for the archiver default.
     *
     * @return entry encoding, or null for the archiver default
     * @since 1.5
     */
    public String getEntryEncoding() {
        return entryEncoding;
    }

    @Override
    public Set<String> getInputStreamArchiveNames() {
        return Sets.newHashSet(AR, ARJ, LHA, ZIP, TAR, JAR, CPIO, DUMP, SEVEN_Z);
    }

    @Override
    public Set<String> getOutputStreamArchiveNames() {
        return Sets.newHashSet(AR, ZIP, TAR, JAR, CPIO, SEVEN_Z);
    }

    /**
     * Sets the encoding to use for arj, jar, ZIP, dump, cpio and tar files. Use null for the archiver default.
     *
     * @param entryEncoding the entry encoding, null uses the archiver default.
     * @since 1.5
     * @deprecated 1.10 use {@link #ArchiveStreamFactory(String)} to specify the encoding
     */
    @Deprecated
    public void setEntryEncoding(final String entryEncoding) {
        this.entryEncoding = entryEncoding;
    }

}

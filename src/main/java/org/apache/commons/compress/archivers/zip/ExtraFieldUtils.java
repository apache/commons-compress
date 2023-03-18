/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.zip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipException;

/**
 * ZipExtraField related methods
 * @NotThreadSafe because the HashMap is not synch.
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF (bc)
public class ExtraFieldUtils {

    /**
     * "enum" for the possible actions to take if the extra field
     * cannot be parsed.
     *
     * <p>This class has been created long before Java 5 and would
     * have been a real enum ever since.</p>
     *
     * @since 1.1
     */
    public static final class UnparseableExtraField implements UnparseableExtraFieldBehavior {
        /**
         * Key for "throw an exception" action.
         */
        public static final int THROW_KEY = 0;
        /**
         * Key for "skip" action.
         */
        public static final int SKIP_KEY = 1;
        /**
         * Key for "read" action.
         */
        public static final int READ_KEY = 2;

        /**
         * Throw an exception if field cannot be parsed.
         */
        public static final UnparseableExtraField THROW
            = new UnparseableExtraField(THROW_KEY);

        /**
         * Skip the extra field entirely and don't make its data
         * available - effectively removing the extra field data.
         */
        public static final UnparseableExtraField SKIP
            = new UnparseableExtraField(SKIP_KEY);

        /**
         * Read the extra field data into an instance of {@link
         * UnparseableExtraFieldData UnparseableExtraFieldData}.
         */
        public static final UnparseableExtraField READ
            = new UnparseableExtraField(READ_KEY);

        private final int key;

        private UnparseableExtraField(final int k) {
            key = k;
        }

        /**
         * Key of the action to take.
         * @return the key
         */
        public int getKey() { return key; }

        @Override
        public ZipExtraField onUnparseableExtraField(final byte[] data, final int off, final int len, final boolean local,
            final int claimedLength) throws ZipException {
            switch(key) {
            case THROW_KEY:
                throw new ZipException("Bad extra field starting at "
                                       + off + ".  Block length of "
                                       + claimedLength + " bytes exceeds remaining"
                                       + " data of "
                                       + (len - WORD)
                                       + " bytes.");
            case READ_KEY:
                final UnparseableExtraFieldData field = new UnparseableExtraFieldData();
                if (local) {
                    field.parseFromLocalFileData(data, off, len);
                } else {
                    field.parseFromCentralDirectoryData(data, off, len);
                }
                return field;
            case SKIP_KEY:
                return null;
            default:
                throw new ZipException("Unknown UnparseableExtraField key: " + key);
            }
        }

    }

    private static final int WORD = 4;

    /**
     * Static registry of known extra fields.
     */
    private static final Map<ZipShort, Class<?>> IMPLEMENTATIONS;

    static {
        IMPLEMENTATIONS = new ConcurrentHashMap<>();
        register(AsiExtraField.class);
        register(X5455_ExtendedTimestamp.class);
        register(X7875_NewUnix.class);
        register(JarMarker.class);
        register(UnicodePathExtraField.class);
        register(UnicodeCommentExtraField.class);
        register(Zip64ExtendedInformationExtraField.class);
        register(X000A_NTFS.class);
        register(X0014_X509Certificates.class);
        register(X0015_CertificateIdForFile.class);
        register(X0016_CertificateIdForCentralDirectory.class);
        register(X0017_StrongEncryptionHeader.class);
        register(X0019_EncryptionRecipientCertificateList.class);
        register(ResourceAlignmentExtraField.class);
    }

    static final ZipExtraField[] EMPTY_ZIP_EXTRA_FIELD_ARRAY = {};

    /**
     * Create an instance of the appropriate ExtraField, falls back to
     * {@link UnrecognizedExtraField UnrecognizedExtraField}.
     * @param headerId the header identifier
     * @return an instance of the appropriate ExtraField
     * @throws InstantiationException if unable to instantiate the class
     * @throws IllegalAccessException if not allowed to instantiate the class
     */
    public static ZipExtraField createExtraField(final ZipShort headerId)
        throws InstantiationException, IllegalAccessException {
        final ZipExtraField field = createExtraFieldNoDefault(headerId);
        if (field != null) {
            return field;
        }
        final UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(headerId);
        return u;
    }

    /**
     * Create an instance of the appropriate ExtraField.
     * @param headerId the header identifier
     * @return an instance of the appropriate ExtraField or null if
     * the id is not supported
     * @throws InstantiationException if unable to instantiate the class
     * @throws IllegalAccessException if not allowed to instantiate the class
     * @since 1.19
     */
    public static ZipExtraField createExtraFieldNoDefault(final ZipShort headerId)
        throws InstantiationException, IllegalAccessException {
        final Class<?> c = IMPLEMENTATIONS.get(headerId);
        if (c != null) {
            return (ZipExtraField) c.newInstance();
        }
        return null;
    }

    /**
     * Fills in the extra field data into the given instance.
     *
     * <p>Calls {@link ZipExtraField#parseFromCentralDirectoryData} or {@link ZipExtraField#parseFromLocalFileData} internally and wraps any {@link ArrayIndexOutOfBoundsException} thrown into a {@link ZipException}.</p>
     *
     * @param ze the extra field instance to fill
     * @param data the array of extra field data
     * @param off offset into data where this field's data starts
     * @param len the length of this field's data
     * @param local whether the extra field data stems from the local
     * file header. If this is false then the data is part if the
     * central directory header extra data.
     * @return the filled field, will never be {@code null}
     * @throws ZipException if an error occurs
     *
     * @since 1.19
     */
    public static ZipExtraField fillExtraField(final ZipExtraField ze, final byte[] data, final int off,
        final int len, final boolean local) throws ZipException {
        try {
            if (local) {
                ze.parseFromLocalFileData(data, off, len);
            } else {
                ze.parseFromCentralDirectoryData(data, off, len);
            }
            return ze;
        } catch (final ArrayIndexOutOfBoundsException aiobe) {
            throw (ZipException) new ZipException("Failed to parse corrupt ZIP extra field of type "
                + Integer.toHexString(ze.getHeaderId().getValue())).initCause(aiobe);
        }
    }

    /**
     * Merges the central directory fields of the given ZipExtraFields.
     * @param data an array of ExtraFields
     * @return an array of bytes
     */
    public static byte[] mergeCentralDirectoryData(final ZipExtraField[] data) {
        final int dataLength = data.length;
        final boolean lastIsUnparseableHolder = dataLength > 0
            && data[dataLength - 1] instanceof UnparseableExtraFieldData;
        final int regularExtraFieldCount =
            lastIsUnparseableHolder ? dataLength - 1 : dataLength;

        int sum = WORD * regularExtraFieldCount;
        for (final ZipExtraField element : data) {
            sum += element.getCentralDirectoryLength().getValue();
        }
        final byte[] result = new byte[sum];
        int start = 0;
        for (int i = 0; i < regularExtraFieldCount; i++) {
            System.arraycopy(data[i].getHeaderId().getBytes(),
                             0, result, start, 2);
            System.arraycopy(data[i].getCentralDirectoryLength().getBytes(),
                             0, result, start + 2, 2);
            start += WORD;
            final byte[] central = data[i].getCentralDirectoryData();
            if (central != null) {
                System.arraycopy(central, 0, result, start, central.length);
                start += central.length;
            }
        }
        if (lastIsUnparseableHolder) {
            final byte[] central = data[dataLength - 1].getCentralDirectoryData();
            if (central != null) {
                System.arraycopy(central, 0, result, start, central.length);
            }
        }
        return result;
    }

    /**
     * Merges the local file data fields of the given ZipExtraFields.
     * @param data an array of ExtraFiles
     * @return an array of bytes
     */
    public static byte[] mergeLocalFileDataData(final ZipExtraField[] data) {
        final int dataLength = data.length;
        final boolean lastIsUnparseableHolder = dataLength > 0
            && data[dataLength - 1] instanceof UnparseableExtraFieldData;
        final int regularExtraFieldCount =
            lastIsUnparseableHolder ? dataLength - 1 : dataLength;

        int sum = WORD * regularExtraFieldCount;
        for (final ZipExtraField element : data) {
            sum += element.getLocalFileDataLength().getValue();
        }

        final byte[] result = new byte[sum];
        int start = 0;
        for (int i = 0; i < regularExtraFieldCount; i++) {
            System.arraycopy(data[i].getHeaderId().getBytes(),
                             0, result, start, 2);
            System.arraycopy(data[i].getLocalFileDataLength().getBytes(),
                             0, result, start + 2, 2);
            start += WORD;
            final byte[] local = data[i].getLocalFileDataData();
            if (local != null) {
                System.arraycopy(local, 0, result, start, local.length);
                start += local.length;
            }
        }
        if (lastIsUnparseableHolder) {
            final byte[] local = data[dataLength - 1].getLocalFileDataData();
            if (local != null) {
                System.arraycopy(local, 0, result, start, local.length);
            }
        }
        return result;
    }

    /**
     * Split the array into ExtraFields and populate them with the
     * given data as local file data, throwing an exception if the
     * data cannot be parsed.
     * @param data an array of bytes as it appears in local file data
     * @return an array of ExtraFields
     * @throws ZipException on error
     */
    public static ZipExtraField[] parse(final byte[] data) throws ZipException {
        return parse(data, true, UnparseableExtraField.THROW);
    }

    /**
     * Split the array into ExtraFields and populate them with the
     * given data, throwing an exception if the data cannot be parsed.
     * @param data an array of bytes
     * @param local whether data originates from the local file data
     * or the central directory
     * @return an array of ExtraFields
     * @throws ZipException on error
     */
    public static ZipExtraField[] parse(final byte[] data, final boolean local)
        throws ZipException {
        return parse(data, local, UnparseableExtraField.THROW);
    }

    /**
     * Split the array into ExtraFields and populate them with the
     * given data.
     * @param data an array of bytes
     * @param parsingBehavior controls parsing of extra fields.
     * @param local whether data originates from the local file data
     * or the central directory
     * @return an array of ExtraFields
     * @throws ZipException on error
     *
     * @since 1.19
     */
    public static ZipExtraField[] parse(final byte[] data, final boolean local,
                                        final ExtraFieldParsingBehavior parsingBehavior)
        throws ZipException {
        final List<ZipExtraField> v = new ArrayList<>();
        int start = 0;
        final int dataLength = data.length;
        LOOP:
        while (start <= dataLength - WORD) {
            final ZipShort headerId = new ZipShort(data, start);
            final int length = new ZipShort(data, start + 2).getValue();
            if (start + WORD + length > dataLength) {
                final ZipExtraField field = parsingBehavior.onUnparseableExtraField(data, start, dataLength - start,
                    local, length);
                if (field != null) {
                    v.add(field);
                }
                // since we cannot parse the data we must assume
                // the extra field consumes the whole rest of the
                // available data
                break LOOP;
            }
            try {
                final ZipExtraField ze = Objects.requireNonNull(parsingBehavior.createExtraField(headerId),
                    "createExtraField must not return null");
                v.add(Objects.requireNonNull(parsingBehavior.fill(ze, data, start + WORD, length, local),
                    "fill must not return null"));
                start += length + WORD;
            } catch (final InstantiationException | IllegalAccessException ie) {
                throw (ZipException) new ZipException(ie.getMessage()).initCause(ie);
            }
        }

        return v.toArray(EMPTY_ZIP_EXTRA_FIELD_ARRAY);
    }

    /**
     * Split the array into ExtraFields and populate them with the
     * given data.
     * @param data an array of bytes
     * @param local whether data originates from the local file data
     * or the central directory
     * @param onUnparseableData what to do if the extra field data
     * cannot be parsed.
     * @return an array of ExtraFields
     * @throws ZipException on error
     *
     * @since 1.1
     */
    public static ZipExtraField[] parse(final byte[] data, final boolean local,
                                        final UnparseableExtraField onUnparseableData)
        throws ZipException {
        return parse(data, local, new ExtraFieldParsingBehavior() {
            @Override
            public ZipExtraField createExtraField(final ZipShort headerId)
                throws ZipException, InstantiationException, IllegalAccessException {
                return ExtraFieldUtils.createExtraField(headerId);
            }

            @Override
            public ZipExtraField fill(final ZipExtraField field, final byte[] data, final int off, final int len, final boolean local)
                throws ZipException {
                return fillExtraField(field, data, off, len, local);
            }

            @Override
            public ZipExtraField onUnparseableExtraField(final byte[] data, final int off, final int len, final boolean local,
                final int claimedLength) throws ZipException {
                return onUnparseableData.onUnparseableExtraField(data, off, len, local, claimedLength);
            }
        });
    }

    /**
     * Register a ZipExtraField implementation.
     *
     * <p>The given class must have a no-arg constructor and implement
     * the {@link ZipExtraField ZipExtraField interface}.</p>
     * @param c the class to register
     */
    public static void register(final Class<?> c) {
        try {
            final ZipExtraField ze = (ZipExtraField) c.newInstance();
            IMPLEMENTATIONS.put(ze.getHeaderId(), c);
        } catch (final ClassCastException cc) { // NOSONAR
            throw new IllegalArgumentException(c + " doesn't implement ZipExtraField"); //NOSONAR
        } catch (final InstantiationException ie) { // NOSONAR
            throw new IllegalArgumentException(c + " is not a concrete class"); //NOSONAR
        } catch (final IllegalAccessException ie) { // NOSONAR
            throw new IllegalArgumentException(c + "'s no-arg constructor is not public"); //NOSONAR
        }
    }
}

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
package org.apache.commons.compress.harmony.unpack200;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.CodecEncoding;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PopulationCodec;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPClass;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPDouble;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPFieldRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPFloat;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPInteger;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPInterfaceMethodRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPLong;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPMethodRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPNameAndType;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPString;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.apache.commons.compress.utils.ExactMath;

/**
 * Abstract superclass for a set of bands.
 */
public abstract class BandSet {

    /**
     * Segment.
     */
    protected Segment segment;

    /**
     * Segment header.
     */
    protected SegmentHeader header;

    /**
     * Constructs a new instance for the given segment.
     *
     * @param segment The segment.
     */
    public BandSet(final Segment segment) {
        this.segment = segment;
        this.header = segment.getSegmentHeader();
    }

    /**
     * Decodes a band and return an array of {@code int} values.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the InputStream to decode.
     * @param codec the default Codec for this band.
     * @param count the number of elements to read.
     * @return an array of decoded {@code int} values.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public int[] decodeBandInt(final String name, final InputStream in, final BHSDCodec codec, final int count) throws IOException, Pack200Exception {
        if (count < 0) {
            throw new Pack200Exception("count < 0");
        }
        // Useful for debugging
//        if (count > 0) {
//            System.out.println("decoding " + name + " " + count);
//        }
        Codec codecUsed = codec;
        if (codec.getB() == 1 || count == 0) {
            return codec.decodeInts(count, in);
        }
        final int[] getFirst = codec.decodeInts(1, in);
        if (getFirst.length == 0) {
            return getFirst;
        }
        final int first = getFirst[0];
        final int[] band;
        if (codec.isSigned() && first >= -256 && first <= -1) {
            // Non-default codec should be used
            codecUsed = CodecEncoding.getCodec(-1 - first, header.getBandHeadersInputStream(), codec);
            band = codecUsed.decodeInts(count, in);
        } else if (!codec.isSigned() && first >= codec.getL() && first <= codec.getL() + 255) {
            // Non-default codec should be used
            codecUsed = CodecEncoding.getCodec(first - codec.getL(), header.getBandHeadersInputStream(), codec);
            band = codecUsed.decodeInts(count, in);
        } else {
            // First element should not be discarded
            band = codec.decodeInts(count - 1, in, first);
        }
        // Useful for debugging -E options:
        // if (!codecUsed.equals(codec)) {
        // int bytes = codecUsed.lastBandLength;
        // System.out.println(count + " " + name + " encoded with " + codecUsed + " " + bytes);
        // }
        if (codecUsed instanceof PopulationCodec) {
            final PopulationCodec popCodec = (PopulationCodec) codecUsed;
            final int[] favoured = popCodec.getFavoured().clone();
            Arrays.sort(favoured);
            for (int i = 0; i < band.length; i++) {
                final boolean favouredValue = Arrays.binarySearch(favoured, band[i]) > -1;
                final Codec theCodec = favouredValue ? popCodec.getFavouredCodec() : popCodec.getUnfavouredCodec();
                if (theCodec instanceof BHSDCodec && ((BHSDCodec) theCodec).isDelta()) {
                    final BHSDCodec bhsd = (BHSDCodec) theCodec;
                    final long cardinality = bhsd.cardinality();
                    while (band[i] > bhsd.largest()) {
                        band[i] -= cardinality;
                    }
                    while (band[i] < bhsd.smallest()) {
                        band[i] = ExactMath.add(band[i], cardinality);
                    }
                }
            }
        }
        return band;
    }

    /**
     * Decodes a band and return an array of {@code int[]} values.
     *
     * @param name         the name of the band (primarily for logging/debugging purposes)
     * @param in           the InputStream to decode from
     * @param defaultCodec the default codec for this band
     * @param counts       the numbers of elements to read for each int array within the array to be returned
     * @return an array of decoded {@code int[]} values
     * @throws IOException      if there is a problem reading from the underlying input stream
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid
     */
    public int[][] decodeBandInt(final String name, final InputStream in, final BHSDCodec defaultCodec, final int[] counts)
            throws IOException, Pack200Exception {
        final int[][] result = new int[counts.length][];
        int totalCount = 0;
        for (final int count : counts) {
            totalCount += count;
        }
        final int[] twoDResult = decodeBandInt(name, in, defaultCodec, totalCount);
        int index = 0;
        for (int i = 0; i < result.length; i++) {
            if (counts[i] > twoDResult.length) {
                throw new IOException("Counts value exceeds length of twoDResult");
            }
            result[i] = new int[counts[i]];
            for (int j = 0; j < result[i].length; j++) {
                result[i][j] = twoDResult[index];
                index++;
            }
        }
        return result;
    }

    /**
     * Gets a new array of String references for the subset defined by the given {@code ints} array.
     *
     * @param ints The indices into the {@code reference} array.
     * @param reference The source array.
     * @return a new array.
     */
    protected String[] getReferences(final int[] ints, final String[] reference) {
        final String[] result = new String[ints.length];
        Arrays.setAll(result, i -> reference[ints[i]]);
        return result;
    }

    /**
     * Gets a new array of String references for the subset defined by the given {@code ints} array.
     *
     * @param ints The indices into the {@code reference} array.
     * @param reference The source array.
     * @return a new array.
     */
    protected String[][] getReferences(final int[][] ints, final String[] reference) {
        final String[][] result = new String[ints.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = new String[ints[i].length];
            for (int j = 0; j < result[i].length; j++) {
                result[i][j] = reference[ints[i][j]];
            }
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPClass}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPClass}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPClass[] parseCPClassReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPClass[] result = new CPClass[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            result[i1] = segment.getCpBands().cpClassValue(indices[i1]);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPNameAndType}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPNameAndType}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPNameAndType[] parseCPDescriptorReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final CpBands cpBands = segment.getCpBands();
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPNameAndType[] result = new CPNameAndType[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            final int index = indices[i1];
            result[i1] = cpBands.cpNameAndTypeValue(index);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPDouble}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPDouble}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPDouble[] parseCPDoubleReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPDouble[] result = new CPDouble[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            result[i1] = segment.getCpBands().cpDoubleValue(indices[i1]);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPFieldRef}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPFieldRef}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPFieldRef[] parseCPFieldRefReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final CpBands cpBands = segment.getCpBands();
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPFieldRef[] result = new CPFieldRef[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            final int index = indices[i1];
            result[i1] = cpBands.cpFieldValue(index);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPFloat}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPFloat}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPFloat[] parseCPFloatReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPFloat[] result = new CPFloat[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            result[i1] = segment.getCpBands().cpFloatValue(indices[i1]);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPInterfaceMethodRef}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPInterfaceMethodRef}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPInterfaceMethodRef[] parseCPInterfaceMethodRefReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final CpBands cpBands = segment.getCpBands();
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPInterfaceMethodRef[] result = new CPInterfaceMethodRef[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            result[i1] = cpBands.cpIMethodValue(indices[i1]);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPInteger}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPInteger}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPInteger[] parseCPIntReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final int[] reference = segment.getCpBands().getCpInt();
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPInteger[] result = new CPInteger[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            final int index = indices[i1];
            if (index < 0 || index >= reference.length) {
                throw new Pack200Exception("Something has gone wrong during parsing references, index = " + index + ", array size = " + reference.length);
            }
            result[i1] = segment.getCpBands().cpIntegerValue(index);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPLong}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPLong}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPLong[] parseCPLongReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final long[] reference = segment.getCpBands().getCpLong();
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPLong[] result = new CPLong[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            final int index = indices[i1];
            if (index < 0 || index >= reference.length) {
                throw new Pack200Exception("Something has gone wrong during parsing references, index = " + index + ", array size = " + reference.length);
            }
            result[i1] = segment.getCpBands().cpLongValue(index);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPMethodRef}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPMethodRef}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPMethodRef[] parseCPMethodRefReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final CpBands cpBands = segment.getCpBands();
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPMethodRef[] result = new CPMethodRef[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            result[i1] = cpBands.cpMethodValue(indices[i1]);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPUTF8}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPUTF8}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPUTF8[] parseCPSignatureReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPUTF8[] result = new CPUTF8[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            result[i1] = segment.getCpBands().cpSignatureValue(indices[i1]);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of array of {@link CPUTF8}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param counts the number of elements to read.
     * @return an array of array of decoded {@link CPUTF8}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    protected CPUTF8[][] parseCPSignatureReferences(final String name, final InputStream in, final BHSDCodec codec, final int[] counts)
            throws IOException, Pack200Exception {
        int sum = 0;
        for (final int count : counts) {
            sum += count;
        }
        final int[] indices = decodeBandInt(name, in, codec, sum);
        final CPUTF8[] result1 = new CPUTF8[sum];
        for (int i1 = 0; i1 < sum; i1++) {
            result1[i1] = segment.getCpBands().cpSignatureValue(indices[i1]);
        }
        int pos = 0;
        final CPUTF8[][] result = new CPUTF8[counts.length][];
        for (int i = 0; i < counts.length; i++) {
            final int num = counts[i];
            result[i] = new CPUTF8[num];
            System.arraycopy(result1, pos, result[i], 0, num);
            pos += num;
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPString}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPString}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPString[] parseCPStringReferences(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPString[] result = new CPString[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            result[i1] = segment.getCpBands().cpStringValue(indices[i1]);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of {@link CPUTF8}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param count the number of elements to read.
     * @return an array of decoded {@link CPUTF8}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPUTF8[] parseCPUTF8References(final String name, final InputStream in, final BHSDCodec codec, final int count)
            throws IOException, Pack200Exception {
        final int[] indices = decodeBandInt(name, in, codec, count);
        final CPUTF8[] result = new CPUTF8[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            final int index = indices[i1];
            result[i1] = segment.getCpBands().cpUTF8Value(index);
        }
        return result;
    }

    /**
     * Parses an input stream into an array of array of {@link CPUTF8}.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the input stream to parse.
     * @param codec the default {@link BHSDCodec} for this band
     * @param counts the number of elements to read.
     * @return an array of array of decoded {@link CPUTF8}.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public CPUTF8[][] parseCPUTF8References(final String name, final InputStream in, final BHSDCodec codec, final int[] counts)
            throws IOException, Pack200Exception {
        final CPUTF8[][] result = new CPUTF8[counts.length][];
        int sum = 0;
        for (int i = 0; i < counts.length; i++) {
            result[i] = new CPUTF8[counts[i]];
            sum += counts[i];
        }
        final CPUTF8[] result1 = new CPUTF8[sum];
        final int[] indices = decodeBandInt(name, in, codec, sum);
        for (int i1 = 0; i1 < sum; i1++) {
            final int index = indices[i1];
            result1[i1] = segment.getCpBands().cpUTF8Value(index);
        }
        int pos = 0;
        for (int i = 0; i < counts.length; i++) {
            final int num = counts[i];
            result[i] = new CPUTF8[num];
            System.arraycopy(result1, pos, result[i], 0, num);
            pos += num;
        }
        return result;
    }

    /**
     * Parses an input stream into an array of flags.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the InputStream to decode.
     * @param count the number of elements to read.
     * @param hiCodec an optional codec if the high flag is on.
     * @param loCodec the codec.
     * @return an array of decoded {@code long} flags.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public long[] parseFlags(final String name, final InputStream in, final int count, final BHSDCodec hiCodec, final BHSDCodec loCodec)
            throws IOException, Pack200Exception {
        return parseFlags(name, in, new int[] { count }, hiCodec, loCodec)[0];
    }

    /**
     * Parses an input stream into an array of flags.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the InputStream to decode.
     * @param count the number of elements to read.
     * @param codec the codec.
     * @param hasHi whether to the high flag is enabled.
     * @return an array of decoded {@code long} flags.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public long[] parseFlags(final String name, final InputStream in, final int count, final BHSDCodec codec, final boolean hasHi)
            throws IOException, Pack200Exception {
        return parseFlags(name, in, new int[] { count }, hasHi ? codec : null, codec)[0];
    }

    /**
     * Parses an input stream into an array of flags.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the InputStream to decode.
     * @param counts the number of elements to read.
     * @param hiCodec an optional codec if the high flag is on.
     * @param loCodec the codec.
     * @return an array of decoded {@code long} flags.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public long[][] parseFlags(final String name, final InputStream in, final int[] counts, final BHSDCodec hiCodec, final BHSDCodec loCodec)
            throws IOException, Pack200Exception {
        final int count = counts.length;
        if (count == 0) {
            return new long[][] { {} };
        }
        int sum = 0;
        final long[][] result = new long[count][];
        for (int i = 0; i < count; i++) {
            sum += counts[i];
        }
        int[] hi = null;
        final int[] lo;
        if (hiCodec != null) {
            hi = decodeBandInt(name, in, hiCodec, sum);
        }
        lo = decodeBandInt(name, in, loCodec, sum);

        int index = 0;
        for (int i = 0; i < count; i++) {
            result[i] = new long[counts[i]];
            for (int j = 0; j < result[i].length; j++) {
                if (hi != null) {
                    result[i][j] = (long) hi[index] << 32 | lo[index] & 4294967295L;
                } else {
                    result[i][j] = lo[index];
                }
                index++;
            }
        }
        return result;
    }

    /**
     * Parses an input stream into an array of flags.
     *
     * @param name  the name of the band (for logging and debugging).
     * @param in    the InputStream to decode.
     * @param counts the number of elements to read.
     * @param codec the codec.
     * @param hasHi whether the high flag is enabnled.
     * @return an array of decoded {@code long} flags.
     * @throws IOException      if there is a problem reading from the underlying input stream.
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid.
     */
    public long[][] parseFlags(final String name, final InputStream in, final int[] counts, final BHSDCodec codec, final boolean hasHi)
            throws IOException, Pack200Exception {
        return parseFlags(name, in, counts, hasHi ? codec : null, codec);
    }

    /**
     * Parses <em>count</em> references from {@code in}, using {@code codec} to decode the values as indexes into {@code reference} (which is populated prior to
     * this call). An exception is thrown if a decoded index falls outside the range [0..reference.length-1].
     *
     * @param name      the band name
     * @param in        the input stream to read from
     * @param codec     the BHSDCodec to use for decoding
     * @param count     the number of references to decode
     * @param reference the array of values to use for the references
     * @return Parsed references.
     * @throws IOException      if a problem occurs during reading from the underlying stream.
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported Codec.
     */
    public String[] parseReferences(final String name, final InputStream in, final BHSDCodec codec, final int count, final String[] reference)
            throws IOException, Pack200Exception {
        return parseReferences(name, in, codec, new int[] { count }, reference)[0];
    }

    /**
     * Parses <em>count</em> references from {@code in}, using {@code codec} to decode the values as indexes into {@code reference} (which is populated prior to
     * this call). An exception is thrown if a decoded index falls outside the range [0..reference.length-1]. Unlike the other parseReferences, this
     * post-processes the result into an array of results.
     *
     * @param name      TODO
     * @param in        the input stream to read from
     * @param codec     the BHSDCodec to use for decoding
     * @param counts    the numbers of references to decode for each array entry
     * @param reference the array of values to use for the references
     * @return Parsed references.
     * @throws IOException      if a problem occurs during reading from the underlying stream.
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported Codec.
     */
    public String[][] parseReferences(final String name, final InputStream in, final BHSDCodec codec, final int[] counts, final String[] reference)
            throws IOException, Pack200Exception {
        final int count = counts.length;
        if (count == 0) {
            return new String[][] { {} };
        }
        int sum = 0;
        for (int i = 0; i < count; i++) {
            sum += counts[i];
        }
        // TODO Merge the decode and parsing of a multiple structure into one
        final int[] indices = decodeBandInt(name, in, codec, sum);
        final String[] result1 = new String[sum];
        for (int i1 = 0; i1 < sum; i1++) {
            final int index = indices[i1];
            if (index < 0 || index >= reference.length) {
                throw new Pack200Exception("Something has gone wrong during parsing references, index = " + index + ", array size = " + reference.length);
            }
            result1[i1] = reference[index];
        }
        // TODO Merge the decode and parsing of a multiple structure into one
        final String[][] result = new String[count][];
        int pos = 0;
        for (int i = 0; i < count; i++) {
            final int num = counts[i];
            result[i] = new String[num];
            System.arraycopy(result1, pos, result[i], 0, num);
            pos += num;
        }
        return result;
    }

    /**
     * Reads the input stream.
     *
     * @param inputStream the stream to read.
     * @throws IOException      if a problem occurs during reading from the underlying stream.
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported Codec.
     */
    public abstract void read(InputStream inputStream) throws IOException, Pack200Exception;

    /**
     * Unpacks this instance.
     *
     * @throws IOException      if a problem occurs during reading from the underlying stream.
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported Codec.
     */
    public abstract void unpack() throws IOException, Pack200Exception;

    /**
     * Unpacks the input stream.
     *
     * @param in the stream to unpack.
     * @throws IOException      if a problem occurs during reading from the underlying stream.
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported Codec.
     */
    public void unpack(final InputStream in) throws IOException, Pack200Exception {
        read(in);
        unpack();
    }

}

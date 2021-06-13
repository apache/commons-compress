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

/**
 * Abstract superclass for a set of bands
 */
public abstract class BandSet {

    public abstract void read(InputStream inputStream) throws IOException,
            Pack200Exception;

    public abstract void unpack() throws IOException, Pack200Exception;

    public void unpack(InputStream in) throws IOException, Pack200Exception {
        read(in);
        unpack();
    }

    protected Segment segment;

    protected SegmentHeader header;

    public BandSet(Segment segment) {
        this.segment = segment;
        this.header = segment.getSegmentHeader();
    }

    /**
     * Decode a band and return an array of <code>int</code> values
     *
     * @param name
     *            the name of the band (primarily for logging/debugging
     *            purposes)
     * @param in
     *            the InputStream to decode from
     * @param codec
     *            the default Codec for this band
     * @param count
     *            the number of elements to read
     * @return an array of decoded <code>int</code> values
     * @throws IOException
     *             if there is a problem reading from the underlying input
     *             stream
     * @throws Pack200Exception
     *             if there is a problem decoding the value or that the value is
     *             invalid
     */
    public int[] decodeBandInt(String name, InputStream in, BHSDCodec codec,
            int count) throws IOException, Pack200Exception {
        int[] band;
        // Useful for debugging
//        if(count > 0) {
//            System.out.println("decoding " + name + " " + count);
//        }
        Codec codecUsed = codec;
        if (codec.getB() == 1 || count == 0) {
            return codec.decodeInts(count, in);
        }
        int[] getFirst = codec.decodeInts(1, in);
        if (getFirst.length == 0) {
            return getFirst;
        }
        int first = getFirst[0];
        if (codec.isSigned() && first >= -256 && first <= -1) {
            // Non-default codec should be used
            codecUsed = CodecEncoding.getCodec((-1 - first), header
                    .getBandHeadersInputStream(), codec);
            band = codecUsed.decodeInts(count, in);
        } else if (!codec.isSigned() && first >= codec.getL()
                && first <= codec.getL() + 255) {
            // Non-default codec should be used
            codecUsed = CodecEncoding.getCodec(first - codec.getL(), header
                    .getBandHeadersInputStream(), codec);
            band = codecUsed.decodeInts(count, in);
        } else {
            // First element should not be discarded
            band = codec.decodeInts(count - 1, in, first);
        }
        // Useful for debugging -E options:
        //if(!codecUsed.equals(codec)) {
        //    int bytes = codecUsed.lastBandLength;
        //    System.out.println(count + " " + name + " encoded with " + codecUsed + " "  + bytes);
        //}
        if (codecUsed instanceof PopulationCodec) {
            PopulationCodec popCodec = (PopulationCodec) codecUsed;
            int[] favoured = (int[]) popCodec.getFavoured().clone();
            Arrays.sort(favoured);
            for (int i = 0; i < band.length; i++) {
                boolean favouredValue = Arrays.binarySearch(favoured, band[i]) > -1;
                Codec theCodec = favouredValue ? popCodec.getFavouredCodec()
                        : popCodec.getUnfavouredCodec();
                if (theCodec instanceof BHSDCodec
                        && ((BHSDCodec) theCodec).isDelta()) {
                    BHSDCodec bhsd = (BHSDCodec) theCodec;
                    long cardinality = bhsd.cardinality();
                    while (band[i] > bhsd.largest()) {
                        band[i] -= cardinality;
                    }
                    while (band[i] < bhsd.smallest()) {
                        band[i] += cardinality;
                    }
                }
            }
        }
        return band;
    }

    /**
     * Decode a band and return an array of <code>int[]</code> values
     *
     * @param name
     *            the name of the band (primarily for logging/debugging
     *            purposes)
     * @param in
     *            the InputStream to decode from
     * @param defaultCodec
     *            the default codec for this band
     * @param counts
     *            the numbers of elements to read for each int array within the
     *            array to be returned
     * @return an array of decoded <code>int[]</code> values
     * @throws IOException
     *             if there is a problem reading from the underlying input
     *             stream
     * @throws Pack200Exception
     *             if there is a problem decoding the value or that the value is
     *             invalid
     */
    public int[][] decodeBandInt(String name, InputStream in,
            BHSDCodec defaultCodec, int[] counts) throws IOException,
            Pack200Exception {
        int[][] result = new int[counts.length][];
        int totalCount = 0;
        for (int i = 0; i < counts.length; i++) {
            totalCount += counts[i];
        }
        int[] twoDResult = decodeBandInt(name, in, defaultCodec, totalCount);
        int index = 0;
        for (int i = 0; i < result.length; i++) {
            result[i] = new int[counts[i]];
            for (int j = 0; j < result[i].length; j++) {
                result[i][j] = twoDResult[index];
                index++;
            }
        }
        return result;
    }

    public long[] parseFlags(String name, InputStream in, int count,
            BHSDCodec codec, boolean hasHi) throws IOException,
            Pack200Exception {
        return parseFlags(name, in, new int[] { count },
                (hasHi ? codec : null), codec)[0];
    }

    public long[][] parseFlags(String name, InputStream in, int counts[],
            BHSDCodec codec, boolean hasHi) throws IOException,
            Pack200Exception {
        return parseFlags(name, in, counts, (hasHi ? codec : null), codec);
    }

    public long[] parseFlags(String name, InputStream in, int count,
            BHSDCodec hiCodec, BHSDCodec loCodec) throws IOException,
            Pack200Exception {
        return parseFlags(name, in, new int[] { count }, hiCodec, loCodec)[0];
    }

    public long[][] parseFlags(String name, InputStream in, int counts[],
            BHSDCodec hiCodec, BHSDCodec loCodec) throws IOException,
            Pack200Exception {
        int count = counts.length;
        if (count == 0) {
            return new long[][] { {} };
        }
        int sum = 0;
        long[][] result = new long[count][];
        for (int i = 0; i < count; i++) {
            result[i] = new long[counts[i]];
            sum += counts[i];
        }
        int[] hi = null;
        int[] lo;
        if (hiCodec != null) {
            hi = decodeBandInt(name, in, hiCodec, sum);
            lo = decodeBandInt(name, in, loCodec, sum);
        } else {
            lo = decodeBandInt(name, in, loCodec, sum);
        }

        int index = 0;
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[i].length; j++) {
                if (hi != null) {
                    result[i][j] = ((long) hi[index] << 32)
                            | (lo[index] & 4294967295L);
                } else {
                    result[i][j] = lo[index];
                }
                index++;
            }
        }
        return result;
    }

    /**
     * Parses <i>count</i> references from <code>in</code>,
     * using <code>codec</code> to decode the values as indexes into
     * <code>reference</code> (which is populated prior to this call). An
     * exception is thrown if a decoded index falls outside the range
     * [0..reference.length-1].
     *
     * @param name
     *            the band name
     * @param in
     *            the input stream to read from
     * @param codec
     *            the BHSDCodec to use for decoding
     * @param count
     *            the number of references to decode
     * @param reference
     *            the array of values to use for the references
     * @return Parsed references.
     *
     * @throws IOException
     *             if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception
     *             if a problem occurs with an unexpected value or unsupported
     *             Codec
     */
    public String[] parseReferences(String name, InputStream in,
            BHSDCodec codec, int count, String[] reference) throws IOException,
            Pack200Exception {
        return parseReferences(name, in, codec, new int[] { count }, reference)[0];
    }

    /**
     * Parses <i>count</i> references from <code>in</code>,
     * using <code>codec</code> to decode the values as indexes into
     * <code>reference</code> (which is populated prior to this call). An
     * exception is thrown if a decoded index falls outside the range
     * [0..reference.length-1]. Unlike the other parseReferences, this
     * post-processes the result into an array of results.
     *
     * @param name
     *            TODO
     * @param in
     *            the input stream to read from
     * @param codec
     *            the BHSDCodec to use for decoding
     * @param counts
     *            the numbers of references to decode for each array entry
     * @param reference
     *            the array of values to use for the references
     * @return Parsed references. 
     *
     * @throws IOException
     *             if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception
     *             if a problem occurs with an unexpected value or unsupported
     *             Codec
     */
    public String[][] parseReferences(String name, InputStream in,
            BHSDCodec codec, int counts[], String[] reference)
            throws IOException, Pack200Exception {
        int count = counts.length;
        if (count == 0) {
            return new String[][] { {} };
        }
        String[][] result = new String[count][];
        int sum = 0;
        for (int i = 0; i < count; i++) {
            result[i] = new String[counts[i]];
            sum += counts[i];
        }
        // TODO Merge the decode and parsing of a multiple structure into one
        String[] result1 = new String[sum];
        int[] indices = decodeBandInt(name, in, codec, sum);
        for (int i1 = 0; i1 < sum; i1++) {
            int index = indices[i1];
            if (index < 0 || index >= reference.length)
                throw new Pack200Exception(
                        "Something has gone wrong during parsing references, index = "
                                + index + ", array size = " + reference.length);
            result1[i1] = reference[index];
        }
        String[] refs = result1;
        // TODO Merge the decode and parsing of a multiple structure into one
        int pos = 0;
        for (int i = 0; i < count; i++) {
            int num = counts[i];
            result[i] = new String[num];
            System.arraycopy(refs, pos, result[i], 0, num);
            pos += num;
        }
        return result;
    }

    public CPInteger[] parseCPIntReferences(String name, InputStream in,
            BHSDCodec codec, int count) throws IOException, Pack200Exception {
        int[] reference = segment.getCpBands().getCpInt();
        int[] indices = decodeBandInt(name, in, codec, count);
        CPInteger[] result = new CPInteger[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            if (index < 0 || index >= reference.length)
                throw new Pack200Exception(
                        "Something has gone wrong during parsing references, index = "
                                + index + ", array size = " + reference.length);
            result[i1] = segment.getCpBands().cpIntegerValue(index);
        }
        return result;
    }

    public CPDouble[] parseCPDoubleReferences(String name, InputStream in,
            BHSDCodec codec, int count) throws IOException, Pack200Exception {
        int[] indices = decodeBandInt(name, in, codec, count);
        CPDouble[] result = new CPDouble[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = segment.getCpBands().cpDoubleValue(index);
        }
        return result;
    }

    public CPFloat[] parseCPFloatReferences(String name, InputStream in,
            BHSDCodec codec, int count) throws IOException, Pack200Exception {
        int[] indices = decodeBandInt(name, in, codec, count);
        CPFloat[] result = new CPFloat[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = segment.getCpBands().cpFloatValue(index);
        }
        return result;
    }

    public CPLong[] parseCPLongReferences(String name, InputStream in,
            BHSDCodec codec, int count) throws IOException, Pack200Exception {
        long[] reference = segment.getCpBands().getCpLong();
        int[] indices = decodeBandInt(name, in, codec, count);
        CPLong[] result = new CPLong[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            if (index < 0 || index >= reference.length)
                throw new Pack200Exception(
                        "Something has gone wrong during parsing references, index = "
                                + index + ", array size = " + reference.length);
            result[i1] = segment.getCpBands().cpLongValue(index);
        }
        return result;
    }

    public CPUTF8[] parseCPUTF8References(String name, InputStream in,
            BHSDCodec codec, int count) throws IOException, Pack200Exception {
        int[] indices = decodeBandInt(name, in, codec, count);
        CPUTF8[] result = new CPUTF8[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = segment.getCpBands().cpUTF8Value(index);
        }
        return result;
    }

    public CPUTF8[][] parseCPUTF8References(String name, InputStream in,
            BHSDCodec codec, int[] counts) throws IOException, Pack200Exception {
        CPUTF8[][] result = new CPUTF8[counts.length][];
        int sum = 0;
        for (int i = 0; i < counts.length; i++) {
            result[i] = new CPUTF8[counts[i]];
            sum += counts[i];
        }
        CPUTF8[] result1 = new CPUTF8[sum];
        int[] indices = decodeBandInt(name, in, codec, sum);
        for (int i1 = 0; i1 < sum; i1++) {
            int index = indices[i1];
            result1[i1] = segment.getCpBands().cpUTF8Value(index);
        }
        CPUTF8[] refs = result1;
        int pos = 0;
        for (int i = 0; i < counts.length; i++) {
            int num = counts[i];
            result[i] = new CPUTF8[num];
            System.arraycopy(refs, pos, result[i], 0, num);
            pos += num;
        }
        return result;
    }

    public CPString[] parseCPStringReferences(String name, InputStream in,
            BHSDCodec codec, int count) throws IOException, Pack200Exception {
        int[] indices = decodeBandInt(name, in, codec, count);
        CPString[] result = new CPString[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = segment.getCpBands().cpStringValue(index);
        }
        return result;
    }

    public CPInterfaceMethodRef[] parseCPInterfaceMethodRefReferences(
            String name, InputStream in, BHSDCodec codec, int count)
            throws IOException, Pack200Exception {
        CpBands cpBands = segment.getCpBands();
        int[] indices = decodeBandInt(name, in, codec, count);
        CPInterfaceMethodRef[] result = new CPInterfaceMethodRef[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = cpBands.cpIMethodValue(index);
        }
        return result;
    }

    public CPMethodRef[] parseCPMethodRefReferences(String name,
            InputStream in, BHSDCodec codec, int count) throws IOException,
            Pack200Exception {
        CpBands cpBands = segment.getCpBands();
        int[] indices = decodeBandInt(name, in, codec, count);
        CPMethodRef[] result = new CPMethodRef[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = cpBands.cpMethodValue(index);
        }
        return result;
    }

    public CPFieldRef[] parseCPFieldRefReferences(String name, InputStream in,
            BHSDCodec codec, int count) throws IOException, Pack200Exception {
        CpBands cpBands = segment.getCpBands();
        int[] indices = decodeBandInt(name, in, codec, count);
        CPFieldRef[] result = new CPFieldRef[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = cpBands.cpFieldValue(index);
        }
        return result;
    }

    public CPNameAndType[] parseCPDescriptorReferences(String name,
            InputStream in, BHSDCodec codec, int count) throws IOException,
            Pack200Exception {
        CpBands cpBands = segment.getCpBands();
        int[] indices = decodeBandInt(name, in, codec, count);
        CPNameAndType[] result = new CPNameAndType[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = cpBands.cpNameAndTypeValue(index);
        }
        return result;
    }

    public CPUTF8[] parseCPSignatureReferences(String name, InputStream in,
            BHSDCodec codec, int count) throws IOException, Pack200Exception {
        int[] indices = decodeBandInt(name, in, codec, count);
        CPUTF8[] result = new CPUTF8[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = segment.getCpBands().cpSignatureValue(index);
        }
        return result;
    }

    protected CPUTF8[][] parseCPSignatureReferences(String name, InputStream in,
            BHSDCodec codec, int[] counts) throws IOException, Pack200Exception {
        CPUTF8[][] result = new CPUTF8[counts.length][];
        int sum = 0;
        for (int i = 0; i < counts.length; i++) {
            result[i] = new CPUTF8[counts[i]];
            sum += counts[i];
        }
        CPUTF8[] result1 = new CPUTF8[sum];
        int[] indices = decodeBandInt(name, in, codec, sum);
        for (int i1 = 0; i1 < sum; i1++) {
            int index = indices[i1];
            result1[i1] = segment.getCpBands().cpSignatureValue(index);
        }
        CPUTF8[] refs = result1;
        int pos = 0;
        for (int i = 0; i < counts.length; i++) {
            int num = counts[i];
            result[i] = new CPUTF8[num];
            System.arraycopy(refs, pos, result[i], 0, num);
            pos += num;
        }
        return result;
    }

    public CPClass[] parseCPClassReferences(String name, InputStream in,
            BHSDCodec codec, int count) throws IOException, Pack200Exception {
        int[] indices = decodeBandInt(name, in, codec, count);
        CPClass[] result = new CPClass[indices.length];
        for (int i1 = 0; i1 < count; i1++) {
            int index = indices[i1];
            result[i1] = segment.getCpBands().cpClassValue(index);
        }
        return result;
    }

    protected String[] getReferences(int[] ints, String[] reference) {
        String[] result = new String[ints.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = reference[ints[i]];
        }
        return result;
    }

    protected String[][] getReferences(int[][] ints, String[] reference) {
        String[][] result = new String[ints.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = new String[ints[i].length];
            for (int j = 0; j < result[i].length; j++) {
                result[i][j] = reference[ints[i][j]];

            }
        }
        return result;
    }

}

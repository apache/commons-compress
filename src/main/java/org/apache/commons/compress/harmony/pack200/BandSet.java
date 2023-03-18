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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Abstract superclass for a set of bands
 */
public abstract class BandSet {

    /**
     * Results obtained by trying different Codecs to encode a band
     */
    public class BandAnalysisResults {

        // The number of Codecs tried so far
        private int numCodecsTried = 0;

        // The number of bytes saved by using betterCodec instead of the default codec
        private int saved = 0;

        // Extra metadata to pass to the segment header (to be appended to the
        // band_headers band)
        private int[] extraMetadata;

        // The results of encoding the band with betterCodec
        private byte[] encodedBand;

        // The best Codec found so far, or should be null if the default is the
        // best so far
        private Codec betterCodec;

    }
    /**
     * BandData represents information about a band, e.g. largest value etc and is used in the heuristics that calculate
     * whether an alternative Codec could make the encoded band smaller.
     */
    public class BandData {

        private final int[] band;
        private int smallest = Integer.MAX_VALUE;
        private int largest = Integer.MIN_VALUE;
        private int smallestDelta;
        private int largestDelta;

        private int deltaIsAscending = 0;
        private int smallDeltaCount = 0;

        private double averageAbsoluteDelta = 0;
        private double averageAbsoluteValue = 0;

        private Map<Integer, Integer> distinctValues;

        /**
         * Create a new instance of BandData. The band is then analysed.
         *
         * @param band - the band of integers
         */
        public BandData(final int[] band) {
            this.band = band;
            final Integer one = Integer.valueOf(1);
            for (int i = 0; i < band.length; i++) {
                if (band[i] < smallest) {
                    smallest = band[i];
                }
                if (band[i] > largest) {
                    largest = band[i];
                }
                if (i != 0) {
                    final int delta = band[i] - band[i - 1];
                    if (delta < smallestDelta) {
                        smallestDelta = delta;
                    }
                    if (delta > largestDelta) {
                        largestDelta = delta;
                    }
                    if (delta >= 0) {
                        deltaIsAscending++;
                    }
                    averageAbsoluteDelta += (double) Math.abs(delta) / (double) (band.length - 1);
                    if (Math.abs(delta) < 256) {
                        smallDeltaCount++;
                    }
                } else {
                    smallestDelta = band[0];
                    largestDelta = band[0];
                }
                averageAbsoluteValue += (double) Math.abs(band[i]) / (double) band.length;
                if (effort > 3) { // do calculations needed to consider population codec
                    if (distinctValues == null) {
                        distinctValues = new HashMap<>();
                    }
                    final Integer value = Integer.valueOf(band[i]);
                    Integer count = distinctValues.get(value);
                    if (count == null) {
                        count = one;
                    } else {
                        count = Integer.valueOf(count.intValue() + 1);
                    }
                    distinctValues.put(value, count);
                }
            }
        }

        /**
         * Returns true if any band elements are negative.
         *
         * @return true if any band elements are negative.
         */
        public boolean anyNegatives() {
            return smallest < 0;
        }

        /**
         * Returns true if the band deltas are mainly positive (heuristic).
         *
         * @return true if the band deltas are mainly positive (heuristic).
         */
        public boolean mainlyPositiveDeltas() {
            // Note: the value below has been tuned - please test carefully if changing it
            return (float) deltaIsAscending / (float) band.length > 0.95F;
        }

        /**
         * Returns true if the deltas between adjacent band elements are mainly small (heuristic).
         *
         * @return true if the deltas between adjacent band elements are mainly small (heuristic).
         */
        public boolean mainlySmallDeltas() {
            // Note: the value below has been tuned - please test carefully if changing it
            return (float) smallDeltaCount / (float) band.length > 0.7F;
        }

        /**
         * Returns the total number of distinct values found in the band.
         *
         * @return the total number of distinct values found in the band.
         */
        public int numDistinctValues() {
            if (distinctValues == null) {
                return band.length;
            }
            return distinctValues.size();
        }

        /**
         * Returns true if the band is well correlated (i.e. would be suitable for a delta encoding) (heuristic).
         *
         * @return true if the band is well correlated (i.e. would be suitable for a delta encoding) (heuristic).
         */
        public boolean wellCorrelated() {
            // Note: the value below has been tuned - please test carefully if changing it
            return averageAbsoluteDelta * 3.1 < averageAbsoluteValue;
        }

    }

    // Minimum size of band for each effort level where we consider alternative codecs
    // Note: these values have been tuned - please test carefully if changing them
    private static final int[] effortThresholds = {0, 0, 1000, 500, 100, 100, 100, 100, 100, 0};

    protected final SegmentHeader segmentHeader;
    final int effort;

    private long[] canonicalLargest;

    private long[] canonicalSmallest;

    /**
     * Create a new BandSet
     *
     * @param effort - the packing effort to be used (must be 1-9)
     * @param header - the segment header
     */
    public BandSet(final int effort, final SegmentHeader header) {
        this.effort = effort;
        this.segmentHeader = header;
    }

    private BandAnalysisResults analyseBand(final String name, final int[] band, final BHSDCodec defaultCodec)
        throws Pack200Exception {

        final BandAnalysisResults results = new BandAnalysisResults();

        if (canonicalLargest == null) {
            canonicalLargest = new long[116];
            canonicalSmallest = new long[116];
            for (int i = 1; i < canonicalLargest.length; i++) {
                canonicalLargest[i] = CodecEncoding.getCanonicalCodec(i).largest();
                canonicalSmallest[i] = CodecEncoding.getCanonicalCodec(i).smallest();
            }
        }
        final BandData bandData = new BandData(band);

        // Check that there is a reasonable saving to be made
        final byte[] encoded = defaultCodec.encode(band);
        results.encodedBand = encoded;

        // Note: these values have been tuned - please test carefully if changing them
        if (encoded.length <= band.length + 23 - 2 * effort) { // TODO: tweak
            return results;
        }

        // Check if we can use BYTE1 as that's a 1:1 mapping if we can
        if (!bandData.anyNegatives() && bandData.largest <= Codec.BYTE1.largest()) {
            results.encodedBand = Codec.BYTE1.encode(band);
            results.betterCodec = Codec.BYTE1;
            return results;
        }

        // Consider a population codec (but can't be nested)
        if (effort > 3 && !name.equals("POPULATION")) {
            final int numDistinctValues = bandData.numDistinctValues();
            final float distinctValuesAsProportion = (float) numDistinctValues / (float) band.length;

            // Note: these values have been tuned - please test carefully if changing them
            if (numDistinctValues < 100 || distinctValuesAsProportion < 0.02
                || (effort > 6 && distinctValuesAsProportion < 0.04)) { // TODO: tweak
                encodeWithPopulationCodec(name, band, defaultCodec, bandData, results);
                if (timeToStop(results)) {
                    return results;
                }
            }
        }

        final List<BHSDCodec[]> codecFamiliesToTry = new ArrayList<>();

        // See if the deltas are mainly small increments
        if (bandData.mainlyPositiveDeltas() && bandData.mainlySmallDeltas()) {
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaUnsignedCodecs2);
        }

        if (bandData.wellCorrelated()) { // Try delta encodings
            if (bandData.mainlyPositiveDeltas()) {
                codecFamiliesToTry.add(CanonicalCodecFamilies.deltaUnsignedCodecs1);
                codecFamiliesToTry.add(CanonicalCodecFamilies.deltaUnsignedCodecs3);
                codecFamiliesToTry.add(CanonicalCodecFamilies.deltaUnsignedCodecs4);
                codecFamiliesToTry.add(CanonicalCodecFamilies.deltaUnsignedCodecs5);
                codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs1);
                codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs3);
                codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs4);
                codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs5);
                codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs2);
            } else {
                codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs1);
                codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs3);
                codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs2);
                codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs4);
                codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs5);
                codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaSignedCodecs1);
                codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaSignedCodecs2);
            }
        } else if (bandData.anyNegatives()) {
            codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaSignedCodecs1);
            codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaSignedCodecs2);
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs1);
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs2);
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs3);
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs4);
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaSignedCodecs5);
        } else {
            codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs1);
            codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs3);
            codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs4);
            codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs5);
            codecFamiliesToTry.add(CanonicalCodecFamilies.nonDeltaUnsignedCodecs2);
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaUnsignedCodecs1);
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaUnsignedCodecs3);
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaUnsignedCodecs4);
            codecFamiliesToTry.add(CanonicalCodecFamilies.deltaUnsignedCodecs5);
        }
        if (name.equalsIgnoreCase("cpint")) {
            System.out.print("");
        }

        for (final BHSDCodec[] family : codecFamiliesToTry) {
            tryCodecs(name, band, defaultCodec, bandData, results, encoded, family);
            if (timeToStop(results)) {
                break;
            }
        }

        return results;
    }

    /**
     * Converts a list of ConstantPoolEntrys to an int[] array of their indices
     *
     * @param list conversion source.
     * @return conversion result.
     */
    protected int[] cpEntryListToArray(final List<? extends ConstantPoolEntry> list) {
        final int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i).getIndex();
            if (array[i] < 0) {
                throw new IllegalArgumentException("Index should be > 0");
            }
        }
        return array;
    }

    /**
     * Converts a list of ConstantPoolEntrys or nulls to an int[] array of their indices +1 (or 0 for nulls)
     *
     * @param list conversion source.
     * @return conversion result.
     */
    protected int[] cpEntryOrNullListToArray(final List<? extends ConstantPoolEntry> list) {
        final int[] array = new int[list.size()];
        for (int j = 0; j < array.length; j++) {
            final ConstantPoolEntry cpEntry = list.get(j);
            array[j] = cpEntry == null ? 0 : cpEntry.getIndex() + 1;
            if (cpEntry != null && cpEntry.getIndex() < 0) {
                throw new IllegalArgumentException("Index should be > 0");
            }
        }
        return array;
    }

    /**
     * Encode a band of integers. The default codec may be used, but other Codecs are considered if effort is greater
     * than 1.
     *
     * @param name - name of the band (used for debugging)
     * @param ints - the band
     * @param defaultCodec - the default Codec
     * @return the encoded band
     * @throws Pack200Exception TODO
     */
    public byte[] encodeBandInt(final String name, final int[] ints, final BHSDCodec defaultCodec)
        throws Pack200Exception {
        byte[] encodedBand = null;
        // Useful for debugging
//        if (ints.length > 0) {
//            System.out.println("encoding " + name + " " + ints.length);
//        }
        if (effort > 1 && (ints.length >= effortThresholds[effort])) {
            final BandAnalysisResults results = analyseBand(name, ints, defaultCodec);
            final Codec betterCodec = results.betterCodec;
            encodedBand = results.encodedBand;
            if (betterCodec != null) {
                if (betterCodec instanceof BHSDCodec) {
                    final int[] specifierBand = CodecEncoding.getSpecifier(betterCodec, defaultCodec);
                    int specifier = specifierBand[0];
                    if (specifierBand.length > 1) {
                        for (int i = 1; i < specifierBand.length; i++) {
                            segmentHeader.appendBandCodingSpecifier(specifierBand[i]);
                        }
                    }
                    if (defaultCodec.isSigned()) {
                        specifier = -1 - specifier;
                    } else {
                        specifier = specifier + defaultCodec.getL();
                    }
                    final byte[] specifierEncoded = defaultCodec.encode(new int[] {specifier});
                    final byte[] band = new byte[specifierEncoded.length + encodedBand.length];
                    System.arraycopy(specifierEncoded, 0, band, 0, specifierEncoded.length);
                    System.arraycopy(encodedBand, 0, band, specifierEncoded.length, encodedBand.length);
                    return band;
                }
                if (betterCodec instanceof PopulationCodec) {
                    IntStream.of(results.extraMetadata).forEach(segmentHeader::appendBandCodingSpecifier);
                    return encodedBand;
                }
                if (betterCodec instanceof RunCodec) {

                }
            }
        }

        // If we get here then we've decided to use the default codec.
        if (ints.length > 0) {
            if (encodedBand == null) {
                encodedBand = defaultCodec.encode(ints);
            }
            final int first = ints[0];
            if (defaultCodec.getB() != 1) {
                if (defaultCodec.isSigned() && first >= -256 && first <= -1) {
                    final int specifier = -1 - CodecEncoding.getSpecifierForDefaultCodec(defaultCodec);
                    final byte[] specifierEncoded = defaultCodec.encode(new int[] {specifier});
                    final byte[] band = new byte[specifierEncoded.length + encodedBand.length];
                    System.arraycopy(specifierEncoded, 0, band, 0, specifierEncoded.length);
                    System.arraycopy(encodedBand, 0, band, specifierEncoded.length, encodedBand.length);
                    return band;
                }
                if (!defaultCodec.isSigned() && first >= defaultCodec.getL() && first <= defaultCodec.getL() + 255) {
                    final int specifier = CodecEncoding.getSpecifierForDefaultCodec(defaultCodec) + defaultCodec.getL();
                    final byte[] specifierEncoded = defaultCodec.encode(new int[] {specifier});
                    final byte[] band = new byte[specifierEncoded.length + encodedBand.length];
                    System.arraycopy(specifierEncoded, 0, band, 0, specifierEncoded.length);
                    System.arraycopy(encodedBand, 0, band, specifierEncoded.length, encodedBand.length);
                    return band;
                }
            }
            return encodedBand;
        }
        return new byte[0];
    }

    /**
     * Encode a band of longs (values are split into their high and low 32 bits and then encoded as two separate bands
     *
     * @param name - name of the band (for debugging purposes)
     * @param flags - the band
     * @param loCodec - Codec for the low 32-bits band
     * @param hiCodec - Codec for the high 32-bits band
     * @param haveHiFlags - ignores the high band if true as all values would be zero
     * @return the encoded band
     * @throws Pack200Exception TODO
     */
    protected byte[] encodeFlags(final String name, final long[] flags, final BHSDCodec loCodec,
        final BHSDCodec hiCodec, final boolean haveHiFlags) throws Pack200Exception {
        if (!haveHiFlags) {
            final int[] loBits = new int[flags.length];
            Arrays.setAll(loBits, i -> (int) flags[i]);
            return encodeBandInt(name, loBits, loCodec);
        }
        final int[] hiBits = new int[flags.length];
        final int[] loBits = new int[flags.length];
        for (int i = 0; i < flags.length; i++) {
            final long l = flags[i];
            hiBits[i] = (int) (l >> 32);
            loBits[i] = (int) l;
        }
        final byte[] hi = encodeBandInt(name, hiBits, hiCodec);
        final byte[] lo = encodeBandInt(name, loBits, loCodec);
        final byte[] total = new byte[hi.length + lo.length];
        System.arraycopy(hi, 0, total, 0, hi.length);
        System.arraycopy(lo, 0, total, hi.length + 1, lo.length);
        return total;
    }

// This could be useful if further enhancements are done but is not currently used
//
//    private void encodeWithRunCodec(String name, int[] band, int index,
//            BHSDCodec defaultCodec, BandData bandData,
//            BandAnalysisResults results) throws Pack200Exception {
//        int[] firstBand = new int[index];
//        int[] secondBand = new int[band.length - index];
//        System.arraycopy(band, 0, firstBand, 0, index);
//        System.arraycopy(band, index, secondBand, 0, secondBand.length);
//        BandAnalysisResults firstResults = analyseBand(name + "A", firstBand, defaultCodec);
//        BandAnalysisResults secondResults = analyseBand(name + "B", secondBand, defaultCodec);
//        int specifier = 117;
//        byte[] specifierEncoded = defaultCodec.encode(new int[] {specifier});
//        int totalLength = firstResults.encodedBand.length + secondResults.encodedBand.length + specifierEncoded.length + 4; // TODO actual
//        if (totalLength < results.encodedBand.length) {
//            System.out.println("using run codec");
//            results.saved += results.encodedBand.length - totalLength;
//            byte[] encodedBand = new byte[specifierEncoded.length + firstResults.encodedBand.length + secondResults.encodedBand.length];
//            System.arraycopy(specifierEncoded, 0, encodedBand, 0, specifierEncoded.length);
//            System.arraycopy(firstResults.encodedBand, 0, encodedBand, specifierEncoded.length, firstResults.encodedBand.length);
//            System.arraycopy(secondResults.encodedBand, 0, encodedBand, specifierEncoded.length + firstResults.encodedBand.length, secondResults.encodedBand.length);
//            results.encodedBand = encodedBand;
//            results.betterCodec = new RunCodec(index, firstResults.betterCodec, secondResults.betterCodec);
//        }
//    }

    protected byte[] encodeFlags(final String name, final long[][] flags, final BHSDCodec loCodec,
        final BHSDCodec hiCodec, final boolean haveHiFlags) throws Pack200Exception {
        return encodeFlags(name, flatten(flags), loCodec, hiCodec, haveHiFlags);
    }

    /**
     * Encode a single value with the given Codec
     *
     * @param value - the value to encode
     * @param codec - Codec to use
     * @return the encoded value
     * @throws Pack200Exception TODO
     */
    public byte[] encodeScalar(final int value, final BHSDCodec codec) throws Pack200Exception {
        return codec.encode(value);
    }

    /**
     * Encode a band without considering other Codecs
     *
     * @param band - the band
     * @param codec - the Codec to use
     * @return the encoded band
     * @throws Pack200Exception TODO
     */
    public byte[] encodeScalar(final int[] band, final BHSDCodec codec) throws Pack200Exception {
        return codec.encode(band);
    }

    private void encodeWithPopulationCodec(final String name, final int[] band, final BHSDCodec defaultCodec,
        final BandData bandData, final BandAnalysisResults results) throws Pack200Exception {
        results.numCodecsTried += 3; // quite a bit more effort to try this codec
        final Map<Integer, Integer> distinctValues = bandData.distinctValues;

        final List<Integer> favored = new ArrayList<>();
        distinctValues.forEach((k, v) -> {
            if (v.intValue() > 2 || distinctValues.size() < 256) { // TODO: tweak
                favored.add(k);
            }
        });

        // Sort the favored list with the most commonly occurring first
        if (distinctValues.size() > 255) {
            favored.sort((arg0, arg1) -> distinctValues.get(arg1).compareTo(distinctValues.get(arg0)));
        }

        final Map<Integer, Integer> favoredToIndex = new HashMap<>();
        for (int i = 0; i < favored.size(); i++) {
            favoredToIndex.put(favored.get(i), Integer.valueOf(i));
        }

        final IntList unfavoured = new IntList();
        final int[] tokens = new int[band.length];
        for (int i = 0; i < band.length; i++) {
            final Integer favouredIndex = favoredToIndex.get(Integer.valueOf(band[i]));
            if (favouredIndex == null) {
                tokens[i] = 0;
                unfavoured.add(band[i]);
            } else {
                tokens[i] = favouredIndex.intValue() + 1;
            }
        }
        favored.add(favored.get(favored.size() - 1)); // repeat last value
        final int[] favouredBand = integerListToArray(favored);
        final int[] unfavouredBand = unfavoured.toArray();

        // Analyse the three bands to get the best codec
        final BandAnalysisResults favouredResults = analyseBand("POPULATION", favouredBand, defaultCodec);
        final BandAnalysisResults unfavouredResults = analyseBand("POPULATION", unfavouredBand, defaultCodec);

        int tdefL = 0;
        int l = 0;
        Codec tokenCodec = null;
        byte[] tokensEncoded;
        final int k = favored.size() - 1;
        if (k < 256) {
            tdefL = 1;
            tokensEncoded = Codec.BYTE1.encode(tokens);
        } else {
            final BandAnalysisResults tokenResults = analyseBand("POPULATION", tokens, defaultCodec);
            tokenCodec = tokenResults.betterCodec;
            tokensEncoded = tokenResults.encodedBand;
            if (tokenCodec == null) {
                tokenCodec = defaultCodec;
            }
            l = ((BHSDCodec) tokenCodec).getL();
            final int h = ((BHSDCodec) tokenCodec).getH();
            final int s = ((BHSDCodec) tokenCodec).getS();
            final int b = ((BHSDCodec) tokenCodec).getB();
            final int d = ((BHSDCodec) tokenCodec).isDelta() ? 1 : 0;
            if (s == 0 && d == 0) {
                boolean canUseTDefL = true;
                if (b > 1) {
                    final BHSDCodec oneLowerB = new BHSDCodec(b - 1, h);
                    if (oneLowerB.largest() >= k) {
                        canUseTDefL = false;
                    }
                }
                if (canUseTDefL) {
                    switch (l) {
                    case 4:
                        tdefL = 1;
                        break;
                    case 8:
                        tdefL = 2;
                        break;
                    case 16:
                        tdefL = 3;
                        break;
                    case 32:
                        tdefL = 4;
                        break;
                    case 64:
                        tdefL = 5;
                        break;
                    case 128:
                        tdefL = 6;
                        break;
                    case 192:
                        tdefL = 7;
                        break;
                    case 224:
                        tdefL = 8;
                        break;
                    case 240:
                        tdefL = 9;
                        break;
                    case 248:
                        tdefL = 10;
                        break;
                    case 252:
                        tdefL = 11;
                        break;
                    }
                }
            }
        }

        final byte[] favouredEncoded = favouredResults.encodedBand;
        final byte[] unfavouredEncoded = unfavouredResults.encodedBand;
        final Codec favouredCodec = favouredResults.betterCodec;
        final Codec unfavouredCodec = unfavouredResults.betterCodec;

        int specifier = 141 + (favouredCodec == null ? 1 : 0) + (4 * tdefL) + (unfavouredCodec == null ? 2 : 0);
        final IntList extraBandMetadata = new IntList(3);
        if (favouredCodec != null) {
            IntStream.of(CodecEncoding.getSpecifier(favouredCodec, null)).forEach(extraBandMetadata::add);
        }
        if (tdefL == 0) {
            IntStream.of(CodecEncoding.getSpecifier(tokenCodec, null)).forEach(extraBandMetadata::add);
        }
        if (unfavouredCodec != null) {
            IntStream.of(CodecEncoding.getSpecifier(unfavouredCodec, null)).forEach(extraBandMetadata::add);
        }
        final int[] extraMetadata = extraBandMetadata.toArray();
        final byte[] extraMetadataEncoded = Codec.UNSIGNED5.encode(extraMetadata);
        if (defaultCodec.isSigned()) {
            specifier = -1 - specifier;
        } else {
            specifier = specifier + defaultCodec.getL();
        }
        final byte[] firstValueEncoded = defaultCodec.encode(new int[] {specifier});
        final int totalBandLength = firstValueEncoded.length + favouredEncoded.length + tokensEncoded.length
            + unfavouredEncoded.length;

        if (totalBandLength + extraMetadataEncoded.length < results.encodedBand.length) {
            results.saved += results.encodedBand.length - (totalBandLength + extraMetadataEncoded.length);
            final byte[] encodedBand = new byte[totalBandLength];
            System.arraycopy(firstValueEncoded, 0, encodedBand, 0, firstValueEncoded.length);
            System.arraycopy(favouredEncoded, 0, encodedBand, firstValueEncoded.length, favouredEncoded.length);
            System.arraycopy(tokensEncoded, 0, encodedBand, firstValueEncoded.length + favouredEncoded.length,
                tokensEncoded.length);
            System.arraycopy(unfavouredEncoded, 0, encodedBand,
                firstValueEncoded.length + favouredEncoded.length + tokensEncoded.length, unfavouredEncoded.length);
            results.encodedBand = encodedBand;
            results.extraMetadata = extraMetadata;
            if (l != 0) {
                results.betterCodec = new PopulationCodec(favouredCodec, l, unfavouredCodec);
            } else {
                results.betterCodec = new PopulationCodec(favouredCodec, tokenCodec, unfavouredCodec);
            }
        }
    }

    /*
     * Flatten a 2-dimension array into a 1-dimension array
     */
    private long[] flatten(final long[][] flags) {
        int totalSize = 0;
        for (final long[] flag : flags) {
            totalSize += flag.length;
        }
        final long[] flatArray = new long[totalSize];
        int index = 0;
        for (final long[] flag : flags) {
            for (final long element : flag) {
                flatArray[index] = element;
                index++;
            }
        }
        return flatArray;
    }

    /**
     * Converts a list of Integers to an int[] array.
     *
     * @param integerList conversion source.
     * @return conversion result.
     */
    protected int[] integerListToArray(final List<Integer> integerList) {
        return integerList.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Converts a list of Longs to an long[] array.
     *
     * @param longList conversion source.
     * @return conversion result.
     */
    protected long[] longListToArray(final List<Long> longList) {
        return longList.stream().mapToLong(Long::longValue).toArray();
    }

    /**
     * Write the packed set of bands to the given output stream
     *
     * @param out TODO
     * @throws IOException If an I/O error occurs.
     * @throws Pack200Exception TODO
     */
    public abstract void pack(OutputStream out) throws IOException, Pack200Exception;

    private boolean timeToStop(final BandAnalysisResults results) {
        // if tried more than effort number of codecs for this band then return
        // Note: these values have been tuned - please test carefully if changing them
        if (effort > 6) {
            return results.numCodecsTried >= effort * 2;
        }
        return results.numCodecsTried >= effort;
        // May want to also check how much we've saved if performance needs improving, e.g. saved more than effort*2 %
        // || (float)results.saved/(float)results.encodedBand.length > (float)effort * 2/100;
    }

    private void tryCodecs(final String name, final int[] band, final BHSDCodec defaultCodec, final BandData bandData,
        final BandAnalysisResults results, final byte[] encoded, final BHSDCodec[] potentialCodecs)
        throws Pack200Exception {
        for (final BHSDCodec potential : potentialCodecs) {
            if (potential.equals(defaultCodec)) {
                return; // don't try codecs with greater cardinality in the same 'family' as the default codec as there
                        // won't be any savings
            }
            if (potential.isDelta()) {
                if (potential.largest() >= bandData.largestDelta && potential.smallest() <= bandData.smallestDelta
                    && potential.largest() >= bandData.largest && potential.smallest() <= bandData.smallest) {
                    // TODO: can represent some negative deltas with overflow
                    final byte[] encoded2 = potential.encode(band);
                    results.numCodecsTried++;
                    final byte[] specifierEncoded = defaultCodec.encode(CodecEncoding.getSpecifier(potential, null));
                    final int saved = encoded.length - encoded2.length - specifierEncoded.length;
                    if (saved > results.saved) {
                        results.betterCodec = potential;
                        results.encodedBand = encoded2;
                        results.saved = saved;
                    }
                }
            } else if (potential.largest() >= bandData.largest && potential.smallest() <= bandData.smallest) {
                final byte[] encoded2 = potential.encode(band);
                results.numCodecsTried++;
                final byte[] specifierEncoded = defaultCodec.encode(CodecEncoding.getSpecifier(potential, null));
                final int saved = encoded.length - encoded2.length - specifierEncoded.length;
                if (saved > results.saved) {
                    results.betterCodec = potential;
                    results.encodedBand = encoded2;
                    results.saved = saved;
                }
            }
            if (timeToStop(results)) {
                return;
            }
        }
    }

}

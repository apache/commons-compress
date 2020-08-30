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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract superclass for a set of bands
 */
public abstract class BandSet {

    protected final SegmentHeader segmentHeader;
    final int effort;

    // Minimum size of band for each effort level where we consider alternative codecs
    // Note: these values have been tuned - please test carefully if changing them
    private static final int[] effortThresholds = new int[] {0, 0, 1000, 500, 100, 100, 100, 100, 100, 0};

    private long[] canonicalLargest;
    private long[] canonicalSmallest;

    /**
     * Create a new BandSet
     * @param effort - the packing effort to be used (must be 1-9)
     * @param header - the segment header
     */
    public BandSet(int effort, SegmentHeader header) {
        this.effort = effort;
        this.segmentHeader = header;
    }

    /**
     * Write the packed set of bands to the given output stream
     * @param out
     * @throws IOException
     * @throws Pack200Exception
     */
    public abstract void pack(OutputStream out) throws IOException, Pack200Exception;

    /**
     * Encode a band without considering other Codecs
     * @param band - the band
     * @param codec - the Codec to use
     * @return the encoded band
     * @throws Pack200Exception
     */
    public byte[] encodeScalar(int[] band, BHSDCodec codec) throws Pack200Exception {
        return codec.encode(band);
    }

    /**
     * Encode a single value with the given Codec
     * @param value - the value to encode
     * @param codec - Codec to use
     * @return the encoded value
     * @throws Pack200Exception
     */
    public byte[] encodeScalar(int value, BHSDCodec codec) throws Pack200Exception {
        return codec.encode(value);
    }

    /**
     * Encode a band of integers. The default codec may be used, but other
     * Codecs are considered if effort is greater than 1.
     *
     * @param name
     *            - name of the band (used for debugging)
     * @param ints
     *            - the band
     * @param defaultCodec
     *            - the default Codec
     * @return the encoded band
     * @throws Pack200Exception
     */
    public byte[] encodeBandInt(String name, int[] ints, BHSDCodec defaultCodec) throws Pack200Exception {
        byte[] encodedBand = null;
     // Useful for debugging
//        if(ints.length > 0) {
//            System.out.println("encoding " + name + " " + ints.length);
//        }
        if(effort > 1 && (ints.length >= effortThresholds[effort])) {
            BandAnalysisResults results = analyseBand(name, ints, defaultCodec);
            Codec betterCodec = results.betterCodec;
            encodedBand = results.encodedBand;
            if(betterCodec != null) {
                if(betterCodec instanceof BHSDCodec) {
                    int[] specifierBand = CodecEncoding.getSpecifier(betterCodec, defaultCodec);
                    int specifier = specifierBand[0];
                    if(specifierBand.length > 1) {
                        for (int i = 1; i < specifierBand.length; i++) {
                            segmentHeader.appendBandCodingSpecifier(specifierBand[i]);
                        }
                    }
                    if(defaultCodec.isSigned()) {
                        specifier = -1 -specifier;
                    } else {
                        specifier = specifier + defaultCodec.getL();
                    }
                    byte[] specifierEncoded = defaultCodec.encode(new int[] {specifier});
                    byte[] band = new byte[specifierEncoded.length + encodedBand.length];
                    System.arraycopy(specifierEncoded, 0, band, 0, specifierEncoded.length);
                    System.arraycopy(encodedBand, 0, band, specifierEncoded.length, encodedBand.length);
                    return band;
                } else if (betterCodec instanceof PopulationCodec) {
                    int[] extraSpecifierInfo = results.extraMetadata;
                    for (int i = 0; i < extraSpecifierInfo.length; i++) {
                        segmentHeader.appendBandCodingSpecifier(extraSpecifierInfo[i]);
                    }
                    return encodedBand;
                } else if (betterCodec instanceof RunCodec) {

                }
            }
        }

        // If we get here then we've decided to use the default codec.
        if(ints.length > 0) {
            if(encodedBand == null) {
                encodedBand = defaultCodec.encode(ints);
            }
            int first = ints[0];
            if(defaultCodec.getB() != 1) {
                if (defaultCodec.isSigned() && first >= -256 && first <= -1) {
                    int specifier = -1 - CodecEncoding.getSpecifierForDefaultCodec(defaultCodec);
                    byte[] specifierEncoded = defaultCodec.encode(new int[] {specifier});
                    byte[] band = new byte[specifierEncoded.length + encodedBand.length];
                    System.arraycopy(specifierEncoded, 0, band, 0, specifierEncoded.length);
                    System.arraycopy(encodedBand, 0, band, specifierEncoded.length, encodedBand.length);
                    return band;
                } else if (!defaultCodec.isSigned() && first >= defaultCodec.getL()
                        && first <= defaultCodec.getL() + 255) {
                    int specifier = CodecEncoding.getSpecifierForDefaultCodec(defaultCodec) + defaultCodec.getL();
                    byte[] specifierEncoded = defaultCodec.encode(new int[] {specifier});
                    byte[] band = new byte[specifierEncoded.length + encodedBand.length];
                    System.arraycopy(specifierEncoded, 0, band, 0, specifierEncoded.length);
                    System.arraycopy(encodedBand, 0, band, specifierEncoded.length, encodedBand.length);
                    return band;
                }
            }
            return encodedBand;
        }
        return new byte[0];
    }

    private BandAnalysisResults analyseBand(String name, int[] band,
            BHSDCodec defaultCodec) throws Pack200Exception {

        BandAnalysisResults results = new BandAnalysisResults();

        if(canonicalLargest == null) {
            canonicalLargest = new long[116];
            canonicalSmallest = new long[116];
            for (int i = 1; i < canonicalLargest.length; i++) {
                canonicalLargest[i] = CodecEncoding.getCanonicalCodec(i).largest();
                canonicalSmallest[i] = CodecEncoding.getCanonicalCodec(i).smallest();
            }
        }
        BandData bandData = new BandData(band);

        // Check that there is a reasonable saving to be made
        byte[] encoded = defaultCodec.encode(band);
        results.encodedBand = encoded;

        // Note: these values have been tuned - please test carefully if changing them
        if(encoded.length <= band.length + 23 - 2*effort) { // TODO: tweak
            return results;
        }

        // Check if we can use BYTE1 as that's a 1:1 mapping if we can
        if(!bandData.anyNegatives() && bandData.largest <= Codec.BYTE1.largest()) {
              results.encodedBand = Codec.BYTE1.encode(band)  ;
              results.betterCodec = Codec.BYTE1;
              return results;
        }

        // Consider a population codec (but can't be nested)
        if(effort > 3 && !name.equals("POPULATION")) {
            int numDistinctValues = bandData.numDistinctValues();
            float distinctValuesAsProportion = (float)numDistinctValues / (float)band.length;

            // Note: these values have been tuned - please test carefully if changing them
            if(numDistinctValues < 100 || distinctValuesAsProportion < 0.02 ||  (effort > 6 && distinctValuesAsProportion < 0.04)) { // TODO: tweak
                encodeWithPopulationCodec(name, band, defaultCodec, bandData, results);
                if(timeToStop(results)) {
                    return results;
                }
            }
        }

        List codecFamiliesToTry = new ArrayList();

        // See if the deltas are mainly small increments
        if(bandData.mainlyPositiveDeltas() && bandData.mainlySmallDeltas()) {
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
        } else {
            if (bandData.anyNegatives()) {
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
        }
        if(name.equalsIgnoreCase("cpint")) {
            System.out.print("");
        }

        for (Iterator iterator = codecFamiliesToTry.iterator(); iterator
                .hasNext();) {
            BHSDCodec[] family = (BHSDCodec[]) iterator.next();
            tryCodecs(name, band, defaultCodec, bandData, results, encoded,
                    family);
            if (timeToStop(results)) {
                break;
            }
        }

        return results;
    }

    private boolean timeToStop(BandAnalysisResults results) {
        // if tried more than effort number of codecs for this band then return
        // Note: these values have been tuned - please test carefully if changing them
        if(effort > 6) {
            return results.numCodecsTried >= effort * 2;
        }
        return results.numCodecsTried >= effort;
        // May want to also check how much we've saved if performance needs improving, e.g. saved more than effort*2 %
        // || (float)results.saved/(float)results.encodedBand.length > (float)effort * 2/100;
    }

    private void tryCodecs(String name, int[] band, BHSDCodec defaultCodec, BandData bandData,
            BandAnalysisResults results, byte[] encoded,
            BHSDCodec[] potentialCodecs) throws Pack200Exception {
        for (int i = 0; i < potentialCodecs.length; i++) {
            BHSDCodec potential = potentialCodecs[i];
            if(potential.equals(defaultCodec)) {
                return; // don't try codecs with greater cardinality in the same 'family' as the default codec as there won't be any savings
            }
            if (potential.isDelta()) {
                if (potential.largest() >= bandData.largestDelta
                        && potential.smallest() <= bandData.smallestDelta
                        && potential.largest() >= bandData.largest
                        && potential.smallest() <= bandData.smallest) {
                    // TODO: can represent some negative deltas with overflow
                    byte[] encoded2 = potential.encode(band);
                    results.numCodecsTried++;
                    byte[] specifierEncoded = defaultCodec.encode(CodecEncoding
                            .getSpecifier(potential, null));
                    int saved = encoded.length - encoded2.length
                            - specifierEncoded.length;
                    if (saved > results.saved) {
                        results.betterCodec = potential;
                        results.encodedBand = encoded2;
                        results.saved = saved;
                    }
                }
            } else if (potential.largest() >= bandData.largest
                    && potential.smallest() <= bandData.smallest) {
                byte[] encoded2 = potential.encode(band);
                results.numCodecsTried++;
                byte[] specifierEncoded = defaultCodec.encode(CodecEncoding
                        .getSpecifier(potential, null));
                int saved = encoded.length - encoded2.length
                        - specifierEncoded.length;
                if (saved > results.saved) {
                    results.betterCodec = potential;
                    results.encodedBand = encoded2;
                    results.saved = saved;
                }
            }
            if(timeToStop(results)) {
                return;
            }
        }
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
//        if(totalLength < results.encodedBand.length) {
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

    private void encodeWithPopulationCodec(String name, int[] band,
            BHSDCodec defaultCodec, BandData bandData, BandAnalysisResults results) throws Pack200Exception {
        results.numCodecsTried += 3; // quite a bit more effort to try this codec
        final Map distinctValues = bandData.distinctValues;

        List favoured = new ArrayList();
        for (Iterator iterator = distinctValues.keySet().iterator(); iterator
                .hasNext();) {
            Integer value = (Integer) iterator.next();
            Integer count = (Integer) distinctValues.get(value);
            if(count.intValue() > 2 || distinctValues.size() < 256) { // TODO: tweak
                favoured.add(value);
            }
        }

        // Sort the favoured list with the most commonly occurring first
        if(distinctValues.size() > 255) {
            Collections.sort(favoured, new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    return ((Integer)distinctValues.get(arg1)).compareTo((Integer)distinctValues.get(arg0));
                }
            });
        }

        IntList unfavoured = new IntList();
        Map favouredToIndex = new HashMap();
        for (int i = 0; i < favoured.size(); i++) {
            Integer value = (Integer) favoured.get(i);
            favouredToIndex.put(value, new Integer(i));
        }

        int[] tokens = new int[band.length];
        for (int i = 0; i < band.length; i++) {
            Integer favouredIndex = (Integer)favouredToIndex.get(new Integer(band[i]));
            if(favouredIndex == null) {
                tokens[i] = 0;
                unfavoured.add(band[i]);
            } else {
                tokens[i] = favouredIndex.intValue() + 1;
            }
        }
        favoured.add(favoured.get(favoured.size() - 1)); // repeat last value
        int[] favouredBand = integerListToArray(favoured);
        int[] unfavouredBand = unfavoured.toArray();

        // Analyse the three bands to get the best codec
        BandAnalysisResults favouredResults = analyseBand("POPULATION", favouredBand, defaultCodec);
        BandAnalysisResults unfavouredResults = analyseBand("POPULATION", unfavouredBand, defaultCodec);

        int tdefL = 0;
        int l = 0;
        Codec tokenCodec = null;
        byte[] tokensEncoded;
        int k = favoured.size() - 1;
        if(k < 256) {
            tdefL = 1;
            tokensEncoded = Codec.BYTE1.encode(tokens);
        } else {
            BandAnalysisResults tokenResults = analyseBand("POPULATION", tokens, defaultCodec);
            tokenCodec = tokenResults.betterCodec;
            tokensEncoded = tokenResults.encodedBand;
            if(tokenCodec == null) {
                tokenCodec = defaultCodec;
            }
            l = ((BHSDCodec) tokenCodec).getL();
            int h = ((BHSDCodec) tokenCodec).getH();
            int s = ((BHSDCodec) tokenCodec).getS();
            int b = ((BHSDCodec) tokenCodec).getB();
            int d = ((BHSDCodec) tokenCodec).isDelta() ? 1 : 0;
            if(s == 0 && d == 0) {
                boolean canUseTDefL = true;
                if(b > 1) {
                    BHSDCodec oneLowerB = new BHSDCodec(b-1, h);
                    if(oneLowerB.largest() >= k) {
                        canUseTDefL = false;
                    }
                }
                if(canUseTDefL) {
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

        byte[] favouredEncoded = favouredResults.encodedBand;
        byte[] unfavouredEncoded = unfavouredResults.encodedBand;
        Codec favouredCodec = favouredResults.betterCodec;
        Codec unfavouredCodec = unfavouredResults.betterCodec;

        int specifier = 141 + (favouredCodec == null ? 1 : 0) + (4 * tdefL) + (unfavouredCodec == null ? 2 : 0);
        IntList extraBandMetadata = new IntList(3);
        if(favouredCodec != null) {
            int[] specifiers = CodecEncoding.getSpecifier(favouredCodec, null);
            for (int i = 0; i < specifiers.length; i++) {
                extraBandMetadata.add(specifiers[i]);
            }
        }
        if(tdefL == 0) {
            int[] specifiers = CodecEncoding.getSpecifier(tokenCodec, null);
            for (int i = 0; i < specifiers.length; i++) {
                extraBandMetadata.add(specifiers[i]);
            }
        }
        if(unfavouredCodec != null) {
            int[] specifiers = CodecEncoding.getSpecifier(unfavouredCodec, null);
            for (int i = 0; i < specifiers.length; i++) {
                extraBandMetadata.add(specifiers[i]);
            }
        }
        int[] extraMetadata = extraBandMetadata.toArray();
        byte[] extraMetadataEncoded = Codec.UNSIGNED5.encode(extraMetadata);
        if(defaultCodec.isSigned()) {
            specifier = -1 -specifier;
        } else {
            specifier = specifier + defaultCodec.getL();
        }
        byte[] firstValueEncoded = defaultCodec.encode(new int[] {specifier});
        int totalBandLength = firstValueEncoded.length + favouredEncoded.length + tokensEncoded.length + unfavouredEncoded.length;

        if(totalBandLength + extraMetadataEncoded.length < results.encodedBand.length) {
            results.saved += results.encodedBand.length - (totalBandLength + extraMetadataEncoded.length);
            byte[] encodedBand = new byte[totalBandLength];
            System.arraycopy(firstValueEncoded, 0, encodedBand, 0, firstValueEncoded.length);
            System.arraycopy(favouredEncoded, 0, encodedBand, firstValueEncoded.length, favouredEncoded.length);
            System.arraycopy(tokensEncoded, 0, encodedBand, firstValueEncoded.length + favouredEncoded.length, tokensEncoded.length);
            System.arraycopy(unfavouredEncoded, 0, encodedBand, firstValueEncoded.length + favouredEncoded.length + tokensEncoded.length, unfavouredEncoded.length);
            results.encodedBand = encodedBand;
            results.extraMetadata = extraMetadata;
            if(l != 0) {
                results.betterCodec = new PopulationCodec(favouredCodec, l, unfavouredCodec);
            } else {
                results.betterCodec = new PopulationCodec(favouredCodec, tokenCodec, unfavouredCodec);
            }
        }
    }

    /**
     * Encode a band of longs (values are split into their high and low 32 bits
     * and then encoded as two separate bands
     *
     * @param name
     *            - name of the band (for debugging purposes)
     * @param flags
     *            - the band
     * @param loCodec
     *            - Codec for the low 32-bits band
     * @param hiCodec
     *            - Codec for the high 32-bits band
     * @param haveHiFlags
     *            - ignores the high band if true as all values would be zero
     * @return the encoded band
     * @throws Pack200Exception
     */
    protected byte[] encodeFlags(String name, long[] flags, BHSDCodec loCodec, BHSDCodec hiCodec,
            boolean haveHiFlags) throws Pack200Exception {
        if(!haveHiFlags) {
            int[] loBits = new int[flags.length];
            for (int i = 0; i < flags.length; i++) {
                loBits[i] = (int) flags[i];
            }
            return encodeBandInt(name, loBits, loCodec);
        } else {

            int[] hiBits = new int[flags.length];
            int[] loBits = new int[flags.length];
            for (int i = 0; i < flags.length; i++) {
                long l = flags[i];
                hiBits[i] = (int) (l >> 32);
                loBits[i] = (int) l;
            }
            byte[] hi = encodeBandInt(name, hiBits, hiCodec);
            byte[] lo = encodeBandInt(name, loBits, loCodec);
            byte[] total = new byte[hi.length + lo.length];
            System.arraycopy(hi, 0, total, 0, hi.length);
            System.arraycopy(lo, 0, total, hi.length + 1, lo.length);
            return total;
        }
    }

    /**
     * Converts a list of Integers to an int[] array
     */
    protected int[] integerListToArray(List integerList) {
        int[] array = new int[integerList.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = ((Integer)integerList.get(i)).intValue();
        }
        return array;
    }

    /**
     * Converts a list of Longs to an long[] array
     */
    protected long[] longListToArray(List longList) {
        long[] array = new long[longList.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = ((Long)longList.get(i)).longValue();
        }
        return array;
    }

    /**
     * Converts a list of ConstantPoolEntrys to an int[] array of their indices
     */
    protected int[] cpEntryListToArray(List list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = ((ConstantPoolEntry)list.get(i)).getIndex();
            if(array[i] < 0) {
                throw new RuntimeException("Index should be > 0");
            }
        }
        return array;
    }

    /**
     * Converts a list of ConstantPoolEntrys or nulls to an int[] array of their
     * indices +1 (or 0 for nulls)
     */
    protected int[] cpEntryOrNullListToArray(List theList) {
        int[] array = new int[theList.size()];
        for (int j = 0; j < array.length; j++) {
            ConstantPoolEntry cpEntry = (ConstantPoolEntry) theList.get(j);
            array[j] = cpEntry == null ? 0 : cpEntry.getIndex() + 1;
            if(cpEntry != null && cpEntry.getIndex() < 0) {
                throw new RuntimeException("Index should be > 0");
            }
        }
        return array;
    }

    protected byte[] encodeFlags(String name, long[][] flags, BHSDCodec loCodec, BHSDCodec hiCodec,
            boolean haveHiFlags) throws Pack200Exception {
        return encodeFlags(name, flatten(flags), loCodec, hiCodec, haveHiFlags);
   }

    /*
     * Flatten a 2-dimension array into a 1-dimension array
     */
    private long[] flatten(long[][] flags) {
        int totalSize = 0;
        for (int i = 0; i < flags.length; i++) {
            totalSize += flags[i].length;
        }
        long[] flatArray = new long[totalSize];
        int index = 0;
        for (int i = 0; i < flags.length; i++) {
            for (int j = 0; j < flags[i].length; j++) {
                flatArray[index] = flags[i][j];
                index++;
            }
        }
        return flatArray;
    }

    /**
     * BandData represents information about a band, e.g. largest value etc
     * and is used in the heuristics that calculate whether an alternative
     * Codec could make the encoded band smaller.
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

        private Map distinctValues;

        /**
         * Create a new instance of BandData.  The band is then analysed.
         * @param band - the band of integers
         */
        public BandData(int[] band) {
            this.band = band;
            Integer one = new Integer(1);
            for (int i = 0; i < band.length; i++) {
                if(band[i] < smallest) {
                    smallest = band[i];
                }
                if(band[i] > largest) {
                    largest = band[i];
                }
                if(i != 0) {
                    int delta = band[i] - band[i - 1];
                    if(delta < smallestDelta) {
                        smallestDelta = delta;
                    }
                    if(delta > largestDelta) {
                        largestDelta = delta;
                    }
                    if(delta >= 0) {
                        deltaIsAscending++;
                    }
                    averageAbsoluteDelta += (double)Math.abs(delta)/(double)(band.length - 1);
                    if(Math.abs(delta) < 256) {
                        smallDeltaCount++;
                    }
                } else {
                    smallestDelta = band[0];
                    largestDelta = band[0];
                }
                averageAbsoluteValue += (double)Math.abs(band[i])/(double)band.length;
                if(effort > 3) { // do calculations needed to consider population codec
                    if(distinctValues == null) {
                        distinctValues = new HashMap();
                    }
                    Integer value = new Integer(band[i]);
                    Integer count = (Integer) distinctValues.get(value);
                    if(count == null) {
                        count = one;
                    } else {
                        count = new Integer(count.intValue() + 1);
                    }
                    distinctValues.put(value, count);
                }
            }
        }

        /**
         * Returns true if the deltas between adjacent band elements are mainly
         * small (heuristic)
         */
        public boolean mainlySmallDeltas() {
            // Note: the value below has been tuned - please test carefully if changing it
            return (float)smallDeltaCount/(float)band.length > 0.7F;
        }

        /**
         * Returns true if the band is well correlated (i.e. would be suitable
         * for a delta encoding) (heuristic)
         */
        public boolean wellCorrelated() {
            // Note: the value below has been tuned - please test carefully if changing it
            return averageAbsoluteDelta * 3.1 < averageAbsoluteValue;
        }

        /**
         * Returns true if the band deltas are mainly positive (heuristic)
         */
        public boolean mainlyPositiveDeltas() {
            // Note: the value below has been tuned - please test carefully if changing it
            return (float)deltaIsAscending/(float)band.length > 0.95F;
        }

        /**
         * Returns true if any band elements are negative
         */
        public boolean anyNegatives() {
            return smallest < 0;
        }

        /**
         * Returns the total number of distinct values found in the band
         */
        public int numDistinctValues() {
            if(distinctValues == null) {
                return band.length;
            }
            return distinctValues.size();
        }

    }

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

}

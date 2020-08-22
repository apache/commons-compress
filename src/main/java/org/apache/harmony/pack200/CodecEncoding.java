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
package org.apache.harmony.pack200;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * CodecEncoding is used to get the right Codec for a given meta-encoding
 */
public class CodecEncoding {

    /**
     * The canonical encodings are defined to allow a single byte to represent
     * one of the standard encodings. The following values are defined in the
     * Pack200 specification, and this array cannot be changed.
     */
    private static final BHSDCodec[] canonicalCodec = { null,
            new BHSDCodec(1, 256), new BHSDCodec(1, 256, 1),
            new BHSDCodec(1, 256, 0, 1), new BHSDCodec(1, 256, 1, 1),
            new BHSDCodec(2, 256), new BHSDCodec(2, 256, 1),
            new BHSDCodec(2, 256, 0, 1), new BHSDCodec(2, 256, 1, 1),
            new BHSDCodec(3, 256), new BHSDCodec(3, 256, 1),
            new BHSDCodec(3, 256, 0, 1), new BHSDCodec(3, 256, 1, 1),
            new BHSDCodec(4, 256), new BHSDCodec(4, 256, 1),
            new BHSDCodec(4, 256, 0, 1), new BHSDCodec(4, 256, 1, 1),
            new BHSDCodec(5, 4), new BHSDCodec(5, 4, 1),
            new BHSDCodec(5, 4, 2), new BHSDCodec(5, 16),
            new BHSDCodec(5, 16, 1), new BHSDCodec(5, 16, 2),
            new BHSDCodec(5, 32), new BHSDCodec(5, 32, 1),
            new BHSDCodec(5, 32, 2), new BHSDCodec(5, 64),
            new BHSDCodec(5, 64, 1), new BHSDCodec(5, 64, 2),
            new BHSDCodec(5, 128), new BHSDCodec(5, 128, 1),
            new BHSDCodec(5, 128, 2), new BHSDCodec(5, 4, 0, 1),
            new BHSDCodec(5, 4, 1, 1), new BHSDCodec(5, 4, 2, 1),
            new BHSDCodec(5, 16, 0, 1), new BHSDCodec(5, 16, 1, 1),
            new BHSDCodec(5, 16, 2, 1), new BHSDCodec(5, 32, 0, 1),
            new BHSDCodec(5, 32, 1, 1), new BHSDCodec(5, 32, 2, 1),
            new BHSDCodec(5, 64, 0, 1), new BHSDCodec(5, 64, 1, 1),
            new BHSDCodec(5, 64, 2, 1), new BHSDCodec(5, 128, 0, 1),
            new BHSDCodec(5, 128, 1, 1), new BHSDCodec(5, 128, 2, 1),
            new BHSDCodec(2, 192), new BHSDCodec(2, 224),
            new BHSDCodec(2, 240), new BHSDCodec(2, 248),
            new BHSDCodec(2, 252), new BHSDCodec(2, 8, 0, 1),
            new BHSDCodec(2, 8, 1, 1), new BHSDCodec(2, 16, 0, 1),
            new BHSDCodec(2, 16, 1, 1), new BHSDCodec(2, 32, 0, 1),
            new BHSDCodec(2, 32, 1, 1), new BHSDCodec(2, 64, 0, 1),
            new BHSDCodec(2, 64, 1, 1), new BHSDCodec(2, 128, 0, 1),
            new BHSDCodec(2, 128, 1, 1), new BHSDCodec(2, 192, 0, 1),
            new BHSDCodec(2, 192, 1, 1), new BHSDCodec(2, 224, 0, 1),
            new BHSDCodec(2, 224, 1, 1), new BHSDCodec(2, 240, 0, 1),
            new BHSDCodec(2, 240, 1, 1), new BHSDCodec(2, 248, 0, 1),
            new BHSDCodec(2, 248, 1, 1), new BHSDCodec(3, 192),
            new BHSDCodec(3, 224), new BHSDCodec(3, 240),
            new BHSDCodec(3, 248), new BHSDCodec(3, 252),
            new BHSDCodec(3, 8, 0, 1), new BHSDCodec(3, 8, 1, 1),
            new BHSDCodec(3, 16, 0, 1), new BHSDCodec(3, 16, 1, 1),
            new BHSDCodec(3, 32, 0, 1), new BHSDCodec(3, 32, 1, 1),
            new BHSDCodec(3, 64, 0, 1), new BHSDCodec(3, 64, 1, 1),
            new BHSDCodec(3, 128, 0, 1), new BHSDCodec(3, 128, 1, 1),
            new BHSDCodec(3, 192, 0, 1), new BHSDCodec(3, 192, 1, 1),
            new BHSDCodec(3, 224, 0, 1), new BHSDCodec(3, 224, 1, 1),
            new BHSDCodec(3, 240, 0, 1), new BHSDCodec(3, 240, 1, 1),
            new BHSDCodec(3, 248, 0, 1), new BHSDCodec(3, 248, 1, 1),
            new BHSDCodec(4, 192), new BHSDCodec(4, 224),
            new BHSDCodec(4, 240), new BHSDCodec(4, 248),
            new BHSDCodec(4, 252), new BHSDCodec(4, 8, 0, 1),
            new BHSDCodec(4, 8, 1, 1), new BHSDCodec(4, 16, 0, 1),
            new BHSDCodec(4, 16, 1, 1), new BHSDCodec(4, 32, 0, 1),
            new BHSDCodec(4, 32, 1, 1), new BHSDCodec(4, 64, 0, 1),
            new BHSDCodec(4, 64, 1, 1), new BHSDCodec(4, 128, 0, 1),
            new BHSDCodec(4, 128, 1, 1), new BHSDCodec(4, 192, 0, 1),
            new BHSDCodec(4, 192, 1, 1), new BHSDCodec(4, 224, 0, 1),
            new BHSDCodec(4, 224, 1, 1), new BHSDCodec(4, 240, 0, 1),
            new BHSDCodec(4, 240, 1, 1), new BHSDCodec(4, 248, 0, 1),
            new BHSDCodec(4, 248, 1, 1) };

    private static Map canonicalCodecsToSpecifiers;


    /**
     * Returns the codec specified by the given value byte and optional byte
     * header. If the value is >=116, then bytes may be consumed from the
     * secondary input stream, which is taken to be the contents of the
     * band_headers byte array. Since the values from this are consumed and not
     * repeated, the input stream should be reused for subsequent encodings.
     * This does not therefore close the input stream.
     *
     * @param value
     *            the canonical encoding value
     * @param in
     *            the input stream to read additional byte headers from
     * @param defaultCodec
     *            TODO
     * @return the corresponding codec, or <code>null</code> if the default
     *         should be used
     *
     * @throws IOException
     *             if there is a problem reading from the input stream (which in
     *             reality, is never, since the band_headers are likely stored
     *             in a byte array and accessed via a ByteArrayInputStream.
     *             However, an EOFException could occur if things go wrong)
     * @throws Pack200Exception
     */
    public static Codec getCodec(int value, InputStream in, Codec defaultCodec)
            throws IOException, Pack200Exception {
        // Sanity check to make sure that no-one has changed
        // the canonical codecs, which would really cause havoc
        if (canonicalCodec.length != 116) {
            throw new Error(
                    "Canonical encodings have been incorrectly modified");
        }
        if (value < 0) {
            throw new IllegalArgumentException(
                    "Encoding cannot be less than zero");
        } else if (value == 0) {
            return defaultCodec;
        } else if (value <= 115) {
            return canonicalCodec[value];
        } else if (value == 116) {
            int code = in.read();
            if (code == -1) {
                throw new EOFException(
                        "End of buffer read whilst trying to decode codec");
            }
            int d = (code & 0x01);
            int s = (code >> 1 & 0x03);
            int b = (code >> 3 & 0x07) + 1; // this might result in an invalid
            // number, but it's checked in the
            // Codec constructor
            code = in.read();
            if (code == -1) {
                throw new EOFException(
                        "End of buffer read whilst trying to decode codec");
            }
            int h = code + 1;
            // This handles the special cases for invalid combinations of data.
            return new BHSDCodec(b, h, s, d);
        } else if (value >= 117 && value <= 140) { // Run codec
            int offset = value - 117;
            int kx = offset & 3;
            boolean kbflag = (offset >> 2 & 1) == 1;
            boolean adef = (offset >> 3 & 1) == 1;
            boolean bdef = (offset >> 4 & 1) == 1;
            // If both A and B use the default encoding, what's the point of
            // having a run of default values followed by default values
            if (adef && bdef) {
                throw new Pack200Exception(
                        "ADef and BDef should never both be true");
            }
            int kb = (kbflag ? in.read() : 3);
            int k = (kb + 1) * (int) Math.pow(16, kx);
            Codec aCodec, bCodec;
            if (adef) {
                aCodec = defaultCodec;
            } else {
                aCodec = getCodec(in.read(), in, defaultCodec);
            }
            if (bdef) {
                bCodec = defaultCodec;
            } else {
                bCodec = getCodec(in.read(), in, defaultCodec);
            }
            return new RunCodec(k, aCodec, bCodec);
        } else if (value >= 141 && value <= 188) { // Population Codec
            int offset = value - 141;
            boolean fdef = (offset & 1) == 1;
            boolean udef = (offset >> 1 & 1) == 1;
            int tdefl = offset >> 2;
            boolean tdef = tdefl != 0;
            // From section 6.7.3 of spec
            final int[] tdefToL = { 0, 4, 8, 16, 32, 64, 128, 192, 224, 240,
                    248, 252 };
            int l = tdefToL[tdefl];
            // NOTE: Do not re-factor this to bring out uCodec; the order in
            // which
            // they are read from the stream is important
            if (tdef) {
                Codec fCodec = (fdef ? defaultCodec : getCodec(in.read(), in,
                        defaultCodec));
                Codec uCodec = (udef ? defaultCodec : getCodec(in.read(), in,
                        defaultCodec));
                // Unfortunately, if tdef, then tCodec depends both on l and
                // also on k, the
                // number of items read from the fCodec. So we don't know in
                // advance what
                // the codec will be.
                return new PopulationCodec(fCodec, l, uCodec);
            } else {
                Codec fCodec = (fdef ? defaultCodec : getCodec(in.read(), in,
                        defaultCodec));
                Codec tCodec = getCodec(in.read(), in, defaultCodec);
                Codec uCodec = (udef ? defaultCodec : getCodec(in.read(), in,
                        defaultCodec));
                return new PopulationCodec(fCodec, tCodec, uCodec);
            }
        } else {
            throw new Pack200Exception("Invalid codec encoding byte (" + value
                    + ") found");
        }
    }

    public static int getSpecifierForDefaultCodec(BHSDCodec defaultCodec) {
        return getSpecifier(defaultCodec, null)[0];
    }

    public static int[] getSpecifier(Codec codec, Codec defaultForBand) {
        // lazy initialization
        if(canonicalCodecsToSpecifiers == null) {
            HashMap reverseMap = new HashMap(canonicalCodec.length);
            for (int i = 0; i < canonicalCodec.length; i++) {
                reverseMap.put(canonicalCodec[i], new Integer(i));
            }
            canonicalCodecsToSpecifiers = reverseMap;
        }

        if(canonicalCodecsToSpecifiers.containsKey(codec)) {
            return new int[] {((Integer)canonicalCodecsToSpecifiers.get(codec)).intValue()};
        } else if (codec instanceof BHSDCodec) {
            // Cache these?
            BHSDCodec bhsdCodec = (BHSDCodec)codec;
            int[] specifiers = new int[3];
            specifiers[0] = 116;
            specifiers[1] = (bhsdCodec.isDelta() ? 1 : 0) + 2
                    * bhsdCodec.getS() + 8 * (bhsdCodec.getB()-1);
            specifiers[2] = bhsdCodec.getH() - 1;
            return specifiers;
        } else if (codec instanceof RunCodec) {
            RunCodec runCodec = (RunCodec) codec;
            int k = runCodec.getK();
            int kb;
            int kx;
            if(k <= 256) {
                kb = 0;
                kx = k - 1;
            } else if (k <= 4096) {
                kb = 1;
                kx = k/16 - 1;
            } else if (k <= 65536) {
                kb = 2;
                kx = k/256 - 1;
            } else {
                kb = 3;
                kx = k/4096 - 1;
            }
            Codec aCodec = runCodec.getACodec();
            Codec bCodec = runCodec.getBCodec();
            int abDef = 0;
            if(aCodec.equals(defaultForBand)) {
                abDef = 1;
            } else if (bCodec.equals(defaultForBand)) {
                abDef = 2;
            }
            int first = 117 + kb + (kx==3 ? 0 : 4) + (8 * abDef);
            int[] aSpecifier = abDef == 1 ? new int[0] : getSpecifier(aCodec, defaultForBand);
            int[] bSpecifier = abDef == 2 ? new int[0] : getSpecifier(bCodec, defaultForBand);
            int[] specifier = new int[1 + (kx==3 ? 0 : 1) + aSpecifier.length + bSpecifier.length];
            specifier[0] = first;
            int index = 1;
            if(kx != 3) {
                specifier[1] = kx;
                index++;
            }
            for (int i = 0; i < aSpecifier.length; i++) {
                specifier[index] = aSpecifier[i];
                index++;
            }
            for (int i = 0; i < bSpecifier.length; i++) {
                specifier[index] = bSpecifier[i];
                index++;
            }
            return specifier;
        } else if (codec instanceof PopulationCodec) {
            PopulationCodec populationCodec = (PopulationCodec) codec;
            Codec tokenCodec = populationCodec.getTokenCodec();
            Codec favouredCodec = populationCodec.getFavouredCodec();
            Codec unfavouredCodec = populationCodec.getUnfavouredCodec();
            int fDef = favouredCodec.equals(defaultForBand) ? 1 : 0;
            int uDef = unfavouredCodec.equals(defaultForBand) ? 1 : 0;
            int tDefL = 0;
            int[] favoured = populationCodec.getFavoured();
            if(favoured != null) {
                int k = favoured.length;
                if(tokenCodec == Codec.BYTE1) {
                    tDefL = 1;
                } else if (tokenCodec instanceof BHSDCodec) {
                    BHSDCodec tokenBHSD = (BHSDCodec) tokenCodec;
                    if(tokenBHSD.getS() == 0) {
                        int[] possibleLValues = new int[] {4, 8, 16, 32, 64, 128, 192, 224, 240, 248, 252};
                        int l = 256-tokenBHSD.getH();
                        int index = Arrays.binarySearch(possibleLValues, l);
                        if(index != -1) {
                            // TODO: check range is ok for ks
                            tDefL = index++;
                        }
                    }
                }
            }
            int first = 141 + fDef + (2 * uDef) + (4 * tDefL);
            int[] favouredSpecifier = fDef == 1 ? new int[0] : getSpecifier(favouredCodec, defaultForBand);
            int[] tokenSpecifier = tDefL != 0 ? new int[0] : getSpecifier(tokenCodec, defaultForBand);
            int[] unfavouredSpecifier = uDef == 1 ? new int[0] : getSpecifier(unfavouredCodec, defaultForBand);
            int[] specifier = new int[1 + favouredSpecifier.length + unfavouredSpecifier.length + tokenSpecifier.length];
            specifier[0] = first;
            int index = 1;
            for (int i = 0; i < favouredSpecifier.length; i++) {
                specifier[index] = favouredSpecifier[i];
                index++;
            }
            for (int i = 0; i < tokenSpecifier.length; i++) {
                specifier[index] = tokenSpecifier[i];
                index++;
            }
            for (int i = 0; i < unfavouredSpecifier.length; i++) {
                specifier[index] = unfavouredSpecifier[i];
                index++;
            }
            return specifier;
        }

        return null;
    }

    public static BHSDCodec getCanonicalCodec(int i) {
        return canonicalCodec[i];
    }
}

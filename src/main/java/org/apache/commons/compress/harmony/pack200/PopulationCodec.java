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
import java.io.InputStream;
import java.util.Arrays;

/**
 * A PopulationCodec is a Codec that is well suited to encoding data that shows statistical or repetitive patterns,
 * containing for example a few numbers which are repeated a lot throughout the set, but not necessarily sequentially.
 */
public class PopulationCodec extends Codec {

    private final Codec favouredCodec;
    private Codec tokenCodec;
    private final Codec unfavouredCodec;
    private int l;
    private int[] favoured;

    public PopulationCodec(final Codec favouredCodec, final Codec tokenCodec, final Codec unvafouredCodec) {
        this.favouredCodec = favouredCodec;
        this.tokenCodec = tokenCodec;
        this.unfavouredCodec = unvafouredCodec;
    }

    public PopulationCodec(final Codec favouredCodec, final int l, final Codec unfavouredCodec) {
        if (l >= 256 || l <= 0) {
            throw new IllegalArgumentException("L must be between 1..255");
        }
        this.favouredCodec = favouredCodec;
        this.l = l;
        this.unfavouredCodec = unfavouredCodec;
    }

    @Override
    public int decode(final InputStream in) throws IOException, Pack200Exception {
        throw new Pack200Exception("Population encoding does not work unless the number of elements are known");
    }

    @Override
    public int decode(final InputStream in, final long last) throws IOException, Pack200Exception {
        throw new Pack200Exception("Population encoding does not work unless the number of elements are known");
    }

    @Override
    public int[] decodeInts(final int n, final InputStream in) throws IOException, Pack200Exception {
        lastBandLength = 0;
        favoured = new int[n]; // there must be <= n values, but probably a lot
        // less
        int[] result;
        // read table of favorites first
        int smallest = Integer.MAX_VALUE, absoluteSmallest;
        int last = 0;
        int value = 0, absoluteValue;
        int k = -1;
        while (true) {
            value = favouredCodec.decode(in, last);
            if (k > -1 && (value == smallest || value == last)) {
                break;
            }
            favoured[++k] = value;
            absoluteSmallest = Math.abs(smallest);
            absoluteValue = Math.abs(value);
            if (absoluteSmallest > absoluteValue) {
                smallest = value;
            } else if (absoluteSmallest == absoluteValue) {
                // ensure that -X and +X -> +X
                smallest = absoluteSmallest;
            }
            last = value;
        }
        lastBandLength += k;
        // if tokenCodec needs to be derived from the T, L and K values
        if (tokenCodec == null) {
            if (k < 256) {
                tokenCodec = Codec.BYTE1;
            } else {
                // if k >= 256, b >= 2
                int b = 1;
                BHSDCodec codec = null;
                while (++b < 5) {
                    codec = new BHSDCodec(b, 256 - l, 0);
                    if (codec.encodes(k)) {
                        tokenCodec = codec;
                        break;
                    }
                }
                if (tokenCodec == null) {
                    throw new Pack200Exception("Cannot calculate token codec from " + k + " and " + l);
                }
            }
        }
        // read favorites
        lastBandLength += n;
        result = tokenCodec.decodeInts(n, in);
        // read unfavorites
        last = 0;
        for (int i = 0; i < n; i++) {
            final int index = result[i];
            if (index == 0) {
                lastBandLength++;
                result[i] = last = unfavouredCodec.decode(in, last);
            } else {
                result[i] = favoured[index - 1];
            }
        }
        return result;
    }

    @Override
    public byte[] encode(final int value) throws Pack200Exception {
        throw new Pack200Exception("Population encoding does not work unless the number of elements are known");
    }

    @Override
    public byte[] encode(final int value, final int last) throws Pack200Exception {
        throw new Pack200Exception("Population encoding does not work unless the number of elements are known");
    }

    public byte[] encode(final int[] favoured, final int[] tokens, final int[] unfavoured) throws Pack200Exception {
        final int[] favoured2 = Arrays.copyOf(favoured, favoured.length + 1);
        favoured2[favoured2.length - 1] = favoured[favoured.length - 1]; // repeat last value;
        final byte[] favouredEncoded = favouredCodec.encode(favoured2);
        final byte[] tokensEncoded = tokenCodec.encode(tokens);
        final byte[] unfavouredEncoded = unfavouredCodec.encode(unfavoured);
        final byte[] band = new byte[favouredEncoded.length + tokensEncoded.length + unfavouredEncoded.length];
        System.arraycopy(favouredEncoded, 0, band, 0, favouredEncoded.length);
        System.arraycopy(tokensEncoded, 0, band, favouredEncoded.length, tokensEncoded.length);
        System.arraycopy(unfavouredEncoded, 0, band, favouredEncoded.length + tokensEncoded.length,
            unfavouredEncoded.length);
        return band;
    }

    public int[] getFavoured() {
        return favoured;
    }

    public Codec getFavouredCodec() {
        return favouredCodec;
    }

    public Codec getTokenCodec() {
        return tokenCodec;
    }

    public Codec getUnfavouredCodec() {
        return unfavouredCodec;
    }
}

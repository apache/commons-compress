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
package org.apache.commons.compress.harmony.pack200;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * A PopulationCodec is a Codec that is well suited to encoding data that shows statistical or repetitive patterns, containing for example a few numbers which
 * are repeated a lot throughout the set, but not necessarily sequentially.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class PopulationCodec extends Codec {

    private final Codec favoredCodec;
    private Codec tokenCodec;
    private final Codec unfavoredCodec;
    private int l;
    private int[] favored;

    /**
     * Constructs a new PopulationCodec.
     *
     * @param favoredCodec The favored codec.
     * @param tokenCodec The token codec.
     * @param unvaforedCodec The unfavored codec.
     */
    public PopulationCodec(final Codec favoredCodec, final Codec tokenCodec, final Codec unvaforedCodec) {
        this.favoredCodec = favoredCodec;
        this.tokenCodec = tokenCodec;
        this.unfavoredCodec = unvaforedCodec;
    }

    /**
     * Constructs a new PopulationCodec.
     *
     * @param favoredCodec The favored codec.
     * @param l The L value.
     * @param unfavoredCodec The unfavored codec.
     */
    public PopulationCodec(final Codec favoredCodec, final int l, final Codec unfavoredCodec) {
        if (l >= 256 || l <= 0) {
            throw new IllegalArgumentException("L must be between 1..255");
        }
        this.favoredCodec = favoredCodec;
        this.l = l;
        this.unfavoredCodec = unfavoredCodec;
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
        favored = new int[Pack200Exception.checkIntArray(check(n, in))]; // there must be <= n values, but probably a lot
        // less
        final int[] result;
        // read table of favorites first
        int smallest = Integer.MAX_VALUE;
        int absoluteSmallest;
        int last = 0;
        int value = 0;
        int absoluteValue;
        int k = -1;
        while (true) {
            value = favoredCodec.decode(in, last);
            if (k > -1 && (value == smallest || value == last)) {
                break;
            }
            favored[++k] = value;
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
                tokenCodec = BYTE1;
            } else {
                // if k >= 256, b >= 2
                int b = 1;
                BHSDCodec codec;
                while (++b < 5) {
                    codec = new BHSDCodec(b, 256 - l, 0);
                    if (codec.encodes(k)) {
                        tokenCodec = codec;
                        break;
                    }
                }
                Pack200Exception.requireNonNull(tokenCodec, "Cannot calculate token codec from %s and %s", k, l);
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
                result[i] = last = unfavoredCodec.decode(in, last);
            } else {
                result[i] = favored[index - 1];
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

    /**
     * Encodes the values.
     *
     * @param favored The favored values.
     * @param tokens The tokens.
     * @param unfavored The unfavored values.
     * @return The encoded bytes.
     * @throws Pack200Exception if an error occurs.
     */
    public byte[] encode(final int[] favored, final int[] tokens, final int[] unfavored) throws Pack200Exception {
        final int[] favored2 = Arrays.copyOf(favored, favored.length + 1);
        favored2[favored2.length - 1] = favored[favored.length - 1]; // repeat last value;
        final byte[] favoredEncoded = favoredCodec.encode(favored2);
        final byte[] tokensEncoded = tokenCodec.encode(tokens);
        final byte[] unfavoredEncoded = unfavoredCodec.encode(unfavored);
        final byte[] band = new byte[favoredEncoded.length + tokensEncoded.length + unfavoredEncoded.length];
        System.arraycopy(favoredEncoded, 0, band, 0, favoredEncoded.length);
        System.arraycopy(tokensEncoded, 0, band, favoredEncoded.length, tokensEncoded.length);
        System.arraycopy(unfavoredEncoded, 0, band, favoredEncoded.length + tokensEncoded.length, unfavoredEncoded.length);
        return band;
    }

    /**
     * Gets the favored values.
     *
     * @return The favored values.
     */
    public int[] getFavoured() {
        return favored;
    }

    /**
     * Gets the favored codec.
     *
     * @return The favored codec.
     */
    public Codec getFavouredCodec() {
        return favoredCodec;
    }

    /**
     * Gets the token codec.
     *
     * @return The token codec.
     */
    public Codec getTokenCodec() {
        return tokenCodec;
    }

    /**
     * Gets the unfavored codec.
     *
     * @return The unfavored codec.
     */
    public Codec getUnfavouredCodec() {
        return unfavoredCodec;
    }
}

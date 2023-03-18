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

import org.apache.commons.compress.utils.ExactMath;

/**
 * A run codec is a grouping of two nested codecs; K values are decoded from the first codec, and the remaining codes
 * are decoded from the remaining codec. Note that since this codec maintains state, the instances are not reusable.
 */
public class RunCodec extends Codec {

    private int k;
    private final Codec aCodec;
    private final Codec bCodec;
    private int last;

    public RunCodec(final int k, final Codec aCodec, final Codec bCodec) throws Pack200Exception {
        if (k <= 0) {
            throw new Pack200Exception("Cannot have a RunCodec for a negative number of numbers");
        }
        if (aCodec == null || bCodec == null) {
            throw new Pack200Exception("Must supply both codecs for a RunCodec");
        }
        this.k = k;
        this.aCodec = aCodec;
        this.bCodec = bCodec;
    }

    @Override
    public int decode(final InputStream in) throws IOException, Pack200Exception {
        return decode(in, this.last);
    }

    @Override
    public int decode(final InputStream in, final long last) throws IOException, Pack200Exception {
        if (--k >= 0) {
            final int value = aCodec.decode(in, this.last);
            this.last = (k == 0 ? 0 : value);
            return normalise(value, aCodec);
        }
        this.last = bCodec.decode(in, this.last);
        return normalise(this.last, bCodec);
    }

    @Override
    public int[] decodeInts(final int n, final InputStream in) throws IOException, Pack200Exception {
        final int[] band = new int[n];
        final int[] aValues = aCodec.decodeInts(k, in);
        normalise(aValues, aCodec);
        final int[] bValues = bCodec.decodeInts(n - k, in);
        normalise(bValues, bCodec);
        System.arraycopy(aValues, 0, band, 0, k);
        System.arraycopy(bValues, 0, band, k, n - k);
        lastBandLength = aCodec.lastBandLength + bCodec.lastBandLength;
        return band;
    }

    @Override
    public byte[] encode(final int value) throws Pack200Exception {
        throw new Pack200Exception("Must encode entire band at once with a RunCodec");
    }

    @Override
    public byte[] encode(final int value, final int last) throws Pack200Exception {
        throw new Pack200Exception("Must encode entire band at once with a RunCodec");
    }

    public Codec getACodec() {
        return aCodec;
    }

    public Codec getBCodec() {
        return bCodec;
    }

    public int getK() {
        return k;
    }

    private int normalise(int value, final Codec codecUsed) {
        if (codecUsed instanceof BHSDCodec) {
            final BHSDCodec bhsd = (BHSDCodec) codecUsed;
            if (bhsd.isDelta()) {
                final long cardinality = bhsd.cardinality();
                while (value > bhsd.largest()) {
                    value -= cardinality;
                }
                while (value < bhsd.smallest()) {
                    value = ExactMath.add(value, cardinality);
                }
            }
        }
        return value;
    }

    private void normalise(final int[] band, final Codec codecUsed) {
        if (codecUsed instanceof BHSDCodec) {
            final BHSDCodec bhsd = (BHSDCodec) codecUsed;
            if (bhsd.isDelta()) {
                final long cardinality = bhsd.cardinality();
                for (int i = 0; i < band.length; i++) {
                    while (band[i] > bhsd.largest()) {
                        band[i] -= cardinality;
                    }
                    while (band[i] < bhsd.smallest()) {
                        band[i] = ExactMath.add(band[i], cardinality);
                    }
                }
            }
        } else if (codecUsed instanceof PopulationCodec) {
            final PopulationCodec popCodec = (PopulationCodec) codecUsed;
            final int[] favoured = popCodec.getFavoured().clone();
            Arrays.sort(favoured);
            for (int i = 0; i < band.length; i++) {
                final boolean favouredValue = Arrays.binarySearch(favoured, band[i]) > -1;
                final Codec theCodec = favouredValue ? popCodec.getFavouredCodec() : popCodec.getUnfavouredCodec();
                if (theCodec instanceof BHSDCodec) {
                    final BHSDCodec bhsd = (BHSDCodec) theCodec;
                    if (bhsd.isDelta()) {
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
        }
    }

    @Override
    public String toString() {
        return "RunCodec[k=" + k + ";aCodec=" + aCodec + "bCodec=" + bCodec + "]";
    }
}

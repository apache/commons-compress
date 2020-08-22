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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 * A run codec is a grouping of two nested codecs; K values are decoded from the
 * first codec, and the remaining codes are decoded from the remaining codec.
 * Note that since this codec maintains state, the instances are not reusable.
 */
public class RunCodec extends Codec {

    private int k;
    private final Codec aCodec;
    private final Codec bCodec;
    private int last;

    public RunCodec(int k, Codec aCodec, Codec bCodec) throws Pack200Exception {
        if (k <= 0) {
            throw new Pack200Exception(
                    "Cannot have a RunCodec for a negative number of numbers");
        }
        if (aCodec == null || bCodec == null) {
            throw new Pack200Exception("Must supply both codecs for a RunCodec");
        }
        this.k = k;
        this.aCodec = aCodec;
        this.bCodec = bCodec;
    }

    public int decode(InputStream in) throws IOException, Pack200Exception {
        return decode(in, this.last);
    }

    public int decode(InputStream in, long last) throws IOException,
            Pack200Exception {
        if (--k >= 0) {
            int value = aCodec.decode(in, this.last);
            this.last = (k == 0 ? 0 : value);
            return normalise(value, aCodec);
        } else {
            this.last = bCodec.decode(in, this.last);
            return normalise(this.last, bCodec);
        }
    }

    private int normalise(int value, Codec codecUsed) {
        if (codecUsed instanceof BHSDCodec) {
            BHSDCodec bhsd = (BHSDCodec) codecUsed;
            if (bhsd.isDelta()) {
                long cardinality = bhsd.cardinality();
                while (value > bhsd.largest()) {
                    value -= cardinality;
                }
                while (value < bhsd.smallest()) {
                    value += cardinality;
                }
            }
        }
        return value;
    }

    public int[] decodeInts(int n, InputStream in) throws IOException,
            Pack200Exception {
        int[] band = new int[n];
        int[] aValues = aCodec.decodeInts(k, in);
        normalise(aValues, aCodec);
        int[] bValues = bCodec.decodeInts(n - k, in);
        normalise(bValues, bCodec);
        System.arraycopy(aValues, 0, band, 0, k);
        System.arraycopy(bValues, 0, band, k, n - k);
        lastBandLength = aCodec.lastBandLength + bCodec.lastBandLength;
        return band;
    }

    private void normalise(int[] band, Codec codecUsed) {
        if (codecUsed instanceof BHSDCodec) {
            BHSDCodec bhsd = (BHSDCodec) codecUsed;
            if (bhsd.isDelta()) {
                long cardinality = bhsd.cardinality();
                for (int i = 0; i < band.length; i++) {
                    while (band[i] > bhsd.largest()) {
                        band[i] -= cardinality;
                    }
                    while (band[i] < bhsd.smallest()) {
                        band[i] += cardinality;
                    }
                }
            }
        } else if (codecUsed instanceof PopulationCodec) {
            PopulationCodec popCodec = (PopulationCodec) codecUsed;
            int[] favoured = (int[]) popCodec.getFavoured().clone();
            Arrays.sort(favoured);
            for (int i = 0; i < band.length; i++) {
                boolean favouredValue = Arrays.binarySearch(favoured, band[i]) > -1;
                Codec theCodec = favouredValue ? popCodec.getFavouredCodec()
                        : popCodec.getUnfavouredCodec();
                if (theCodec instanceof BHSDCodec) {
                    BHSDCodec bhsd = (BHSDCodec) theCodec;
                    if (bhsd.isDelta()) {
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
        }
    }
    
    public String toString() {
        return "RunCodec[k=" + k + ";aCodec=" + aCodec + "bCodec=" + bCodec
                + "]";
    }

    public byte[] encode(int value, int last) throws Pack200Exception {
        throw new Pack200Exception("Must encode entire band at once with a RunCodec");
    }

    public byte[] encode(int value) throws Pack200Exception {
        throw new Pack200Exception("Must encode entire band at once with a RunCodec");
    }

    public int getK() {
        return k;
    }

    public Codec getACodec() {
        return aCodec;
    }

    public Codec getBCodec() {
        return bCodec;
    }
}

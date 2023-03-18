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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.utils.ExactMath;

/**
 * A BHSD codec is a means of encoding integer values as a sequence of bytes or vice versa using a specified "BHSD"
 * encoding mechanism. It uses a variable-length encoding and a modified sign representation such that small numbers are
 * represented as a single byte, whilst larger numbers take more bytes to encode. The number may be signed or unsigned;
 * if it is unsigned, it can be weighted towards positive numbers or equally distributed using a one's complement. The
 * Codec also supports delta coding, where a sequence of numbers is represented as a series of first-order differences.
 * So a delta encoding of the integers [1..10] would be represented as a sequence of 10x1s. This allows the absolute
 * value of a coded integer to fall outside of the 'small number' range, whilst still being encoded as a single byte.
 *
 * A BHSD codec is configured with four parameters:
 * <dl>
 * <dt>B</dt>
 * <dd>The maximum number of bytes that each value is encoded as. B must be a value between [1..5]. For a pass-through
 * coding (where each byte is encoded as itself, aka {@link #BYTE1}, B is 1 (each byte takes a maximum of 1 byte).</dd>
 * <dt>H</dt>
 * <dd>The radix of the integer. Values are defined as a sequence of values, where value {@code n} is multiplied by
 * {@code H^<sup>n</sup>}. So the number 1234 may be represented as the sequence 4 3 2 1 with a radix (H) of 10.
 * Note that other permutations are also possible; 43 2 1 will also encode 1234. The co-parameter L is defined as 256-H.
 * This is important because only the last value in a sequence may be &lt; L; all prior values must be &gt; L.</dd>
 * <dt>S</dt>
 * <dd>Whether the codec represents signed values (or not). This may have 3 values; 0 (unsigned), 1 (signed, one's
 * complement) or 2 (signed, two's complement)</dd>
 * <dt>D</dt>
 * <dd>Whether the codec represents a delta encoding. This may be 0 (no delta) or 1 (delta encoding). A delta encoding
 * of 1 indicates that values are cumulative; a sequence of {@code 1 1 1 1 1} will represent the sequence
 * {@code 1 2 3 4 5}. For this reason, the codec supports two variants of decode; one
 * {@link #decode(InputStream, long) with} and one {@link #decode(InputStream) without} a {@code last} parameter.
 * If the codec is a non-delta encoding, then the value is ignored if passed. If the codec is a delta encoding, it is a
 * run-time error to call the value without the extra parameter, and the previous value should be returned. (It was
 * designed this way to support multi-threaded access without requiring a new instance of the Codec to be cloned for
 * each use.)
 * </dd>
 * </dl>
 *
 * Codecs are notated as (B,H,S,D) and either D or S,D may be omitted if zero. Thus {@link #BYTE1} is denoted
 * (1,256,0,0) or (1,256). The {@link #toString()} method prints out the condensed form of the encoding. Often, the last
 * character in the name ({@link #BYTE1}, {@link #UNSIGNED5}) gives a clue as to the B value. Those that start with U
 * ({@link #UDELTA5}, {@link #UNSIGNED5}) are unsigned; otherwise, in most cases, they are signed. The presence of the
 * word Delta ({@link #DELTA5}, {@link #UDELTA5}) indicates a delta encoding is used.
 *
 */
public final class BHSDCodec extends Codec {

    /**
     * The maximum number of bytes in each coding word
     */
    private final int b;

    /**
     * Whether delta encoding is used (0=false,1=true)
     */
    private final int d;

    /**
     * The radix of the encoding
     */
    private final int h;

    /**
     * The co-parameter of h; 256-h
     */
    private final int l;

    /**
     * Represents signed numbers or not (0=unsigned,1/2=signed)
     */
    private final int s;

    private long cardinality;

    private final long smallest;

    private final long largest;

    /**
     * radix^i powers
     */
    private final long[] powers;

    /**
     * Constructs an unsigned, non-delta Codec with the given B and H values.
     *
     * @param b the maximum number of bytes that a value can be encoded as [1..5]
     * @param h the radix of the encoding [1..256]
     */
    public BHSDCodec(final int b, final int h) {
        this(b, h, 0, 0);
    }

    /**
     * Constructs a non-delta Codec with the given B, H and S values.
     *
     * @param b the maximum number of bytes that a value can be encoded as [1..5]
     * @param h the radix of the encoding [1..256]
     * @param s whether the encoding represents signed numbers (s=0 is unsigned; s=1 is signed with 1s complement; s=2
     *        is signed with ?)
     */
    public BHSDCodec(final int b, final int h, final int s) {
        this(b, h, s, 0);
    }

    /**
     * Constructs a Codec with the given B, H, S and D values.
     *
     * @param b the maximum number of bytes that a value can be encoded as [1..5]
     * @param h the radix of the encoding [1..256]
     * @param s whether the encoding represents signed numbers (s=0 is unsigned; s=1 is signed with 1s complement; s=2
     *        is signed with ?)
     * @param d whether this is a delta encoding (d=0 is non-delta; d=1 is delta)
     */
    public BHSDCodec(final int b, final int h, final int s, final int d) {
        if (b < 1 || b > 5) {
            throw new IllegalArgumentException("1<=b<=5");
        }
        if (h < 1 || h > 256) {
            throw new IllegalArgumentException("1<=h<=256");
        }
        if (s < 0 || s > 2) {
            throw new IllegalArgumentException("0<=s<=2");
        }
        if (d < 0 || d > 1) {
            throw new IllegalArgumentException("0<=d<=1");
        }
        if (b == 1 && h != 256) {
            throw new IllegalArgumentException("b=1 -> h=256");
        }
        if (h == 256 && b == 5) {
            throw new IllegalArgumentException("h=256 -> b!=5");
        }
        this.b = b;
        this.h = h;
        this.s = s;
        this.d = d;
        this.l = 256 - h;
        if (h == 1) {
            cardinality = b * 255 + 1;
        } else {
            cardinality = (long) ((long) (l * (1 - Math.pow(h, b)) / (1 - h)) + Math.pow(h, b));
        }
        smallest = calculateSmallest();
        largest = calculateLargest();

        powers = new long[b];
        Arrays.setAll(powers, c -> (long) Math.pow(h, c));
    }

    private long calculateLargest() {
        long result;
        // TODO This can probably be optimized into a better mathematical
        // statement
        if (d == 1) {
            final BHSDCodec bh0 = new BHSDCodec(b, h);
            return bh0.largest();
        }
        switch (s) {
        case 0:
            result = cardinality() - 1;
            break;
        case 1:
            result = cardinality() / 2 - 1;
            break;
        case 2:
            result = (3L * cardinality()) / 4 - 1;
            break;
        default:
            throw new Error("Unknown s value");
        }
        return Math.min((s == 0 ? ((long) Integer.MAX_VALUE) << 1 : Integer.MAX_VALUE) - 1, result);
    }

    private long calculateSmallest() {
        long result;
        if (d == 1 || !isSigned()) {
            if (cardinality >= 4294967296L) { // 2^32
                result = Integer.MIN_VALUE;
            } else {
                result = 0;
            }
        } else {
            result = Math.max(Integer.MIN_VALUE, -cardinality() / (1 << s));
        }
        return result;
    }

    /**
     * Returns the cardinality of this codec; that is, the number of distinct values that it can contain.
     *
     * @return the cardinality of this codec
     */
    public long cardinality() {
        return cardinality;
    }

    @Override
    public int decode(final InputStream in) throws IOException, Pack200Exception {
        if (d != 0) {
            throw new Pack200Exception("Delta encoding used without passing in last value; this is a coding error");
        }
        return decode(in, 0);
    }

    @Override
    public int decode(final InputStream in, final long last) throws IOException, Pack200Exception {
        int n = 0;
        long z = 0;
        long x = 0;

        do {
            x = in.read();
            lastBandLength++;
            z += x * powers[n];
            n++;
        } while (x >= l && n < b);

        if (x == -1) {
            throw new EOFException("End of stream reached whilst decoding");
        }

        if (isSigned()) {
            final int u = ((1 << s) - 1);
            if ((z & u) == u) {
                z = z >>> s ^ -1L;
            } else {
                z = z - (z >>> s);
            }
        }
        // This algorithm does the same thing, but is probably slower. Leaving
        // in for now for readability
        // if (isSigned()) {
        // long u = z;
        // long twoPowS = (long)Math.pow(2, s);
        // double twoPowSMinusOne = twoPowS-1;
        // if (u % twoPowS < twoPowSMinusOne) {
        // if (cardinality < Math.pow(2, 32)) {
        // z = (long) (u - (Math.floor(u/ twoPowS)));
        // } else {
        // z = cast32((long) (u - (Math.floor(u/ twoPowS))));
        // }
        // } else {
        // z = (long) (-Math.floor(u/ twoPowS) - 1);
        // }
        // }
        if (isDelta()) {
            z += last;
        }
        return (int) z;
    }

    // private long cast32(long u) {
    // u = (long) ((long) ((u + Math.pow(2, 31)) % Math.pow(2, 32)) -
    // Math.pow(2, 31));
    // return u;
    // }

    @Override
    public int[] decodeInts(final int n, final InputStream in) throws IOException, Pack200Exception {
        final int[] band = super.decodeInts(n, in);
        if (isDelta()) {
            for (int i = 0; i < band.length; i++) {
                while (band[i] > largest) {
                    band[i] -= cardinality;
                }
                while (band[i] < smallest) {
                    band[i] = ExactMath.add(band[i], cardinality);
                }
            }
        }
        return band;
    }

    @Override
    public int[] decodeInts(final int n, final InputStream in, final int firstValue)
        throws IOException, Pack200Exception {
        final int[] band = super.decodeInts(n, in, firstValue);
        if (isDelta()) {
            for (int i = 0; i < band.length; i++) {
                while (band[i] > largest) {
                    band[i] -= cardinality;
                }
                while (band[i] < smallest) {
                    band[i] = ExactMath.add(band[i], cardinality);
                }
            }
        }
        return band;
    }

    @Override
    public byte[] encode(final int value) throws Pack200Exception {
        return encode(value, 0);
    }

    @Override
    public byte[] encode(final int value, final int last) throws Pack200Exception {
        if (!encodes(value)) {
            throw new Pack200Exception("The codec " + this + " does not encode the value " + value);
        }

        long z = value;
        if (isDelta()) {
            z -= last;
        }
        if (isSigned()) {
            if (z < Integer.MIN_VALUE) {
                z += 4294967296L;
            } else if (z > Integer.MAX_VALUE) {
                z -= 4294967296L;
            }
            if (z < 0) {
                z = (-z << s) - 1;
            } else if (s == 1) {
                z = z << s;
            } else {
                z += (z - z % 3) / 3;
            }
        } else if (z < 0) {
            // Need to use integer overflow here to represent negatives.
            // 4294967296L is the 1 << 32.
            z += Math.min(cardinality, 4294967296L);
        }
        if (z < 0) {
            throw new Pack200Exception("unable to encode");
        }

        final List<Byte> byteList = new ArrayList<>();
        for (int n = 0; n < b; n++) {
            long byteN;
            if (z < l) {
                byteN = z;
            } else {
                byteN = z % h;
                while (byteN < l) {
                    byteN += h;
                }
            }
            byteList.add(Byte.valueOf((byte) byteN));
            if (byteN < l) {
                break;
            }
            z -= byteN;
            z /= h;
        }
        final byte[] bytes = new byte[byteList.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = byteList.get(i).byteValue();
        }
        return bytes;
    }

    /**
     * True if this encoding can code the given value
     *
     * @param value the value to check
     * @return {@code true} if the encoding can encode this value
     */
    public boolean encodes(final long value) {
        return value >= smallest && value <= largest;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof BHSDCodec) {
            final BHSDCodec codec = (BHSDCodec) o;
            return codec.b == b && codec.h == h && codec.s == s && codec.d == d;
        }
        return false;
    }

    /**
     * @return the b
     */
    public int getB() {
        return b;
    }

    /**
     * @return the h
     */
    public int getH() {
        return h;
    }

    /**
     * @return the l
     */
    public int getL() {
        return l;
    }

    /**
     * @return the s
     */
    public int getS() {
        return s;
    }

    @Override
    public int hashCode() {
        return ((b * 37 + h) * 37 + s) * 37 + d;
    }

    /**
     * Returns true if this codec is a delta codec
     *
     * @return true if this codec is a delta codec
     */
    public boolean isDelta() {
        return d != 0;
    }

    /**
     * Returns true if this codec is a signed codec
     *
     * @return true if this codec is a signed codec
     */
    public boolean isSigned() {
        return s != 0;
    }

    /**
     * Returns the largest value that this codec can represent.
     *
     * @return the largest value that this codec can represent.
     */
    public long largest() {
        return largest;
    }

    /**
     * Returns the smallest value that this codec can represent.
     *
     * @return the smallest value that this codec can represent.
     */
    public long smallest() {
        return smallest;
    }

    /**
     * Returns the codec in the form (1,256) or (1,64,1,1). Note that trailing zero fields are not shown.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(11);
        buffer.append('(');
        buffer.append(b);
        buffer.append(',');
        buffer.append(h);
        if (s != 0 || d != 0) {
            buffer.append(',');
            buffer.append(s);
        }
        if (d != 0) {
            buffer.append(',');
            buffer.append(d);
        }
        buffer.append(')');
        return buffer.toString();
    }
}

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

/**
 * A Codec allows a sequence of bytes to be decoded into integer values (or vice versa).
 *
 * There are a number of standard Codecs ({@link #UDELTA5}, {@link #UNSIGNED5}, {@link #BYTE1}, {@link #CHAR3}) that are
 * used in the implementation of many bands; but there are a variety of other ones, and indeed the specification assumes
 * that other combinations of values can result in more specific and efficient formats. There are also a sequence of
 * canonical encodings defined by the Pack200 specification, which allow a Codec to be referred to by canonical number.
 * {@link CodecEncoding#getCodec(int, InputStream, Codec)})
 */
public abstract class Codec {

    /**
     * BCI5 = (5,4): Used for storing branching information in bytecode.
     */
    public static final BHSDCodec BCI5 = new BHSDCodec(5, 4);

    /**
     * BRANCH5 = (5,4,2): Used for storing branching information in bytecode.
     */
    public static final BHSDCodec BRANCH5 = new BHSDCodec(5, 4, 2);

    /**
     * BYTE1 = (1,256): Used for storing plain bytes.
     */
    public static final BHSDCodec BYTE1 = new BHSDCodec(1, 256);

    /**
     * CHAR3 = (3,128): Used for storing text (UTF-8) strings. NB This isn't quite the same as UTF-8, but has similar
     * properties; ASCII characters &lt; 127 are stored in a single byte.
     */
    public static final BHSDCodec CHAR3 = new BHSDCodec(3, 128);

    /**
     * DELTA5 = (5,64,1,1): Used for the majority of numerical codings where there is a correlated sequence of signed
     * values.
     */
    public static final BHSDCodec DELTA5 = new BHSDCodec(5, 64, 1, 1);

    /**
     * MDELTA5 = (5,64,2,1): Used for the majority of numerical codings where there is a correlated sequence of signed
     * values, but where most of them are expected to be non-negative.
     */
    public static final BHSDCodec MDELTA5 = new BHSDCodec(5, 64, 2, 1);

    /**
     * SIGNED5 = (5,64,1): Used for small signed values.
     */
    public static final BHSDCodec SIGNED5 = new BHSDCodec(5, 64, 1);

    /**
     * UDELTA5 = (5,64,0,1): Used for the majority of numerical codings where there is a correlated sequence of unsigned
     * values.
     */
    public static final BHSDCodec UDELTA5 = new BHSDCodec(5, 64, 0, 1);

    /**
     * UNSIGNED5 = (5,64): Used for small unsigned values.
     */
    public static final BHSDCodec UNSIGNED5 = new BHSDCodec(5, 64);

    public int lastBandLength;

    /**
     * Decode a sequence of bytes from the given input stream, returning the value as a long. Note that this method can
     * only be applied for non-delta encodings.
     *
     * @param in the input stream to read from
     * @return the value as a long
     * @throws IOException if there is a problem reading from the underlying input stream
     * @throws Pack200Exception if the encoding is a delta encoding
     */
    public abstract int decode(InputStream in) throws IOException, Pack200Exception;

    /**
     * Decode a sequence of bytes from the given input stream, returning the value as a long. If this encoding is a
     * delta encoding (d=1) then the previous value must be passed in as a parameter. If it is a non-delta encoding,
     * then it does not matter what value is passed in, so it makes sense for the value to be passed in by default using
     * code similar to:
     *
     * <pre>
     * long last = 0;
     * while (condition) {
     *     last = codec.decode(in, last);
     *     // do something with last
     * }
     * </pre>
     *
     * @param in the input stream to read from
     * @param last the previous value read, which must be supplied if the codec is a delta encoding
     * @return the value as a long
     * @throws IOException if there is a problem reading from the underlying input stream
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid
     */
    public abstract int decode(InputStream in, long last) throws IOException, Pack200Exception;

    /**
     * Decodes a sequence of {@code n} values from {@code in}. This should probably be used in most cases,
     * since some codecs (such as {@link PopulationCodec}) only work when the number of values to be read is known.
     *
     * @param n the number of values to decode
     * @param in the input stream to read from
     * @return an array of {@code int} values corresponding to values decoded
     * @throws IOException if there is a problem reading from the underlying input stream
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid
     */
    public int[] decodeInts(final int n, final InputStream in) throws IOException, Pack200Exception {
        lastBandLength = 0;
        final int[] result = new int[n];
        int last = 0;
        for (int i = 0; i < n; i++) {
            result[i] = last = decode(in, last);
        }
        return result;
    }

    /**
     * Decodes a sequence of {@code n} values from {@code in}.
     *
     * @param n the number of values to decode
     * @param in the input stream to read from
     * @param firstValue the first value in the band if it has already been read
     * @return an array of {@code int} values corresponding to values decoded, with firstValue as the first value
     *         in the array.
     * @throws IOException if there is a problem reading from the underlying input stream
     * @throws Pack200Exception if there is a problem decoding the value or that the value is invalid
     */
    public int[] decodeInts(final int n, final InputStream in, final int firstValue)
        throws IOException, Pack200Exception {
        final int[] result = new int[n + 1];
        result[0] = firstValue;
        int last = firstValue;
        for (int i = 1; i < n + 1; i++) {
            result[i] = last = decode(in, last);
        }
        return result;
    }

    /**
     * Encode a single value into a sequence of bytes. Note that this method can only be used for non-delta encodings.
     *
     * @param value the value to encode
     * @return the encoded bytes
     * @throws Pack200Exception TODO
     */
    public abstract byte[] encode(int value) throws Pack200Exception;

    /**
     * Encode a single value into a sequence of bytes.
     *
     * @param value the value to encode
     * @param last the previous value encoded (for delta encodings)
     * @return the encoded bytes
     * @throws Pack200Exception TODO
     */
    public abstract byte[] encode(int value, int last) throws Pack200Exception;

    /**
     * Encode a sequence of integers into a byte array
     *
     * @param ints the values to encode
     * @return byte[] encoded bytes
     * @throws Pack200Exception if there is a problem encoding any of the values
     */
    public byte[] encode(final int[] ints) throws Pack200Exception {
        int total = 0;
        final byte[][] bytes = new byte[ints.length][];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = encode(ints[i], i > 0 ? ints[i - 1] : 0);
            total += bytes[i].length;
        }
        final byte[] encoded = new byte[total];
        int index = 0;
        for (final byte[] element : bytes) {
            System.arraycopy(element, 0, encoded, index, element.length);
            index += element.length;
        }
        return encoded;
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.commons.compress.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility methods for reading and writing bytes.
 * @since 1.14
 */
public final class ByteUtils {
    private ByteUtils() { /* no instances */ }

    /**
     * Used to supply bytes.
     * @since 1.14
     */
    public interface ByteSupplier {
        /**
         * The contract is similar to {@link InputStream#read()}, return
         * the byte as an unsigned int, -1 if there are no more bytes.
         */
        int getAsByte() throws IOException;
    }

    /**
     * Used to consume bytes.
     * @since 1.14
     */
    public interface ByteConsumer {
        /**
         * The contract is similar to {@link OutputStream#write(int)},
         * consume the lower eight bytes of the int as a byte.
         */
        void accept(int b) throws IOException;
    }

    /**
     * Reads the given byte array as a little endian long.
     * @param bytes the byte array to convert
     */
    public static long fromLittleEndian(byte[] bytes) {
        return fromLittleEndian(bytes, 0, bytes.length);
    }

    /**
     * Reads the given byte array as a little endian long.
     * @param bytes the byte array to convert
     * @param off the offset into the array that starts the value
     * @param length the number of bytes representing the value
     * @throws IllegalArgumentException if len is bigger than eight
     */
    public static long fromLittleEndian(byte[] bytes, final int off, final int length) {
        if (length > 8) {
            throw new IllegalArgumentException("can't read more than eight bytes into a long value");
        }
        long l = 0;
        for (int i = 0; i < length; i++) {
            l |= (bytes[off + i] & 0xffl) << (8 * i);
        }
        return l;
    }

    /**
     * Reads the given number of bytes from the given stream as a little endian long.
     * @param in the stream to read from
     * @param length the number of bytes representing the value
     * @throws IllegalArgumentException if len is bigger than eight
     * @throws IOException if reading fails or the stream doesn't
     * contain the given number of bytes anymore
     */
    public static long fromLittleEndian(InputStream in, int length) throws IOException {
        return fromLittleEndian(new InputStreamByteSupplier(in), length);
    }

    /**
     * Reads the given number of bytes from the given supplier as a little endian long.
     * @param supplier the supplier for bytes
     * @param length the number of bytes representing the value
     * @throws IllegalArgumentException if len is bigger than eight
     * @throws IOException if the supplier fails or doesn't supply the
     * given number of bytes anymore
     */
    public static long fromLittleEndian(ByteSupplier supplier, final int length) throws IOException {
        if (length > 8) {
            throw new IllegalArgumentException("can't read more than eight bytes into a long value");
        }
        long l = 0;
        for (int i = 0; i < length; i++) {
            int b = supplier.getAsByte();
            if (b == -1) {
                throw new IOException("premature end of data");
            }
            l |= (b << (i * 8));
        }
        return l;
    }

    /**
     * Writes the given value to the given stream as a little endian
     * array of the given length.
     * @param out the stream to write to
     * @param value the value to write
     * @param length the number of bytes to use to represent the value
     * @throws IOException if writing fails
     */
    public static void toLittleEndian(OutputStream out, final long value, final int length)
        throws IOException {
        toLittleEndian(new OutputStreamByteConsumer(out), value, length);
    }

    /**
     * Provides the given value to the given consumer as a little endian
     * sequence of the given length.
     * @param consumer the consumer to provide the bytes to
     * @param value the value to provide
     * @param length the number of bytes to use to represent the value
     * @throws IOException if writing fails
     */
    public static void toLittleEndian(ByteConsumer consumer, final long value, final int length)
        throws IOException {
        long num = value;
        for (int i = 0; i < length; i++) {
            consumer.accept((int) (num & 0xff));
            num >>= 8;
        }
    }

    /**
     * {@link ByteSupplier} based on {@link InputStream}.
     * @since 1.14
     */
    public static class InputStreamByteSupplier implements ByteSupplier {
        private final InputStream is;
        public InputStreamByteSupplier(InputStream is) {
            this.is = is;
        }
        @Override
        public int getAsByte() throws IOException {
            return is.read();
        }
    }

    /**
     * {@link ByteConsumer} based on {@link OutputStream}.
     * @since 1.14
     */
    public static class OutputStreamByteConsumer implements ByteConsumer {
        private final OutputStream os;
        public OutputStreamByteConsumer(OutputStream os) {
            this.os = os;
        }
        @Override
        public void accept(int b) throws IOException {
            os.write(b);
        }
    }
}

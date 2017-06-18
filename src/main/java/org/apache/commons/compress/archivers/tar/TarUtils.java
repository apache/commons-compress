/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.tar;

import static org.apache.commons.compress.archivers.tar.TarConstants.CHKSUMLEN;
import static org.apache.commons.compress.archivers.tar.TarConstants.CHKSUM_OFFSET;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import org.apache.commons.compress.archivers.zip.HasCharset;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;

/**
 * This class provides static utility methods to work with byte streams.
 *
 * @Immutable
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF (bc)
public class TarUtils {

    private static final int BYTE_MASK = 255;

    static final ZipEncoding DEFAULT_ENCODING =
        ZipEncodingHelper.getZipEncoding(null);

    static final ZipEncoding FALLBACK_ENCODING = ZipEncodingHelper.getZipEncoding("ISO-LATIN-1");

    /**
     * Encapsulates the algorithms used up to Commons Compress 1.3 as ZipEncoding.
     */
    static final ZipEncoding OLD_FALLBACK_ENCODING = new ZipEncoding() {
        @Override
        public boolean canEncode(final String name) {
            return true;
        }

        @Override
        public ByteBuffer encode(final String name) {
            final int length = name.length();
            final byte[] buf = new byte[length];

            // copy until end of input or output is reached.
            for (int i = 0; i < length; ++i) {
                buf[i] = (byte) name.charAt(i);
            }
            return ByteBuffer.wrap(buf);
        }

        @Override
        public String decode(final byte[] buffer1) {
            final int length = buffer1.length;
            final StringBuilder result = new StringBuilder(length);

            for (final byte b : buffer1) {
                if (b == 0) { // Trailing null
                    break;
                }
                result.append((char) (b & 0xFF)); // Allow for sign-extension
            }

            return result.toString();
        }
    };

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private TarUtils() {
    }

    /**
     * Parse an octal string from a buffer.
     *
     * <p>Leading spaces are ignored. The buffer must contain a trailing space or NUL, and may
     * contain an additional trailing space or NUL.</p>
     *
     * <p>The input buffer is allowed to contain all NULs, in which case the method returns 0L (this
     * allows for missing fields).</p>
     *
     * <p>To work-around some tar implementations that insert a leading NUL this method returns 0 if
     * it detects a leading NUL since Commons Compress 1.4.</p>
     *
     * @param buffer The buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The maximum number of bytes to parse - must be at least 2 bytes.
     * @return The long value of the octal string.
     * @throws IllegalArgumentException if the trailing space/NUL is missing or if a invalid byte is
     * detected.
     */
    public static long parseOctal(final byte[] buffer, final int offset, final int length) {
        long result = 0;
        int end = offset + length;
        int start = offset;

        if (length < 2) {
            throw new IllegalArgumentException("Length " + length + " must be at least 2");
        }

        if (buffer[start] == 0) {
            return 0L;
        }

        // Skip leading spaces
        while (start < end) {
            if (buffer[start] == ' ') {
                start++;
            } else {
                break;
            }
        }

        // Trim all trailing NULs and spaces.
        // The ustar and POSIX tar specs require a trailing NUL or
        // space but some implementations use the extra digit for big
        // sizes/uids/gids ...
        byte trailer = buffer[end - 1];
        while (start < end && (trailer == 0 || trailer == ' ')) {
            end--;
            trailer = buffer[end - 1];
        }

        for (; start < end; start++) {
            final byte currentByte = buffer[start];
            // CheckStyle:MagicNumber OFF
            if (currentByte < '0' || currentByte > '7') {
                throw new IllegalArgumentException(
                    exceptionMessage(buffer, offset, length, start, currentByte));
            }
            result = (result << 3) + (currentByte - '0'); // convert from ASCII
            // CheckStyle:MagicNumber ON
        }

        return result;
    }

    /**
     * Parse an octal string from a buffer.
     *
     * <p>Leading spaces are ignored. </p>
     *
     * <p>The input buffer is allowed to contain all NULs, in which case the method returns 0L (this
     * allows for missing fields).</p>
     *
     * <p>To work-around some tar implementations that insert a leading NUL this method returns 0 if
     * it detects a leading NUL</p>
     *
     * @param buffer ByteBuffer.
     * @param length The maximum number of bytes to parse - must be at least 2 bytes.
     * @return The long value of the octal string.
     * @throws IllegalArgumentException if an invalid byte is detected.
     */

    static long parseOctal(ByteBuffer buffer, final int length) {
        int saveLimit = buffer.limit();
        int end = buffer.position() + length;
        buffer.limit(end);
        long result;
        byte c = buffer.get(buffer.position());
        if (c == 0) {
            return 0L;
        }
        while (buffer.hasRemaining()) {
            c = buffer.get();
            if (c != ' ') {
                break;
            }
        }
        if (!isOctalDigit(c)) {
            throw new IllegalArgumentException("expected octal numeral, not " + (char) c);
        }
        result = (c - '0');

        while (buffer.hasRemaining()) {
            c = buffer.get();
            if (!isOctalDigit(c)) {
                break;
            }
            result = result * 8 + (c - '0');
        }
        if (c != ' ' && c != 0) {
            throw new IllegalArgumentException("Expected space or null at end of octal string");
        }
        buffer.limit(saveLimit);
        buffer.position(end);
        return result;
    }

    private static boolean isOctalDigit(byte c) {
        return c >= '0' && c <= '7';
    }

    /**
     * Compute the value contained in a byte buffer.  If the most significant bit of the first byte
     * in the buffer is set, this bit is ignored and the rest of the buffer is interpreted as a
     * binary number.  Otherwise, the buffer is interpreted as an octal number as per the parseOctal
     * function above.
     *
     * @param buffer The buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The maximum number of bytes to parse.
     * @return The long value of the octal or binary string.
     * @throws IllegalArgumentException if the trailing space/NUL is missing or an invalid byte is
     * detected in an octal number, or if a binary number would exceed the size of a signed long
     * 64-bit integer.
     * @since 1.4
     */
    public static long parseOctalOrBinary(final byte[] buffer, final int offset,
        final int length) {
        if (shouldDecodeAsBase256(buffer[offset])) {
            return parseBase256(buffer, offset, length);
        } else {
            return parseOctal(buffer, offset, length);
        }
    }

    static long parseOctalOrBinary(ByteBuffer buffer,
        final int length) {
        if (shouldDecodeAsBase256(buffer.get(buffer.position()))) {
            return parseBase256(buffer, length);
        } else {
            return parseOctal(buffer, length);
        }
    }

    /**
     * Checks first byte of a numeric header field to see if the value should be decoded as base256
     * instead of Octal numerals.
     */
    private static boolean shouldDecodeAsBase256(byte b) {
        return (b & 0x80) != 0;
    }

    /**
     * decode a base256 numeric header field to long. The value is assumed to fit in a signed 64 bit
     * long. For fields larger than 8 bytes, the value is assumed to have been correctly sign
     * extended, such that for positive values the first byte is 0x80, and the remaining padding is
     * 0x00, and for negative values, all padding bytes are 0xff.
     */
    private static long parseBase256(byte[] buffer, int offset, int length) {
        long result = 0L;
        byte b = buffer[offset];
        if ((b & 0x40) == 0) {
            b = (byte) (b & ~0x80);
        } else {
            result = -1L;
            b |= (byte) 0x80;
        }
        result |= unsigned(b);
        for (int i = 1; i < length; i++) {
            b = buffer[offset + i];
            result <<= 8;
            result |= unsigned(buffer[offset + i]);
        }
        return result;
    }

    private static long parseBase256(ByteBuffer buffer, int length) {
        long result = 0L;
        byte b = buffer.get();
        if ((b & 0x40) == 0) {
            b = (byte) (b & ~0x80);
        } else {
            result = -1L;
            b |= (byte) 0x80;
        }
        result |= unsigned(b);
        for (int i = 1; i < length; i++) {
            result <<= 8;
            result |= unsigned(buffer.get());
        }
        return result;

    }


    /**
     * Parse a boolean byte from a buffer. Leading spaces and NUL are ignored. The buffer may
     * contain trailing spaces or NULs.
     *
     * @param buffer The buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @return The boolean value of the bytes.
     * @throws IllegalArgumentException if an invalid byte is detected.
     */
    public static boolean parseBoolean(final byte[] buffer, final int offset) {
        return buffer[offset] == 1;
    }
    
     static boolean parseBoolean(ByteBuffer buffer) {
        return buffer.get() == 1;
    }

    // Helper method to generate the exception message
    private static String exceptionMessage(final byte[] buffer, final int offset,
        final int length, final int current, final byte currentByte) {
        // default charset is good enough for an exception message,
        //
        // the alternative was to modify parseOctal and
        // parseOctalOrBinary to receive the ZipEncoding of the
        // archive (deprecating the existing public methods, of
        // course) and dealing with the fact that ZipEncoding#decode
        // can throw an IOException which parseOctal* doesn't declare
        String string = new String(buffer, offset, length);

        string = string.replaceAll("\0", "{NUL}"); // Replace NULs to allow string to be printed
        final String s =
            "Invalid byte " + currentByte + " at offset " + (current - offset) + " in '" + string
                + "' len=" + length;
        return s;
    }

    /**
     * Parse an entry name from a buffer. Parsing stops when a NUL is found or the buffer length is
     * reached.
     *
     * @param buffer The buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The maximum number of bytes to parse.
     * @return The entry name.
     */
    public static String parseName(final byte[] buffer, final int offset, final int length) {
        try {
            return parseName(buffer, offset, length, DEFAULT_ENCODING);
        } catch (final IOException ex) {
            try {
                return parseName(buffer, offset, length, FALLBACK_ENCODING);
            } catch (final IOException ex2) {
                // impossible
                throw new RuntimeException(ex2); //NOSONAR
            }
        }
    }
    public static String parseName(ByteBuffer buffer, int namelen) {
        try {
            return parseName(buffer, namelen, ZipEncodingHelper.getZipEncoding(null));
        } catch (final IOException ex) {
            try {
                return parseName(buffer,  namelen, ZipEncodingHelper.getZipEncoding("ISO-8859-1"));
            } catch (final IOException ex2) {
                // impossible
                throw new RuntimeException(ex2); //NOSONAR
            }
        }
    }

    /**
     * Parse an entry name from a buffer. Parsing stops when a NUL is found or the buffer length is
     * reached.
     *
     * @param buffer The buffer from which to parse.
     * @param offset The offset into the buffer from which to parse.
     * @param length The maximum number of bytes to parse.
     * @param encoding name of the encoding to use for file names
     * @return The entry name.
     * @throws IOException on error
     * @since 1.4
     */
    public static String parseName(final byte[] buffer, final int offset,
        final int length,
        final ZipEncoding encoding) throws IOException {

        int len = length;
        for (; len > 0; len--) {
            if (buffer[offset + len - 1] != 0) {
                break;
            }
        }
        if (len > 0) {
            final byte[] b = new byte[len];
            System.arraycopy(buffer, offset, b, 0, len);
            return encoding.decode(b);
        }
        return "";
    }

    public static String parseName(ByteBuffer buffer, final int length, final ZipEncoding encoding)
        throws IOException {
        Charset cs = ((HasCharset) encoding).getCharset();
        CharsetDecoder decoder = cs.newDecoder();
        int limit = buffer.limit();
        int savedLimit = limit;
        int position = buffer.position();
        int end = position + length;
        buffer.limit(end);
        limit = end;
        while (limit > position && buffer.get(--limit) == 0) {
            buffer.limit(limit);
        }
        CharBuffer decode = decoder.decode(buffer);
        buffer.limit(savedLimit).position(end);
        return decode.toString();
    }


    /**
     * Copy a name into a buffer. Copies characters from the name into the buffer starting at the
     * specified offset. If the buffer is longer than the name, the buffer is filled with trailing
     * NULs. If the name is longer than the buffer, the output is truncated.
     *
     * @param name The header name from which to copy the characters.
     * @param buf The buffer where the name is to be stored.
     * @param offset The starting offset into the buffer
     * @param length The maximum number of header bytes to copy.
     * @return The updated offset, i.e. offset + length
     */
    public static int formatNameBytes(final String name, final byte[] buf, final int offset,
        final int length) {
        try {
            return formatNameBytes(name, buf, offset, length, DEFAULT_ENCODING);
        } catch (final IOException ex) {
            try {
                return formatNameBytes(name, buf, offset, length,
                    FALLBACK_ENCODING);
            } catch (final IOException ex2) {
                // impossible
                throw new RuntimeException(ex2); //NOSONAR
            }
        }
    }

    /**
     * Copy a name into a buffer. Copies characters from the name into the buffer starting at the
     * specified offset. If the buffer is longer than the name, the buffer is filled with trailing
     * NULs. If the name is longer than the buffer, the output is truncated.
     *
     * @param name The header name from which to copy the characters.
     * @param buf The buffer where the name is to be stored.
     * @param offset The starting offset into the buffer
     * @param length The maximum number of header bytes to copy.
     * @param encoding name of the encoding to use for file names
     * @return The updated offset, i.e. offset + length
     * @throws IOException on error
     * @since 1.4
     */
    public static int formatNameBytes(final String name, final byte[] buf, final int offset,
        final int length,
        final ZipEncoding encoding)
        throws IOException {
        int len = name.length();
        ByteBuffer b = encoding.encode(name);
        while (b.limit() > length && len > 0) {
            b = encoding.encode(name.substring(0, --len));
        }
        final int limit = b.limit() - b.position();
        System.arraycopy(b.array(), b.arrayOffset(), buf, offset, limit);

        // Pad any remaining output bytes with NUL
        for (int i = limit; i < length; ++i) {
            buf[offset + i] = 0;
        }

        return offset + length;
    }

    /**
     * Append name to the supplied buffer, using the system default character set. <ul> <li>If the
     * length is less than len bytes, the remaining space will be null padded. <li>If the length is
     * exactly len bytes, no padding will be added. <li>If the encoded name exceeds len bytes, then
     * append as many complete characters as will fit. Any remaining space will be null padded
     * </ul>
     *
     * @param name Name to append
     * @param buffer ByteBuffer to append.  Buffer must have at least len bytes remaining.
     * @param len Space to be filled by encoded name
     */

    static void formatNameBytes(String name, ByteBuffer buffer, int len)
        throws IOException {
        formatNameBytes(name, buffer, len, null);
    }
    /**
     * Append name to the supplied buffer, using the supplied encoder.
     * <ul>
     *     <li>If the length is less than len bytes, the remaining space will be null padded.
     *     <li>If the length is  exactly len bytes, no padding will be added.
     *     <li>If the encoded name exceeds len bytes, then append as many complete characters
     *         as will fit. Any remaining space will be null padded
     * </ul>
     *
     * @param name Name to append
     * @param buffer ByteBuffer to append.  Buffer must have at least len bytes remaining.
     * @param len Space to be filled by encoded name
     * @param encoder Characterset encoder to use.  The encoder will be reset before use.
     */

    static void formatNameBytes(String name, ByteBuffer buffer, CharsetEncoder encoder, int len) {
        int savedLimit = buffer.limit();
        buffer.limit(buffer.position() + len);
        formatNameBytes(name,buffer,encoder);
        buffer.limit(savedLimit);

    }


    /**
     * Append name to the supplied buffer.
     * <ul>
     *     <li>If the length is less than len bytes, the  remaining space will be null padded.
     *     <li>If the length is exactly len bytes, no padding will  be added.
     *     <li>If the encoded name exceeds len bytes, then append as many complete characters
     *         as will fit. Any remaining space will be null padded
     *     </ul>
     *  If the supplied ZipEncoding implements HasCharset, an NIO CharsetEncoder will be created
     *  and used directly instead of  delegating to the ZipEncoding.
     *
     * @param name Name to append
     * @param buffer ByteBuffer to append.  Buffer must have at least len bytes remaining.
     * @param len Space to be filled by encoded name
     * @param encoding Zip Encoding to use for encoding, or null to use system default.
     */
    static void formatNameBytes(String name, ByteBuffer buffer, int len, ZipEncoding encoding)
        throws IOException {
        int savedLimit = buffer.limit();
        buffer.limit(buffer.position() + len);

        try {
            if (encoding == null || encoding instanceof HasCharset) {
                Charset charset;
                if (encoding != null) {
                    charset = ((HasCharset) encoding).getCharset();
                } else {
                    charset = Charset.defaultCharset();
                }
                formatNameBytes(name, buffer, charset);
            } else {
                byte tmp[] = new byte[len];
                formatNameBytes(name, tmp, 0, len);
                buffer.put(tmp);
            }
        } finally {
            buffer.limit(savedLimit);
        }
    }

    static void formatNameBytes(String name, ByteBuffer buffer, Charset charset) {
        CharsetEncoder encoder = newEncoder(charset);
        formatNameBytes(name, buffer, encoder);
    }

    private static CharsetEncoder newEncoder(Charset charset) {
        return charset.newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }


    static void formatNameBytes(String name, ByteBuffer buffer, CharsetEncoder encoder) {
        CharBuffer src = CharBuffer.wrap(name);
        CoderResult coderResult = encoder.encode(src, buffer, true);

        if (coderResult.isOverflow() || coderResult.isUnderflow()) {
            padBuffer(buffer);
        } else {
            throw new IllegalStateException(
                String.format("Unexpected coder result: %s", coderResult));
        }
    }

    private static void padBuffer(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }
    }

    /**
     * Fill buffer with unsigned octal number, padded with leading zeroes.
     *
     * @param value number to convert to octal - treated as unsigned
     * @param buffer destination buffer
     * @param offset starting offset in buffer
     * @param length length of buffer to fill
     * @throws IllegalArgumentException if the value will not fit in the buffer
     */
    public static void formatUnsignedOctalString(final long value, final byte[] buffer,
        final int offset, final int length) {
        int remaining = length;
        remaining--;
        if (value == 0) {
            buffer[offset + remaining--] = (byte) '0';
        } else {
            long val = value;
            for (; remaining >= 0 && val != 0; --remaining) {
                // CheckStyle:MagicNumber OFF
                buffer[offset + remaining] = (byte) ((byte) '0' + (byte) (val & 7));
                val = val >>> 3;
                // CheckStyle:MagicNumber ON
            }
            if (val != 0) {
                throw new IllegalArgumentException
                    (value + "=" + Long.toOctalString(value)
                        + " will not fit in octal number buffer of length " + length);
            }
        }

        for (; remaining >= 0; --remaining) { // leading zeros
            buffer[offset + remaining] = (byte) '0';
        }
    }

    private static void formatUnsignedOctalString(long value, ByteBuffer buffer, int length) {
        int start = buffer.position();
        int end = start + length;
        for (int i = end - 1; i >= start; i--) {
            buffer.put(i, (byte) ((value & 7) + '0'));
            value >>>= 3;
        }
        if (value != 0) {
            throw new IllegalArgumentException(value + "=" + Long.toOctalString(value)
                + " will not fit in octal number buffer of length " + length);
        }
        buffer.position(end);
    }


    /**
     * Write an octal integer into a buffer.
     *
     * Uses {@link #formatUnsignedOctalString} to format the value as an octal string with leading
     * zeros. The converted number is followed by space and NUL
     *
     * @param value The value to write
     * @param buf The buffer to receive the output
     * @param offset The starting offset into the buffer
     * @param length The size of the output buffer
     * @return The updated offset, i.e offset+length
     * @throws IllegalArgumentException if the value (and trailer) will not fit in the buffer
     */
    public static int formatOctalBytes(final long value, final byte[] buf, final int offset,
        final int length) {

        int idx = length - 2; // For space and trailing null
        formatUnsignedOctalString(value, buf, offset, idx);

        buf[offset + idx++] = (byte) ' '; // Trailing space
        buf[offset + idx] = 0; // Trailing null

        return offset + length;
    }

    /**
     * Write an octal long integer into a buffer.
     *
     * Uses {@link #formatUnsignedOctalString} to format the value as an octal string with leading
     * zeros. The converted number is followed by a space.
     *
     * @param value The value to write as octal
     * @param buf The destinationbuffer.
     * @param offset The starting offset into the buffer.
     * @param length The length of the buffer
     * @return The updated offset
     * @throws IllegalArgumentException if the value (and trailer) will not fit in the buffer
     */
    public static int formatLongOctalBytes(final long value, final byte[] buf, final int offset,
        final int length) {

        final int idx = length - 1; // For space

        formatUnsignedOctalString(value, buf, offset, idx);
        buf[offset + idx] = (byte) ' '; // Trailing space

        return offset + length;
    }

    static void formatLongOctalBytes(long value, ByteBuffer buffer, int length) {
        int savedLimit = buffer.limit();
        int end = buffer.position() + length - 1;
        buffer.limit(end);
        formatUnsignedOctalString(value, buffer, length - 1);
        buffer.limit(savedLimit);
        buffer.position(end);
        buffer.put((byte) ' ');
    }


    /**
     * Write an long integer into a buffer as an octal string if this will fit, or as a binary
     * number otherwise.
     *
     * Uses {@link #formatUnsignedOctalString} to format the value as an octal string with leading
     * zeros. The converted number is followed by a space.
     *
     * @param value The value to write into the buffer.
     * @param buf The destination buffer.
     * @param offset The starting offset into the buffer.
     * @param length The length of the buffer.
     * @return The updated offset.
     * @throws IllegalArgumentException if the value (and trailer) will not fit in the buffer.
     * @since 1.4
     */
    public static int formatLongOctalOrBinaryBytes(
        final long value, final byte[] buf, final int offset, final int length) {

        long bits = (length - 1) * 3; // 3 bits per char
        long maxAsUnsignedOctalChars = (1L << (bits)) - 1;

        final boolean negative = value < 0;
        if (!negative && value <= maxAsUnsignedOctalChars) { // OK to store as octal chars
            return formatLongOctalBytes(value, buf, offset, length);
        } else {
            return formatBase256(value, buf, offset, length, negative);
        }
    }

    private static int formatBase256(long value, byte[] buf, int offset, int length,
        boolean negative) {
        int bits = length * 8 - 1;
        int end = offset + length;
        if (length < 9) {
            long l = 1L << (bits - 1);
            long min = -l;
            long max = l - 1;
            if (value < min || value > max) {
                throw new IllegalArgumentException(String
                    .format("Can't fit %,d (%016x) in %d bits (min %,d, max %,d)", value, value,
                        bits, min, max));
            }
            for (int i = end - 1; i >= offset; i--) {
                buf[i] = (byte) (value & 0xff);
                value >>>= 8;
            }
            buf[offset] |= 0x80;
        } else {
            byte pad;
            if (negative) {
                buf[offset] = (byte) 0xff;
                pad = (byte) 0xff;
            } else {
                buf[offset] = (byte) 0x80;
                pad = 0;
            }
            int padlen = length - 8;
            for (int i = 1; i < padlen; i++) {
                buf[offset + i] = pad;
            }
            for (int i = length - 1; i >= padlen; i--) {
                buf[offset + i] = (byte) (value & 0xff);
                value >>>= 8;
            }
        }

        return end;
    }


    static void formatLongOctalOrBinaryBytes(
        final long value, final ByteBuffer buffer, final int length) {
        long bits = (length - 1) * 3; // 3 bits per char
        long maxAsUnsignedOctalChars = (1L << (bits)) - 1;
        final boolean negative = value < 0;
        if (!negative && value <= maxAsUnsignedOctalChars) { // OK to store as octal chars
            formatLongOctalBytes(value, buffer, length);
        } else {
            formatBase256(value, buffer, length);
        }
    }

    static void formatBase256(long value, ByteBuffer buffer, int length) {
        int bits = length * 8 - 1;
        int pos = buffer.position();
        if (length < 9) {
            long l = 1L << (bits - 1);
            long min = -l;
            long max = l - 1;
            if (value < min || value > max) {
                throw new IllegalArgumentException(String
                    .format("Can't fit %,d (%016x) in %d bits (min %,d, max %,d)", value,
                        value,
                        bits, min, max));
            }
            for (int i = length - 1; i > 0; i--) {
                buffer.put(pos + i, (byte) (value & 0xff));
                value >>>= 8;
            }
            buffer.put(pos, (byte) ((value & 0xff) | 0x80));
            buffer.position(pos + length);
        } else {
            byte pad;
            if (value < 0) {
                buffer.put((byte) 0xff);
                pad = (byte) 0xff;
            } else {
                buffer.put((byte) 0x80);
                pad = 0;
            }
            int padlen = length - 8;
            for (int i = 1; i < padlen; i++) {
                buffer.put(pad);
            }
            ByteOrder savedOrder = buffer.order();
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(value);
            buffer.order(savedOrder);
        }
    }


    /**
     * Writes an octal value into a buffer.
     *
     * Uses {@link #formatUnsignedOctalString} to format the value as an octal string with leading
     * zeros. The converted number is followed by NUL and then space.
     *
     * @param value The value to convert
     * @param buf The destination buffer
     * @param offset The starting offset into the buffer.
     * @param length The size of the buffer.
     * @return The updated value of offset, i.e. offset+length
     * @throws IllegalArgumentException if the value (and trailer) will not fit in the buffer
     */
    public static int formatCheckSumOctalBytes(final long value, final byte[] buf, final int offset,
        final int length) {

        int idx = length - 2; // for NUL and space
        formatUnsignedOctalString(value, buf, offset, idx);

        buf[offset + idx++] = 0; // Trailing null
        buf[offset + idx] = (byte) ' '; // Trailing space

        return offset + length;
    }


    static void formatCheckSumOctalBytes(final long value, ByteBuffer buffer, final int offset,
        final int length) {

        int idx = length - 2; // for NUL and space
        buffer.position(offset);
        formatUnsignedOctalString(value, buffer, idx);

        buffer.put((byte) 0); // Trailing null
        buffer.put((byte) ' '); // Trailing space
        buffer.position(buffer.limit());
    }

    /**
     * Compute the checksum of a tar entry header.
     *
     * @param buf The tar entry's header buffer.
     * @return The computed checksum.
     */
    public static long computeCheckSum(final byte[] buf) {
        long sum = 0;

        for (final byte element : buf) {
            sum += BYTE_MASK & element;
        }

        return sum;
    }

    public static long computeCheckSum(final ByteBuffer buffer) {
        int savedPosition = buffer.position();
        int savedLimit = buffer.limit();
        long sum = 0L;
        buffer.position(0);
        buffer.limit(CHKSUM_OFFSET);
        while (buffer.hasRemaining()) {
            sum += unsigned(buffer.get());
        }
        for(int i=0;i<CHKSUMLEN;i++) {
            sum += 0x20;
        }
        buffer.limit(savedLimit).position(CHKSUM_OFFSET + CHKSUMLEN);
        while(buffer.hasRemaining()) {
            sum += unsigned (buffer.get());
        }
        buffer.position(savedPosition);
        return sum;
    }

    /**
     * Wikipedia <a href="http://en.wikipedia.org/wiki/Tar_(file_format)#File_header">says</a>:
     * <blockquote> The checksum is calculated by taking the sum of the unsigned byte values of the
     * header block with the eight checksum bytes taken to be ascii spaces (decimal value 32). It is
     * stored as a six digit octal number with leading zeroes followed by a NUL and then a space.
     * Various implementations do not adhere to this format. For better compatibility, ignore
     * leading and trailing whitespace, and get the first six digits. In addition, some historic tar
     * implementations treated bytes as signed. Implementations typically calculate the checksum
     * both ways, and treat it as good if either the signed or unsigned sum matches the included
     * checksum. </blockquote> <p> The return value of this method should be treated as a
     * best-effort heuristic rather than an absolute and final truth. The checksum verification
     * logic may well evolve over time as more special cases are encountered.
     *
     * @param header tar header
     * @return whether the checksum is reasonably good
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-191">COMPRESS-191</a>
     * @since 1.5
     */
    public static boolean verifyCheckSum(final byte[] header) {
        final long storedSum = parseOctal(header, CHKSUM_OFFSET, CHKSUMLEN);
        long unsignedSum = 0;
        long signedSum = 0;

        for (int i = 0; i < header.length; i++) {
            byte b = header[i];
            if (CHKSUM_OFFSET <= i && i < CHKSUM_OFFSET + CHKSUMLEN) {
                b = ' ';
            }
            unsignedSum += unsigned(b);
            signedSum += b;
        }
        return storedSum == unsignedSum || storedSum == signedSum;
    }

    private static long unsigned(byte b) {
        return b & 0xff;
    }

    static boolean verifyCheckSum(ByteBuffer buffer) {
        int pos = buffer.position();
        buffer.position(CHKSUM_OFFSET);
        final long storedSum = parseOctal(buffer, CHKSUMLEN);
        long unsignedSum = 0;
        long signedSum = 0;
        buffer.position(0);
        for (int i = 0; i < CHKSUM_OFFSET; i++) {
            byte b = buffer.get();
            unsignedSum += unsigned(b);
            signedSum += b;
        }
        for (int i = 0; i < CHKSUMLEN; i++) {
            unsignedSum += 0x20;
            signedSum += 0x20;
        }
        buffer.position(CHKSUM_OFFSET + CHKSUMLEN);
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            unsignedSum += unsigned(b);
            signedSum += b;
        }
        buffer.position(pos);
        return storedSum == unsignedSum || storedSum == signedSum;
    }

}

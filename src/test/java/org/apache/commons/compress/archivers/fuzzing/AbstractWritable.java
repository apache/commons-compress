package org.apache.commons.compress.archivers.fuzzing;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public abstract class AbstractWritable {

    public abstract int getRecordSize();

    public abstract void writeTo(ByteBuffer buffer);

    protected void writeOctalString(ByteBuffer buffer, long value, int length) {
        final byte[] bytes = Long.toOctalString(value).getBytes(US_ASCII);
        if (bytes.length > length) {
            throw new IllegalArgumentException(
                    "Value " + value + " is too large to fit in " + length + " octal digits");
        }
        buffer.put(bytes);
        pad(buffer, bytes.length, length, (byte) ' ');
    }

    protected void writeString(ByteBuffer buffer, String value, Charset charset, int length) {
        final byte[] bytes = value.getBytes(charset);
        if (bytes.length > length) {
            throw new IllegalArgumentException(
                    "String \"" + value + "\" is too long to fit in " + length + " bytes");
        }
        buffer.put(bytes);
        pad(buffer, bytes.length, length, (byte) 0);
    }

    protected void pad(ByteBuffer buffer, int written, int length, byte padByte) {
        while (written % length != 0) {
            buffer.put(padByte);
            written++;
        }
    }
}

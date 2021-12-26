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

package org.apache.commons.compress.archivers.zip;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.*;

public class NioZipEncodingTest {

    private static final String UMLAUTS = "\u00e4\u00f6\u00fc";

    @Test
    public void umlautToUTF16BE() {
        final NioZipEncoding e = new NioZipEncoding(UTF_16BE, false);
        final ByteBuffer bb = e.encode(UMLAUTS);
        final int off = bb.arrayOffset();
        final byte[] result = Arrays.copyOfRange(bb.array(), off, off + bb.limit() - bb.position());
        Assert.assertArrayEquals(UMLAUTS.getBytes(UTF_16BE), result);
    }

    @Test
    public void umlautToUTF8() {
        final NioZipEncoding e = new NioZipEncoding(UTF_8, true);
        final ByteBuffer bb = e.encode("\u00e4\u00f6\u00fc");
        final int off = bb.arrayOffset();
        final byte[] result = Arrays.copyOfRange(bb.array(), off, off + bb.limit() - bb.position());
        Assert.assertArrayEquals(UMLAUTS.getBytes(UTF_8), result);
    }

    @Test
    public void umlautToISO88591() {
        final NioZipEncoding e = new NioZipEncoding(ISO_8859_1, true);
        final ByteBuffer bb = e.encode("\u00e4\u00f6\u00fc");
        final int off = bb.arrayOffset();
        final byte[] result = Arrays.copyOfRange(bb.array(), off, off + bb.limit() - bb.position());
        Assert.assertArrayEquals(UMLAUTS.getBytes(ISO_8859_1), result);
    }

    @Test
    public void unmappableUmlauts() {
        final NioZipEncoding e = new NioZipEncoding(US_ASCII, false);
        final ByteBuffer bb = e.encode("\u00e4\u00f6\u00fc");
        final int off = bb.arrayOffset();
        final byte[] result = Arrays.copyOfRange(bb.array(), off, off + bb.limit() - bb.position());
        Assert.assertEquals("%U00E4%U00F6%U00FC", new String(result, US_ASCII));
    }

    private static final String RAINBOW_EMOJI = "\ud83c\udf08";

    @Test
    public void unmappableRainbowEmoji() {
        final NioZipEncoding e = new NioZipEncoding(US_ASCII, false);
        final ByteBuffer bb = e.encode(RAINBOW_EMOJI);
        final int off = bb.arrayOffset();
        final byte[] result = Arrays.copyOfRange(bb.array(), off, off + bb.limit() - bb.position());
        Assert.assertEquals("%UD83C%UDF08", new String(result, US_ASCII));
    }

    @Test
    public void rainbowEmojiToSurrogatePairUTF16() {
        final NioZipEncoding e = new NioZipEncoding(UTF_16BE, false);
        final ByteBuffer bb = e.encode(RAINBOW_EMOJI);
        final int off = bb.arrayOffset();
        final byte[] result = Arrays.copyOfRange(bb.array(), off, off + bb.limit() - bb.position());
        Assert.assertArrayEquals(RAINBOW_EMOJI.getBytes(UTF_16BE), result);
    }

    @Test
    public void partialSurrogatePair() {
        final NioZipEncoding e = new NioZipEncoding(US_ASCII, false);
        final ByteBuffer bb = e.encode("\ud83c");
        final int off = bb.arrayOffset();
        final byte[] result = Arrays.copyOfRange(bb.array(), off, off + bb.limit() - bb.position());
        Assert.assertEquals(0, result.length);
    }
}

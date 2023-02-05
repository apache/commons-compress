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

package org.apache.commons.compress.archivers.tar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.CharsetNames;
import org.junit.jupiter.api.Test;

public class TarUtilsTest extends AbstractTestCase {

    private void checkName(final String string) {
        final byte[] buff = new byte[100];
        final int len = TarUtils.formatNameBytes(string, buff, 0, buff.length);
        assertEquals(string, TarUtils.parseName(buff, 0, len));
    }

    private void checkRoundTripOctal(final long value) {
        checkRoundTripOctal(value, TarConstants.SIZELEN);
    }

    private void checkRoundTripOctal(final long value, final int bufsize) {
        final byte [] buffer = new byte[bufsize];
        long parseValue;
        TarUtils.formatLongOctalBytes(value, buffer, 0, buffer.length);
        parseValue = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(value,parseValue);
    }

    private void checkRoundTripOctalOrBinary(final long value, final int bufsize) {
        final byte [] buffer = new byte[bufsize];
        long parseValue;
        TarUtils.formatLongOctalOrBinaryBytes(value, buffer, 0, buffer.length);
        parseValue = TarUtils.parseOctalOrBinary(buffer,0, buffer.length);
        assertEquals(value,parseValue);
    }

    @Test
    public void parseFromPAX01SparseHeaders() throws Exception {
        final String map = "0,10,20,0,20,5";
        final List<TarArchiveStructSparse> sparse = TarUtils.parseFromPAX01SparseHeaders(map);
        assertEquals(3, sparse.size());
        assertEquals(0, sparse.get(0).getOffset());
        assertEquals(10, sparse.get(0).getNumbytes());
        assertEquals(20, sparse.get(1).getOffset());
        assertEquals(0, sparse.get(1).getNumbytes());
        assertEquals(20, sparse.get(2).getOffset());
        assertEquals(5, sparse.get(2).getNumbytes());
    }

    @Test
    public void parseFromPAX01SparseHeadersRejectsNegativeNumbytes() throws Exception {
        assertThrows(IOException.class, () -> TarUtils.parseFromPAX01SparseHeaders("0,10,20,0,20,-5"));
    }

    @Test
    public void parseFromPAX01SparseHeadersRejectsNegativeOffset() throws Exception {
        assertThrows(IOException.class, () -> TarUtils.parseFromPAX01SparseHeaders("0,10,20,0,-2,5"));
    }

    @Test
    public void parseFromPAX01SparseHeadersRejectsNonNumericNumbytes() throws Exception {
        assertThrows(IOException.class, () -> TarUtils.parseFromPAX01SparseHeaders("0,10,20,0,20,b"));
    }

    @Test
    public void parseFromPAX01SparseHeadersRejectsNonNumericOffset() throws Exception {
        assertThrows(IOException.class, () -> TarUtils.parseFromPAX01SparseHeaders("0,10,20,0,2a,5"));
    }

    @Test
    public void parseFromPAX01SparseHeadersRejectsOddNumberOfEntries() throws Exception {
        final String map = "0,10,20,0,20";
        assertThrows(IOException.class, () -> TarUtils.parseFromPAX01SparseHeaders(map));
    }

    @Test
    public void parsePAX01SparseHeadersRejectsOddNumberOfEntries() {
        final String map = "0,10,20,0,20";
        assertThrows(UncheckedIOException.class, () -> TarUtils.parsePAX01SparseHeaders(map));
    }

    @Test
    public void parsePAX1XSparseHeaders() throws Exception {
        final byte[] header = ("1\n"
                + "0\n"
                + "20\n")
            .getBytes();
        final byte[] block = new byte[512];
        System.arraycopy(header, 0, block, 0, header.length);
        try (ByteArrayInputStream in = new ByteArrayInputStream(block)) {
            final List<TarArchiveStructSparse> sparse = TarUtils.parsePAX1XSparseHeaders(in, 512);
            assertEquals(1, sparse.size());
            assertEquals(0, sparse.get(0).getOffset());
            assertEquals(20, sparse.get(0).getNumbytes());
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void parsePAX1XSparseHeadersRejectsIncompleteLastLine() throws Exception {
        final byte[] header = ("1\n"
                + "0\n"
                + "20")
            .getBytes();
        try (ByteArrayInputStream in = new ByteArrayInputStream(header)) {
            assertThrows(IOException.class, () -> TarUtils.parsePAX1XSparseHeaders(in, 512));
        }
    }

    @Test
    public void parsePAX1XSparseHeadersRejectsNegativeNumberOfEntries() throws Exception {
        final byte[] header = ("111111111111111111111111111111111111111111111111111111111111111\n"
                + "0\n"
                + "20\n")
            .getBytes();
        final byte[] block = new byte[512];
        System.arraycopy(header, 0, block, 0, header.length);
        try (ByteArrayInputStream in = new ByteArrayInputStream(block)) {
            assertThrows(IOException.class, () -> TarUtils.parsePAX1XSparseHeaders(in, 512));
        }
    }

    @Test
    public void parsePAX1XSparseHeadersRejectsNegativeNumbytes() throws Exception {
        final byte[] header = ("1\n"
                + "0\n"
                + "111111111111111111111111111111111111111111111111111111111111111\n")
            .getBytes();
        final byte[] block = new byte[512];
        System.arraycopy(header, 0, block, 0, header.length);
        try (ByteArrayInputStream in = new ByteArrayInputStream(block)) {
            assertThrows(IOException.class, () -> TarUtils.parsePAX1XSparseHeaders(in, 512));
        }
    }

    @Test
    public void parsePAX1XSparseHeadersRejectsNegativeOffset() throws Exception {
        final byte[] header = ("1\n"
                + "111111111111111111111111111111111111111111111111111111111111111\n"
                + "20\n")
            .getBytes();
        final byte[] block = new byte[512];
        System.arraycopy(header, 0, block, 0, header.length);
        try (ByteArrayInputStream in = new ByteArrayInputStream(block)) {
            assertThrows(IOException.class, () -> TarUtils.parsePAX1XSparseHeaders(in, 512));
        }
    }

    @Test
    public void parsePAX1XSparseHeadersRejectsNonNumericNumberOfEntries() throws Exception {
        final byte[] header = ("x\n"
                + "0\n"
                + "20\n")
            .getBytes();
        final byte[] block = new byte[512];
        System.arraycopy(header, 0, block, 0, header.length);
        try (ByteArrayInputStream in = new ByteArrayInputStream(block)) {
            assertThrows(IOException.class, () -> TarUtils.parsePAX1XSparseHeaders(in, 512));
        }
    }

    @Test
    public void parsePAX1XSparseHeadersRejectsNonNumericNumbytes() throws Exception {
        final byte[] header = ("1\n"
                + "0\n"
                + "2x\n")
            .getBytes();
        final byte[] block = new byte[512];
        System.arraycopy(header, 0, block, 0, header.length);
        try (ByteArrayInputStream in = new ByteArrayInputStream(block)) {
            assertThrows(IOException.class, () -> TarUtils.parsePAX1XSparseHeaders(in, 512));
        }
    }


    @Test
    public void parsePAX1XSparseHeadersRejectsNonNumericOffset() throws Exception {
        final byte[] header = ("1\n"
                + "x\n"
                + "20\n")
            .getBytes();
        final byte[] block = new byte[512];
        System.arraycopy(header, 0, block, 0, header.length);
        try (ByteArrayInputStream in = new ByteArrayInputStream(block)) {
            assertThrows(IOException.class, () -> TarUtils.parsePAX1XSparseHeaders(in, 512));
        }
    }

    @Test
    public void paxHeaderEntryWithEmptyValueRemovesKey() throws Exception {
        final Map<String, String> headers = TarUtils
                .parsePaxHeaders(new ByteArrayInputStream("11 foo=bar\n7 foo=\n"
                        .getBytes(UTF_8)), null, new HashMap<>());
        assertEquals(0, headers.size());
    }

    @Test
    public void readNonAsciiPaxHeader() throws Exception {
        final String ae = "\u00e4";
        final String line = "11 path="+ ae + "\n";
        assertEquals(11, line.getBytes(UTF_8).length);
        final Map<String, String> headers = TarUtils
                .parsePaxHeaders(new ByteArrayInputStream(line.getBytes(UTF_8)), null, new HashMap<>());
        assertEquals(1, headers.size());
        assertEquals(ae, headers.get("path"));
    }

    @Test
    public void readPax00SparseHeader() throws Exception {
        final String header = "23 GNU.sparse.offset=0\n26 GNU.sparse.numbytes=10\n";
        final List<TarArchiveStructSparse> sparseHeaders = new ArrayList<>();
        TarUtils.parsePaxHeaders(
            new ByteArrayInputStream(header.getBytes(UTF_8)),
            sparseHeaders, Collections.emptyMap());
        assertEquals(1, sparseHeaders.size());
        assertEquals(0, sparseHeaders.get(0).getOffset());
        assertEquals(10, sparseHeaders.get(0).getNumbytes());
    }

    @Test
    public void readPax00SparseHeaderMakesNumbytesOptional() throws Exception {
        final String header = "23 GNU.sparse.offset=0\n24 GNU.sparse.offset=10\n";
        final List<TarArchiveStructSparse> sparseHeaders = new ArrayList<>();
        TarUtils.parsePaxHeaders(
            new ByteArrayInputStream(header.getBytes(UTF_8)),
            sparseHeaders, Collections.emptyMap());
        assertEquals(2, sparseHeaders.size());
        assertEquals(0, sparseHeaders.get(0).getOffset());
        assertEquals(0, sparseHeaders.get(0).getNumbytes());
        assertEquals(10, sparseHeaders.get(1).getOffset());
        assertEquals(0, sparseHeaders.get(1).getNumbytes());
    }

    @Test
    public void readPax00SparseHeaderRejectsNegativeNumbytes() throws Exception {
        final String header = "23 GNU.sparse.offset=0\n26 GNU.sparse.numbytes=-1\n";
        assertThrows(IOException.class, () -> TarUtils.parsePaxHeaders(
            new ByteArrayInputStream(header.getBytes(UTF_8)),
            null, Collections.emptyMap()));
    }

    @Test
    public void readPax00SparseHeaderRejectsNegativeOffset() throws Exception {
        final String header = "24 GNU.sparse.offset=-1\n26 GNU.sparse.numbytes=10\n";
        assertThrows(IOException.class, () -> TarUtils.parsePaxHeaders(
            new ByteArrayInputStream(header.getBytes(UTF_8)),
            null, Collections.emptyMap()));
    }

    @Test
    public void readPax00SparseHeaderRejectsNonNumericNumbytes() throws Exception {
        final String header = "23 GNU.sparse.offset=0\n26 GNU.sparse.numbytes=1a\n";
        assertThrows(IOException.class, () -> TarUtils.parsePaxHeaders(
            new ByteArrayInputStream(header.getBytes(UTF_8)),
            null, Collections.emptyMap()));
    }

    @Test
    public void readPax00SparseHeaderRejectsNonNumericOffset() throws Exception {
        final String header = "23 GNU.sparse.offset=a\n26 GNU.sparse.numbytes=10\n";
        assertThrows(IOException.class, () -> TarUtils.parsePaxHeaders(
            new ByteArrayInputStream(header.getBytes(UTF_8)),
            null, Collections.emptyMap()));
    }

    @Test
    public void readPaxHeaderWithEmbeddedNewline() throws Exception {
        final Map<String, String> headers = TarUtils
                .parsePaxHeaders(new ByteArrayInputStream("28 comment=line1\nline2\nand3\n"
                        .getBytes(UTF_8)), null, new HashMap<>());
        assertEquals(1, headers.size());
        assertEquals("line1\nline2\nand3", headers.get("comment"));
    }

    @Test
    public void readPaxHeaderWithoutTrailingNewline() throws Exception {
        assertThrows(IOException.class, () -> TarUtils.parsePaxHeaders(
            new ByteArrayInputStream("30 atime=1321711775.9720594634".getBytes(UTF_8)),
            null, Collections.emptyMap()));
    }

    @Test
    public void readSimplePaxHeader() throws Exception {
        final Map<String, String> headers = TarUtils.parsePaxHeaders(
                new ByteArrayInputStream("30 atime=1321711775.972059463\n".getBytes(UTF_8)),
                null, new HashMap<>());
        assertEquals(1, headers.size());
        assertEquals("1321711775.972059463", headers.get("atime"));
    }

    @Test
    public void readSparseStructsBinary() throws Exception {
        final byte[] header = {
            (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,
        };
        assertEquals(24, header.length);
        final List<TarArchiveStructSparse> sparse = TarUtils.readSparseStructs(header, 0, 1);
        assertEquals(1, sparse.size());
        assertEquals(0, sparse.get(0).getOffset());
        assertEquals(7, sparse.get(0).getNumbytes());
    }

    @Test
    public void readSparseStructsOctal() throws Exception {
        final byte[] header = "00000000000 00000000007 ".getBytes();
        assertEquals(24, header.length);
        final List<TarArchiveStructSparse> sparse = TarUtils.readSparseStructs(header, 0, 1);
        assertEquals(1, sparse.size());
        assertEquals(0, sparse.get(0).getOffset());
        assertEquals(7, sparse.get(0).getNumbytes());
    }

    @Test
    public void readSparseStructsRejectsNegativeNumbytes() throws Exception {
        final byte[] header = {
            (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        };
        assertThrows(IOException.class, () -> TarUtils.readSparseStructs(header, 0, 1));
    }

    @Test
    public void readSparseStructsRejectsNegativeOffset() throws Exception {
        final byte[] header = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,
        };
        assertThrows(IOException.class, () -> TarUtils.readSparseStructs(header, 0, 1));
    }

    @Test
    public void readSparseStructsRejectsNonNumericNumbytes() throws Exception {
        final byte[] header = "00000000000 0000000000x ".getBytes();
        assertThrows(IOException.class, () -> TarUtils.readSparseStructs(header, 0, 1));
    }

    @Test
    public void readSparseStructsRejectsNonNumericOffset() throws Exception {
        final byte[] header = "0000000000x 00000000007 ".getBytes();
        assertThrows(IOException.class, () -> TarUtils.readSparseStructs(header, 0, 1));
    }

    @Test
    public void secondEntryWinsWhenPaxHeaderContainsDuplicateKey() throws Exception {
        final Map<String, String> headers = TarUtils.parsePaxHeaders(new ByteArrayInputStream("11 foo=bar\n11 foo=baz\n"
                        .getBytes(UTF_8)), null, new HashMap<>());
        assertEquals(1, headers.size());
        assertEquals("baz", headers.get("foo"));
    }

    @Test
    public void testName(){
        byte [] buff = new byte[20];
        final String sb1 = "abcdefghijklmnopqrstuvwxyz";
        int off = TarUtils.formatNameBytes(sb1, buff, 1, buff.length-1);
        assertEquals(off, 20);
        String sb2 = TarUtils.parseName(buff, 1, 10);
        assertEquals(sb2,sb1.substring(0,10));
        sb2 = TarUtils.parseName(buff, 1, 19);
        assertEquals(sb2,sb1.substring(0,19));
        buff = new byte[30];
        off = TarUtils.formatNameBytes(sb1, buff, 1, buff.length-1);
        assertEquals(off, 30);
        sb2 = TarUtils.parseName(buff, 1, buff.length-1);
        assertEquals(sb1, sb2);
        buff = new byte[]{0, 1, 0};
        sb2 = TarUtils.parseName(buff, 0, 3);
        assertEquals("", sb2);
    }

    @Test
    public void testNegative() {
        final byte [] buffer = new byte[22];
        TarUtils.formatUnsignedOctalString(-1, buffer, 0, buffer.length);
        assertEquals("1777777777777777777777", new String(buffer, UTF_8));
    }

    @Test
    public void testOverflow() {
        final byte [] buffer = new byte[8-1]; // a lot of the numbers have 8-byte buffers (nul term)
        TarUtils.formatUnsignedOctalString(07777777L, buffer, 0, buffer.length);
        assertEquals("7777777", new String(buffer, UTF_8));
        assertThrows(IllegalArgumentException.class, () -> TarUtils.formatUnsignedOctalString(017777777L, buffer, 0, buffer.length),
                "Should have cause IllegalArgumentException");
    }

    @Test
    public void testParseOctal() {
        long value;
        byte [] buffer;
        final long MAX_OCTAL  = 077777777777L; // Allowed 11 digits
        final long MAX_OCTAL_OVERFLOW  = 0777777777777L; // in fact 12 for some implementations
        final String maxOctal = "777777777777"; // Maximum valid octal
        buffer = maxOctal.getBytes(UTF_8);
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(MAX_OCTAL_OVERFLOW, value);
        buffer[buffer.length - 1] = ' ';
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(MAX_OCTAL, value);
        buffer[buffer.length-1]=0;
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(MAX_OCTAL, value);
        buffer=new byte[]{0,0};
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(0, value);
        buffer=new byte[]{0,' '};
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(0, value);
        buffer=new byte[]{' ',0};
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(0, value);
    }

    @Test
    public void testParseOctalCompress330() {
        final long expected = 0100000;
        final byte [] buffer = {
            32, 32, 32, 32, 32, 49, 48, 48, 48, 48, 48, 32
        };
        assertEquals(expected, TarUtils.parseOctalOrBinary(buffer, 0, buffer.length));
    }

    @Test
    public void testParseOctalInvalid() {
        final byte[] buffer1 = ByteUtils.EMPTY_BYTE_ARRAY;
        assertThrows(IllegalArgumentException.class, () -> TarUtils.parseOctal(buffer1, 0, buffer1.length),
                "Expected IllegalArgumentException - should be at least 2 bytes long");

        final byte[] buffer2 = {0}; // 1-byte array
        assertThrows(IllegalArgumentException.class, () -> TarUtils.parseOctal(buffer2, 0, buffer2.length),
                "Expected IllegalArgumentException - should be at least 2 bytes long");

        final byte[] buffer3 = "abcdef ".getBytes(UTF_8); // Invalid input
        assertThrows(IllegalArgumentException.class, () -> TarUtils.parseOctal(buffer3, 0, buffer3.length),
                "Expected IllegalArgumentException");

        final byte[] buffer4 = " 0 07 ".getBytes(UTF_8); // Invalid - embedded space
        assertThrows(IllegalArgumentException.class, () -> TarUtils.parseOctal(buffer3, 0, buffer3.length),
                "Expected IllegalArgumentException - embedded space");

        final byte[] buffer5 = " 0\00007 ".getBytes(UTF_8); // Invalid - embedded NUL
        assertThrows(IllegalArgumentException.class, () -> TarUtils.parseOctal(buffer5, 0, buffer5.length),
                "Expected IllegalArgumentException - embedded NUL");
    }

    @Test
    public void testParseSparse() {
        final long expectedOffset = 0100000;
        final long expectedNumbytes = 0111000;
        final byte [] buffer = {
                ' ', ' ', ' ', ' ', ' ', '0', '1', '0', '0', '0', '0', '0', // sparseOffset
                ' ', ' ', ' ', ' ', ' ', '0', '1', '1', '1', '0', '0', '0'};
        final TarArchiveStructSparse sparse = TarUtils.parseSparse(buffer, 0);
        assertEquals(sparse.getOffset(), expectedOffset);
        assertEquals(sparse.getNumbytes(), expectedNumbytes);
    }

    @Test
    public void testParseTarWithSpecialPaxHeaders() throws IOException {
        try (InputStream in = newInputStream("COMPRESS-530.tar");
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            assertThrows(IOException.class, () -> archive.getNextEntry());
            // IOUtils.toByteArray(archive);
        }
    }

    @Test
    public void testReadNegativeBinary12Byte() {
        final byte[] b = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xf1, (byte) 0xef,
        };
        assertEquals(-3601L, TarUtils.parseOctalOrBinary(b, 0, 12));
    }

    @Test
    public void testReadNegativeBinary8Byte() {
        final byte[] b = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xf1, (byte) 0xef,
        };
        assertEquals(-3601L, TarUtils.parseOctalOrBinary(b, 0, 8));
    }

    @Test
    public void testRoundEncoding() throws Exception {
        // COMPRESS-114
        final ZipEncoding enc = ZipEncodingHelper.getZipEncoding(CharsetNames.ISO_8859_1);
        final String s = "0302-0601-3\u00b1\u00b1\u00b1F06\u00b1W220\u00b1ZB\u00b1LALALA\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1CAN\u00b1\u00b1DC\u00b1\u00b1\u00b104\u00b1060302\u00b1MOE.model";
        final byte[] buff = new byte[100];
        final int len = TarUtils.formatNameBytes(s, buff, 0, buff.length, enc);
        assertEquals(s, TarUtils.parseName(buff, 0, len, enc));
    }

    @Test
    public void testRoundTripNames(){
        checkName("");
        checkName("The quick brown fox\n");
        checkName("\177");
        // checkName("\0"); // does not work, because NUL is ignored
    }

    @Test
    public void testRoundTripOctal() {
        checkRoundTripOctal(0);
        checkRoundTripOctal(1);
//        checkRoundTripOctal(-1); // TODO What should this do?
        checkRoundTripOctal(TarConstants.MAXSIZE);
//        checkRoundTripOctal(0100000000000L); // TODO What should this do?

        checkRoundTripOctal(0, TarConstants.UIDLEN);
        checkRoundTripOctal(1, TarConstants.UIDLEN);
        checkRoundTripOctal(TarConstants.MAXID, 8);
    }

    private void testRoundTripOctalOrBinary(final int length) {
        checkRoundTripOctalOrBinary(0, length);
        checkRoundTripOctalOrBinary(1, length);
        checkRoundTripOctalOrBinary(TarConstants.MAXSIZE, length); // will need binary format
        checkRoundTripOctalOrBinary(-1, length); // will need binary format
        checkRoundTripOctalOrBinary(0xffffffffffffffL, length);
        checkRoundTripOctalOrBinary(-0xffffffffffffffL, length);
    }

    @Test
    public void testRoundTripOctalOrBinary12() {
        testRoundTripOctalOrBinary(12);
        checkRoundTripOctalOrBinary(Long.MAX_VALUE, 12);
        checkRoundTripOctalOrBinary(Long.MIN_VALUE + 1, 12);
    }

    @Test
    public void testRoundTripOctalOrBinary8() {
        testRoundTripOctalOrBinary(8);
    }

    @Test
    public void testRoundTripOctalOrBinary8_ValueTooBigForBinary() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> checkRoundTripOctalOrBinary(Long.MAX_VALUE, 8),
                "Should throw exception - value is too long to fit buffer of this len");
        assertEquals("Value 9223372036854775807 is too large for 8 byte field.", e.getMessage());
    }
    // Check correct trailing bytes are generated
    @Test
    public void testTrailers() {
        final byte [] buffer = new byte[12];
        TarUtils.formatLongOctalBytes(123, buffer, 0, buffer.length);
        assertEquals(' ', buffer[buffer.length-1]);
        assertEquals('3', buffer[buffer.length-2]); // end of number
        TarUtils.formatOctalBytes(123, buffer, 0, buffer.length);
        assertEquals(0  , buffer[buffer.length-1]);
        assertEquals(' ', buffer[buffer.length-2]);
        assertEquals('3', buffer[buffer.length-3]); // end of number
        TarUtils.formatCheckSumOctalBytes(123, buffer, 0, buffer.length);
        assertEquals(' ', buffer[buffer.length-1]);
        assertEquals(0  , buffer[buffer.length-2]);
        assertEquals('3', buffer[buffer.length-3]); // end of number
    }

    // https://issues.apache.org/jira/browse/COMPRESS-191
    @Test
    public void testVerifyHeaderCheckSum() {
        final byte[] valid = { // from bla.tar
                116, 101, 115, 116, 49, 46, 120, 109, 108, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 48, 48, 48, 48, 54, 52, 52, 0, 48, 48, 48, 48, 55, 54, 53,
                0, 48, 48, 48, 48, 55, 54, 53, 0, 48, 48, 48, 48, 48, 48, 48,
                49, 49, 52, 50, 0, 49, 48, 55, 49, 54, 53, 52, 53, 54, 50, 54,
                0, 48, 49, 50, 50, 54, 48, 0, 32, 48, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 117, 115, 116, 97, 114, 32, 32, 0,
                116, 99, 117, 114, 100, 116, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 116, 99, 117,
                114, 100, 116, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0 };
        assertTrue(TarUtils.verifyCheckSum(valid));

        final byte[] compress117 = { // from COMPRESS-117
            (byte) 0x37, (byte) 0x7a, (byte) 0x43, (byte) 0x2e, (byte) 0x74, (byte) 0x78, (byte) 0x74, (byte) 0x00,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x37,
            (byte) 0x37, (byte) 0x37, (byte) 0x20, (byte) 0x00, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x30, (byte) 0x20, (byte) 0x00, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x30, (byte) 0x20, (byte) 0x00, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x31, (byte) 0x33, (byte) 0x30, (byte) 0x33, (byte) 0x33, (byte) 0x20,
            (byte) 0x31, (byte) 0x31, (byte) 0x31, (byte) 0x31, (byte) 0x35, (byte) 0x31, (byte) 0x36, (byte) 0x36,
            (byte) 0x30, (byte) 0x31, (byte) 0x36, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x35, (byte) 0x34,
            (byte) 0x31, (byte) 0x37, (byte) 0x20, (byte) 0x00, (byte) 0x30, (byte) 0x00, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        };
        assertTrue(TarUtils.verifyCheckSum(compress117));

        final byte[] invalid = { // from the testAIFF.aif file in Tika
                70, 79, 82, 77, 0, 0, 15, 46, 65, 73, 70, 70, 67, 79, 77, 77,
                0, 0, 0, 18, 0, 2, 0, 0, 3, -64, 0, 16, 64, 14, -84, 68, 0, 0,
                0, 0, 0, 0, 83, 83, 78, 68, 0, 0, 15, 8, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 1, -1, -1, 0, 1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 1, -1, -1,
                0, 0, 0, 0, 0, 0, -1, -1, 0, 2, -1, -2, 0, 2, -1, -1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 1, -1, -1, 0, 0, -1, -1, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 1, -1, -1, 0, 1, -1, -2, 0, 1, -1, -1, 0, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0,
                2, -1, -2, 0, 2, -1, -1, 0, 0, 0, 1, -1, -1, 0, 1, -1, -1, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -2, 0, 2, -1, -2, 0, 1, 0, 0,
                0, 1, -1, -1, 0, 0, 0, 1, -1, -1, 0, 0, 0, 1, -1, -2, 0, 2,
                -1, -1, 0, 0, 0, 0, 0, 0, -1, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 1, -1, -1, 0, 2, -1, -2,
                0, 2, -1, -2, 0, 2, -1, -2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, -1,
                -2, 0, 2, -1, -2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                -1, -1, 0, 1, 0, 0, -1, -1, 0, 2, -1, -2, 0, 2, -1, -1, 0, 0,
                0, 0, 0, 0, -1, -1, 0, 1, -1, -1, 0, 1, -1, -1, 0, 2, -1, -2,
                0, 1, 0, 0, -1, -1, 0, 2, -1, -2, 0, 2, -1, -2, 0, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -1, 0, 0, 0,
                0, -1, -1, 0, 1, 0, 0, 0, 0, 0, 1, -1, -1, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -2, 0, 2, -1, -1, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -2, 0, 1, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 1, -1, -1, 0, 0, 0, 0, -1, -1, 0, 2, -1, -2,
                0, 2, -1, -2, 0, 2, -1, -1, 0, 0, 0, 0, -1, -1, 0, 1, -1, -1,
                0, 1, -1, -1, 0, 1, -1, -1, 0, 1, -1, -1, 0, 1, 0, 0, 0, 0,
                -1, -1, 0, 2, -1, -2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, -1, -1, 0, 0, 0, 0, -1, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 1 };
        assertFalse(TarUtils.verifyCheckSum(invalid));
    }

    @Test
    public void testWriteNegativeBinary8Byte() {
        final byte[] b = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xf1, (byte) 0xef,
        };
        assertEquals(-3601L, TarUtils.parseOctalOrBinary(b, 0, 8));
    }

}

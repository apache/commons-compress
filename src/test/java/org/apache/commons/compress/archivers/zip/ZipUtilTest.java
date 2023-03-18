/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.zip;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ZipUtilTest {

    static void assertDosDate(
            final long value,
            final int year,
            final int month,
            final int day,
            final int hour,
            final int minute,
            final int second) {
        int pos = 0;
        assertEquals((year - 1980), ((int) (value << pos)) >>> (32 - 7));
        assertEquals(month, ((int) (value << (pos += 7))) >>> (32 - 4));
        assertEquals(day, ((int) (value << (pos += 4))) >>> (32 - 5));
        assertEquals(hour, ((int) (value << (pos += 5))) >>> (32 - 5));
        assertEquals(minute, ((int) (value << (pos += 5))) >>> (32 - 6));
        assertEquals(second, ((int) (value << (pos + 6))) >>> (32 - 5) << 1); // DOS dates only store even seconds
    }
    static Instant toLocalInstant(final String date) {
        return LocalDateTime.parse(date).atZone(ZoneId.systemDefault()).toInstant();
    }

    private Date time;

    private ZipLong zl;

    @BeforeEach
    public void setUp() throws Exception {
        time = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        final int year = cal.get(Calendar.YEAR);
        final int month = cal.get(Calendar.MONTH) + 1;
        // @formatter:off
        final long value = ((year - 1980) << 25)
            | (month << 21)
            | (cal.get(Calendar.DAY_OF_MONTH) << 16)
            | (cal.get(Calendar.HOUR_OF_DAY) << 11)
            | (cal.get(Calendar.MINUTE) << 5)
            | (cal.get(Calendar.SECOND) >> 1);
        // @formatter:on

        final byte[] result = new byte[4];
        result[0] = (byte) ((value & 0xFF));
        result[1] = (byte) ((value & 0xFF00) >> 8);
        result[2] = (byte) ((value & 0xFF0000) >> 16);
        result[3] = (byte) ((value & 0xFF000000L) >> 24);
        zl = new ZipLong(result);
    }

    @Test
    public void testAdjustToLong() {
        assertEquals(Integer.MAX_VALUE,
                     ZipUtil.adjustToLong(Integer.MAX_VALUE));
        assertEquals(((long) Integer.MAX_VALUE) + 1,
                     ZipUtil.adjustToLong(Integer.MAX_VALUE + 1));
        assertEquals(2 * ((long) Integer.MAX_VALUE),
                     ZipUtil.adjustToLong(2 * Integer.MAX_VALUE));
    }

    @Test
    public void testBigToLong() {
        final BigInteger big1 = BigInteger.valueOf(1);
        final BigInteger big2 = BigInteger.valueOf(Long.MAX_VALUE);
        final BigInteger big3 = BigInteger.valueOf(Long.MIN_VALUE);

        assertEquals(1L, ZipUtil.bigToLong(big1));
        assertEquals(Long.MAX_VALUE, ZipUtil.bigToLong(big2));
        assertEquals(Long.MIN_VALUE, ZipUtil.bigToLong(big3));

        final BigInteger big4 = big2.add(big1);
        assertThrows(IllegalArgumentException.class, () -> ZipUtil.bigToLong(big4),
                "Should have thrown IllegalArgumentException");

        final BigInteger big5 = big3.subtract(big1);
        assertThrows(IllegalArgumentException.class, () -> ZipUtil.bigToLong(big5),
                "ZipUtil.bigToLong(BigInteger) should have thrown IllegalArgumentException");
    }

    @Test
    public void testFromDosTime() {
        ZipLong testDosTime = new ZipLong(1 << 21);
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1980);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date testDate = ZipUtil.fromDosTime(testDosTime);
        assertEquals(testDate.getTime(), cal.getTime().getTime());

        testDosTime = ZipUtil.toDosTime(time);
        testDate = ZipUtil.fromDosTime(testDosTime);
        // the minimal time unit for dos time is 2 seconds
        assertEquals(testDate.getTime() / 2000, (time.getTime() / 2000));
    }

    @Test
    public void testInsideCalendar(){
        final long date = toLocalInstant("1985-02-01T09:00:00").toEpochMilli();
        final byte[] b1 = ZipUtil.toDosTime(date);
        assertEquals(0, b1[0]);
        assertEquals(72, b1[1]);
        assertEquals(65, b1[2]);
        assertEquals(10, b1[3]);
    }
    @Test
    public void testInsideCalendar_bigValue(){
        final long date = toLocalInstant("2097-11-27T23:59:59").toEpochMilli();
        final long value = ZipLong.getValue(ZipUtil.toDosTime(date));
        assertDosDate(value, 2097, 11, 27, 23, 59, 58); // DOS dates only store even seconds
    }

    @Test
    public void testInsideCalendar_long(){
        final long date = toLocalInstant("1985-02-01T09:00:00").toEpochMilli();
        final long value = ZipLong.getValue(ZipUtil.toDosTime(date));
        assertDosDate(value, 1985, 2, 1, 9, 0, 0);
    }

    @Test
    public void testInsideCalendar_modernDate(){
        final long date = toLocalInstant("2022-12-27T16:18:23").toEpochMilli();
        final long value = ZipLong.getValue(ZipUtil.toDosTime(date));
        assertDosDate(value, 2022, 12, 27, 16, 18, 22); // DOS dates only store even seconds
    }

    @Test
    public void testIsDosTime(){
        assertFalse(ZipUtil.isDosTime(toLocalInstant("1975-01-31T23:00:00").toEpochMilli()));
        assertTrue(ZipUtil.isDosTime(toLocalInstant("1980-01-03T00:00:00").toEpochMilli()));
        assertTrue(ZipUtil.isDosTime(toLocalInstant("2097-11-27T00:00:00").toEpochMilli()));
        assertFalse(ZipUtil.isDosTime(toLocalInstant("2099-01-01T00:00:00").toEpochMilli()));
    }

    @Test
    public void testLongToBig() {
        final long l0 = 0;
        final long l1 = 1;
        final long l2 = -1;
        final long l3 = Integer.MIN_VALUE;
        final long l4 = Long.MAX_VALUE;
        final long l5 = Long.MIN_VALUE;

        final BigInteger big0 = ZipUtil.longToBig(l0);
        final BigInteger big1 = ZipUtil.longToBig(l1);
        final BigInteger big2 = ZipUtil.longToBig(l2);
        final BigInteger big3 = ZipUtil.longToBig(l3);
        final BigInteger big4 = ZipUtil.longToBig(l4);

        assertEquals(0, big0.longValue());
        assertEquals(1, big1.longValue());
        assertEquals(0xFFFFFFFFL, big2.longValue());
        assertEquals(0x80000000L, big3.longValue());
        assertEquals(Long.MAX_VALUE, big4.longValue());

        assertThrows(IllegalArgumentException.class, () -> ZipUtil.longToBig(l5),
                "ZipUtil.longToBig(long) should have thrown IllegalArgumentException");
    }

    @Test
    public void testMinTime(){
        final byte[] b1 = ZipUtil.toDosTime(0);
        final byte b10 = b1[0]; // Save the first byte
        b1[0]++; // change it
        final byte[] b2 = ZipUtil.toDosTime(0); // get the same time
        assertEquals(b10,b2[0]); // first byte should still be the same
    }

    @Test
    public void testOutsideCalendar(){
        final long date = toLocalInstant("1975-01-31T23:00:00").toEpochMilli();
        final byte[] b1 = ZipUtil.toDosTime(date);
        assertEquals(0, b1[0]);
        assertEquals(0, b1[1]);
        assertEquals(33, b1[2]);
        assertEquals(0, b1[3]);
    }

    @Test
    public void testOutsideCalendar_long(){
        final long date = toLocalInstant("1975-01-31T23:00:00").toEpochMilli();
        final long value = ZipLong.getValue(ZipUtil.toDosTime(date));
        assertDosDate(value, 1980, 1, 1, 0, 0, 0);
    }

    @Test
    public void testReverse() {
        final byte[][] bTest = new byte[6][];
        bTest[0] = new byte[]{};
        bTest[1] = new byte[]{1};
        bTest[2] = new byte[]{1, 2};
        bTest[3] = new byte[]{1, 2, 3};
        bTest[4] = new byte[]{1, 2, 3, 4};
        bTest[5] = new byte[]{1, 2, 3, 4, 5};

        final byte[][] rTest = new byte[6][];
        rTest[0] = new byte[]{};
        rTest[1] = new byte[]{1};
        rTest[2] = new byte[]{2, 1};
        rTest[3] = new byte[]{3, 2, 1};
        rTest[4] = new byte[]{4, 3, 2, 1};
        rTest[5] = new byte[]{5, 4, 3, 2, 1};

        assertEquals(bTest.length, rTest.length, "test and result arrays are same length");

        for (int i = 0; i < bTest.length; i++) {
            final byte[] result = ZipUtil.reverse(bTest[i]);
            assertSame(bTest[i], result, "reverse mutates in-place");
            assertArrayEquals(rTest[i], result, "reverse actually reverses");
        }
    }

    @Test
    public void testSignedByteToUnsignedInt() {
        // Yay, we can completely test all possible input values in this case!
        int expectedVal = 128;
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            final byte b = (byte) i;
            assertEquals(expectedVal, ZipUtil.signedByteToUnsignedInt(b));
            expectedVal++;
            if (expectedVal == 256) {
                expectedVal = 0;
            }
        }
    }

    @Test
    public void testUnknownMethod() throws Exception {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        ze.setMethod(100);
        assertThrows(UnsupportedZipFeatureException.class, () -> ZipUtil.checkRequestedFeatures(ze));
    }

    @Test
    public void testUnsignedIntToSignedByte() {
        int unsignedVal = 128;
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            final byte expectedVal = (byte) i;
            assertEquals(expectedVal, ZipUtil.unsignedIntToSignedByte(unsignedVal));
            unsignedVal++;
            if (unsignedVal == 256) {
                unsignedVal = 0;
            }
        }

        assertThrows(IllegalArgumentException.class, () -> ZipUtil.unsignedIntToSignedByte(-1),
                "ZipUtil.unsignedIntToSignedByte(-1) should have thrown IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> ZipUtil.unsignedIntToSignedByte(256),
                "ZipUtil.unsignedIntToSignedByte(256) should have thrown IllegalArgumentException");
    }

    @Test
    public void testUnsupportedMethod() throws Exception {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        ze.setMethod(ZipMethod.EXPANDING_LEVEL_1.getCode());
        assertThrows(UnsupportedZipFeatureException.class, () -> ZipUtil.checkRequestedFeatures(ze));
    }

    @Test
    public void testZipLong() {
        final ZipLong test = ZipUtil.toDosTime(time);
        assertEquals(test.getValue(), zl.getValue());
    }
}

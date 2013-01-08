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

import junit.framework.TestCase;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class ZipUtilTest extends TestCase {

    private Date time;
    private ZipLong zl;

    /**
     * Constructor
     */
    public ZipUtilTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        time = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        long value =  ((year - 1980) << 25)
            |         (month << 21)
            |         (cal.get(Calendar.DAY_OF_MONTH) << 16)
            |         (cal.get(Calendar.HOUR_OF_DAY) << 11)
            |         (cal.get(Calendar.MINUTE) << 5)
            |         (cal.get(Calendar.SECOND) >> 1);

        byte[] result = new byte[4];
        result[0] = (byte) ((value & 0xFF));
        result[1] = (byte) ((value & 0xFF00) >> 8);
        result[2] = (byte) ((value & 0xFF0000) >> 16);
        result[3] = (byte) ((value & 0xFF000000L) >> 24);
        zl = new ZipLong(result);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testZipLong() throws Exception {
        ZipLong test = ZipUtil.toDosTime(time);
        assertEquals(test.getValue(), zl.getValue());
    }

    public void testAdjustToLong() {
        assertEquals(Integer.MAX_VALUE,
                     ZipUtil.adjustToLong(Integer.MAX_VALUE));
        assertEquals(((long) Integer.MAX_VALUE) + 1,
                     ZipUtil.adjustToLong(Integer.MAX_VALUE + 1));
        assertEquals(2 * ((long) Integer.MAX_VALUE),
                     ZipUtil.adjustToLong(2 * Integer.MAX_VALUE));
    }

    public void testMinTime(){
        byte[] b1 = ZipUtil.toDosTime(0);
        byte b10 = b1[0]; // Save the first byte
        b1[0]++; // change it
        byte[] b2 = ZipUtil.toDosTime(0); // get the same time
        assertEquals(b10,b2[0]); // first byte should still be the same
    }

    public void testReverse() {
        byte[][] bTest = new byte[6][];
        bTest[0] = new byte[]{};
        bTest[1] = new byte[]{1};
        bTest[2] = new byte[]{1, 2};
        bTest[3] = new byte[]{1, 2, 3};
        bTest[4] = new byte[]{1, 2, 3, 4};
        bTest[5] = new byte[]{1, 2, 3, 4, 5};

        byte[][] rTest = new byte[6][];
        rTest[0] = new byte[]{};
        rTest[1] = new byte[]{1};
        rTest[2] = new byte[]{2, 1};
        rTest[3] = new byte[]{3, 2, 1};
        rTest[4] = new byte[]{4, 3, 2, 1};
        rTest[5] = new byte[]{5, 4, 3, 2, 1};

        assertEquals("test and result arrays are same length", bTest.length, rTest.length);

        for (int i = 0; i < bTest.length; i++) {
            byte[] result = ZipUtil.reverse(bTest[i]);
            assertTrue("reverse mutates in-place", bTest[i] == result);
            assertTrue("reverse actually reverses", Arrays.equals(rTest[i], result));
        }
    }

    public void testBigToLong() {
        BigInteger big1 = BigInteger.valueOf(1);
        BigInteger big2 = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger big3 = BigInteger.valueOf(Long.MIN_VALUE);

        assertEquals(1L, ZipUtil.bigToLong(big1));
        assertEquals(Long.MAX_VALUE, ZipUtil.bigToLong(big2));
        assertEquals(Long.MIN_VALUE, ZipUtil.bigToLong(big3));

        BigInteger big4 = big2.add(big1);
        try {
            ZipUtil.bigToLong(big4);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // All is good.
        }

        BigInteger big5 = big3.subtract(big1);
        try {
            ZipUtil.bigToLong(big5);
            fail("ZipUtil.bigToLong(BigInteger) should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // All is good.
        }
    }

    public void testLongToBig() {
        long l0 = 0;
        long l1 = 1;
        long l2 = -1;
        long l3 = Integer.MIN_VALUE;
        long l4 = Long.MAX_VALUE;
        long l5 = Long.MIN_VALUE;

        BigInteger big0 = ZipUtil.longToBig(l0);
        BigInteger big1 = ZipUtil.longToBig(l1);
        BigInteger big2 = ZipUtil.longToBig(l2);
        BigInteger big3 = ZipUtil.longToBig(l3);
        BigInteger big4 = ZipUtil.longToBig(l4);

        assertEquals(0, big0.longValue());
        assertEquals(1, big1.longValue());
        assertEquals(0xFFFFFFFFL, big2.longValue());
        assertEquals(0x80000000L, big3.longValue());
        assertEquals(Long.MAX_VALUE, big4.longValue());

        try {
            ZipUtil.longToBig(l5);
            fail("ZipUtil.longToBig(long) should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {

        }
    }

    public void testSignedByteToUnsignedInt() {
        // Yay, we can completely test all possible input values in this case!
        int expectedVal = 128;
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            assertEquals(expectedVal, ZipUtil.signedByteToUnsignedInt(b));
            expectedVal++;
            if (expectedVal == 256) {
                expectedVal = 0;
            }
        }
    }

    public void testUnsignedIntToSignedByte() {
        int unsignedVal = 128;
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte expectedVal = (byte) i;
            assertEquals(expectedVal, ZipUtil.unsignedIntToSignedByte(unsignedVal));
            unsignedVal++;
            if (unsignedVal == 256) {
                unsignedVal = 0;
            }
        }

        try {
            ZipUtil.unsignedIntToSignedByte(-1);
            fail("ZipUtil.unsignedIntToSignedByte(-1) should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // All is good.
        }

        try {
            ZipUtil.unsignedIntToSignedByte(256);
            fail("ZipUtil.unsignedIntToSignedByte(256) should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // All is good.
        }

    }


}

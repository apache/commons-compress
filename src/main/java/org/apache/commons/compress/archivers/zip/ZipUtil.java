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

import java.util.Calendar;
import java.util.Date;

public abstract class ZipUtil {
    /**
     * Smallest date/time ZIP can handle.
     */
    private static final byte[] DOS_TIME_MIN = ZipLong.getBytes(0x00002100L);

    /**
     * Convert a Date object to a DOS date/time field.
     * @param time the <code>Date</code> to convert
     * @return the date as a <code>ZipLong</code>
     */
    public static ZipLong toDosTime(Date time) {
        return new ZipLong(toDosTime(time.getTime()));
    }

    /**
     * Convert a Date object to a DOS date/time field.
     *
     * <p>Stolen from InfoZip's <code>fileio.c</code></p>
     * @param t number of milliseconds since the epoch
     * @return the date as a byte array
     */
    public static byte[] toDosTime(long t) {
        Date time = new Date(t);
        // CheckStyle:MagicNumberCheck OFF - I do not think that using constants
        //                                   here will improve the readablity
        int year = time.getYear() + 1900;
        if (year < 1980) {
            return DOS_TIME_MIN;
        }
        int month = time.getMonth() + 1;
        long value =  ((year - 1980) << 25)
            |         (month << 21)
            |         (time.getDate() << 16)
            |         (time.getHours() << 11)
            |         (time.getMinutes() << 5)
            |         (time.getSeconds() >> 1);
        return ZipLong.getBytes(value);
        // CheckStyle:MagicNumberCheck ON
    }

    /**
     * Assumes a negative integer really is a positive integer that
     * has wrapped around and re-creates the original value.
     * @param i the value to treat as unsigned int.
     * @return the unsigned int as a long.
     */
    public static long adjustToLong(int i) {
        if (i < 0) {
            return 2 * ((long) Integer.MAX_VALUE) + 2 + i;
        } else {
            return i;
        }
    }

    /**
     * Convert a DOS date/time field to a Date object.
     *
     * @param zipDosTime contains the stored DOS time.
     * @return a Date instance corresponding to the given time.
     */
    public static Date fromDosTime(ZipLong zipDosTime) {
        long dosTime = zipDosTime.getValue();
        return new Date(dosToJavaTime(dosTime));
    }

    /**
     * Converts DOS time to Java time (number of milliseconds since
     * epoch).
     */
    public static long dosToJavaTime(long dosTime) {
        Calendar cal = Calendar.getInstance();
        // CheckStyle:MagicNumberCheck OFF - no point
        cal.set(Calendar.YEAR, (int) ((dosTime >> 25) & 0x7f) + 1980);
        cal.set(Calendar.MONTH, (int) ((dosTime >> 21) & 0x0f) - 1);
        cal.set(Calendar.DATE, (int) (dosTime >> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (int) (dosTime >> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (int) (dosTime >> 5) & 0x3f);
        cal.set(Calendar.SECOND, (int) (dosTime << 1) & 0x3e);
        // CheckStyle:MagicNumberCheck ON
        return cal.getTime().getTime();
    }


}
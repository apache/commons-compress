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
package org.apache.commons.compress.harmony.unpack200;

/**
 * Utility class for unpack200
 */
public final class SegmentUtils {

    public static int countArgs(final String descriptor) {
        return countArgs(descriptor, 1);
    }

    /**
     * Count the number of arguments in the descriptor. Each long or double counts as widthOfLongsAndDoubles; all other
     * arguments count as 1.
     *
     * @param descriptor String for which arguments are counted
     * @param widthOfLongsAndDoubles int increment to apply for longs doubles. This is typically 1 when counting
     *        arguments alone, or 2 when counting arguments for invokeinterface.
     * @return integer count
     */
    protected static int countArgs(final String descriptor, final int widthOfLongsAndDoubles) {
        final int bra = descriptor.indexOf('(');
        final int ket = descriptor.indexOf(')');
        if (bra == -1 || ket == -1 || ket < bra) {
            throw new IllegalArgumentException("No arguments");
        }

        boolean inType = false;
        boolean consumingNextType = false;
        int count = 0;
        for (int i = bra + 1; i < ket; i++) {
            final char charAt = descriptor.charAt(i);
            if (inType && charAt == ';') {
                inType = false;
                consumingNextType = false;
            } else if (!inType && charAt == 'L') {
                inType = true;
                count++;
            } else if (charAt == '[') {
                consumingNextType = true;
            } else if (inType) {
                // NOP
            } else if (consumingNextType) {
                count++;
                consumingNextType = false;
            } else if (charAt == 'D' || charAt == 'J') {
                count += widthOfLongsAndDoubles;
            } else {
                count++;
            }
        }
        return count;
    }

    public static int countBit16(final int[] flags) {
        int count = 0;
        for (final int flag : flags) {
            if ((flag & (1 << 16)) != 0) {
                count++;
            }
        }
        return count;
    }

    public static int countBit16(final long[] flags) {
        int count = 0;
        for (final long flag : flags) {
            if ((flag & (1 << 16)) != 0) {
                count++;
            }
        }
        return count;
    }

    public static int countBit16(final long[][] flags) {
        int count = 0;
        for (final long[] flag : flags) {
            for (final long element : flag) {
                if ((element & (1 << 16)) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int countInvokeInterfaceArgs(final String descriptor) {
        return countArgs(descriptor, 2);
    }

    public static int countMatches(final long[] flags, final IMatcher matcher) {
        int count = 0;
        for (final long flag : flags) {
            if (matcher.matches(flag)) {
                count++;
            }
        }
        return count;
    }

    public static int countMatches(final long[][] flags, final IMatcher matcher) {
        int count = 0;
        for (final long[] flag : flags) {
            count += countMatches(flag, matcher);
        }
        return count;
    }

}

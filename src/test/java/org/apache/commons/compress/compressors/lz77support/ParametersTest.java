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
package org.apache.commons.compress.compressors.lz77support;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParametersTest {

    @Test
    public void defaultConstructor() {
        Parameters p = new Parameters(128);
        assertEquals(128, p.getWindowSize());
        assertEquals(3, p.getMinMatchSize());
        assertEquals(128, p.getMaxMatchSize());
        assertEquals(128, p.getMaxOffset());
        assertEquals(128, p.getMaxLiteralSize());
    }

    @Test
    public void minMatchSizeIsAtLeastThree() {
        Parameters p = new Parameters(128, 2, 3, 4, 5);
        assertEquals(3, p.getMinMatchSize());
    }

    @Test
    public void maxMatchSizeIsInfiniteWhenSmallerThanMinMatchSize() {
        Parameters p = new Parameters(128, 2, 2, 4, 5);
        assertEquals(Integer.MAX_VALUE, p.getMaxMatchSize());
    }

    @Test
    public void maxMatchSizeIsMinMatchSizeIfBothAreEqual() {
        Parameters p = new Parameters(128, 2, 3, 4, 5);
        assertEquals(3, p.getMaxMatchSize());
    }

    @Test
    public void maxOffsetIsWindowSizeIfSetTo0() {
        Parameters p = new Parameters(128, 2, 3, 0, 5);
        assertEquals(128, p.getMaxOffset());
    }

    @Test
    public void maxOffsetIsWindowSizeIfSetToANegativeValue() {
        Parameters p = new Parameters(128, 2, 3, -1, 5);
        assertEquals(128, p.getMaxOffset());
    }

    @Test
    public void maxOffsetIsWindowSizeIfBiggerThanWindowSize() {
        Parameters p = new Parameters(128, 2, 3, 129, 5);
        assertEquals(128, p.getMaxOffset());
    }

    @Test
    public void maxLiteralSizeIsWindowSizeIfSetTo0() {
        Parameters p = new Parameters(128, 2, 3, 4, 0);
        assertEquals(128, p.getMaxLiteralSize());
    }

    @Test
    public void maxLiteralSizeIsWindowSizeIfSetToANegativeValue() {
        Parameters p = new Parameters(128, 2, 3, 0, -1);
        assertEquals(128, p.getMaxLiteralSize());
    }

    @Test
    public void maxLiteralSizeIsWindowSizeIfSetToAValueTooBigToHoldInSlidingWindow() {
        Parameters p = new Parameters(128, 2, 3, 0, 259);
        assertEquals(128, p.getMaxLiteralSize());
    }

    @Test
    public void allParametersUsuallyTakeTheirSpecifiedValues() {
        Parameters p = new Parameters(256, 4, 5, 6, 7);
        assertEquals(256, p.getWindowSize());
        assertEquals(4, p.getMinMatchSize());
        assertEquals(5, p.getMaxMatchSize());
        assertEquals(6, p.getMaxOffset());
        assertEquals(7, p.getMaxLiteralSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void windowSizeMustNotBeSmallerThanMinMatchSize() {
        new Parameters(128, 200, 300, 400, 500);
    }

    @Test(expected = IllegalArgumentException.class)
    public void windowSizeMustNotBeAPowerOfTwo() {
        new Parameters(100, 200, 300, 400, 500);
    }
}

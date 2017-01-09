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
        assertEquals(3, p.getMinMatchLength());
        assertEquals(127, p.getMaxMatchLength());
        assertEquals(127, p.getMaxOffset());
        assertEquals(128, p.getMaxLiteralLength());
    }

    @Test
    public void minMatchLengthIsAtLeastThree() {
        Parameters p = new Parameters(128, 2, 3, 4, 5);
        assertEquals(3, p.getMinMatchLength());
    }

    @Test
    public void maxMatchLengthIsWinsizeMinus1WhenSmallerThanMinMatchLength() {
        Parameters p = new Parameters(128, 2, 2, 4, 5);
        assertEquals(127, p.getMaxMatchLength());
    }

    @Test
    public void maxMatchLengthIsMinMatchLengthIfBothAreEqual() {
        Parameters p = new Parameters(128, 2, 3, 4, 5);
        assertEquals(3, p.getMaxMatchLength());
    }

    @Test
    public void maxOffsetIsWindowSizeMinus1IfSetTo0() {
        Parameters p = new Parameters(128, 2, 3, 0, 5);
        assertEquals(127, p.getMaxOffset());
    }

    @Test
    public void maxOffsetIsWindowSizeMinus1IfSetToANegativeValue() {
        Parameters p = new Parameters(128, 2, 3, -1, 5);
        assertEquals(127, p.getMaxOffset());
    }

    @Test
    public void maxOffsetIsWindowSizeMinus1IfBiggerThanWindowSize() {
        Parameters p = new Parameters(128, 2, 3, 129, 5);
        assertEquals(127, p.getMaxOffset());
    }

    @Test
    public void maxLiteralLengthIsWindowSizeIfSetTo0() {
        Parameters p = new Parameters(128, 2, 3, 4, 0);
        assertEquals(128, p.getMaxLiteralLength());
    }

    @Test
    public void maxLiteralLengthIsWindowSizeIfSetToANegativeValue() {
        Parameters p = new Parameters(128, 2, 3, 0, -1);
        assertEquals(128, p.getMaxLiteralLength());
    }

    @Test
    public void maxLiteralLengthIsWindowSizeIfSetToAValueTooBigToHoldInSlidingWindow() {
        Parameters p = new Parameters(128, 2, 3, 0, 259);
        assertEquals(128, p.getMaxLiteralLength());
    }

    @Test
    public void allParametersUsuallyTakeTheirSpecifiedValues() {
        Parameters p = new Parameters(256, 4, 5, 6, 7);
        assertEquals(256, p.getWindowSize());
        assertEquals(4, p.getMinMatchLength());
        assertEquals(5, p.getMaxMatchLength());
        assertEquals(6, p.getMaxOffset());
        assertEquals(7, p.getMaxLiteralLength());
    }

    @Test(expected = IllegalArgumentException.class)
    public void windowSizeMustNotBeSmallerThanMinMatchLength() {
        new Parameters(128, 200, 300, 400, 500);
    }

    @Test(expected = IllegalArgumentException.class)
    public void windowSizeMustNotBeAPowerOfTwo() {
        new Parameters(100, 200, 300, 400, 500);
    }
}

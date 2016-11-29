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
package org.apache.commons.compress.archivers.sevenz;

import org.junit.Assert;
import org.junit.Test;
import org.tukaani.xz.LZMA2Options;

public class SevenZMethodConfigurationTest {

    @Test
    public void shouldAllowNullOptions() {
        Assert.assertNull(new SevenZMethodConfiguration(SevenZMethod.LZMA2, null)
                          .getOptions());
    }

    @Test
    public void shouldAllowLZMA2OptionsForLZMA() {
        Assert.assertNotNull(new SevenZMethodConfiguration(SevenZMethod.LZMA,
                                                           new LZMA2Options())
                             .getOptions());
    }

    @Test
    public void shouldAllowNumberForLZMA() {
        Assert.assertNotNull(new SevenZMethodConfiguration(SevenZMethod.LZMA, 42)
                             .getOptions());
    }

    @Test
    public void shouldAllowLZMA2OptionsForLZMA2() {
        Assert.assertNotNull(new SevenZMethodConfiguration(SevenZMethod.LZMA2,
                                                           new LZMA2Options())
                             .getOptions());
    }

    @Test
    public void shouldAllowNumberForLZMA2() {
        Assert.assertNotNull(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 42)
                             .getOptions());
    }

    @Test
    public void shouldAllowNumberForBzip2() {
        Assert.assertNotNull(new SevenZMethodConfiguration(SevenZMethod.BZIP2, 42)
                             .getOptions());
    }

    @Test
    public void shouldAllowNumberForDeflate() {
        Assert.assertNotNull(new SevenZMethodConfiguration(SevenZMethod.DEFLATE, 42)
                             .getOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowStringOptionsForLZMA() {
        new SevenZMethodConfiguration(SevenZMethod.LZMA, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowStringOptionsForLZMA2() {
        new SevenZMethodConfiguration(SevenZMethod.LZMA2, "");
    }

}

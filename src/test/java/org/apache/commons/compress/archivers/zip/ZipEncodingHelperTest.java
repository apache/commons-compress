/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ZipEncodingHelper}.
 */
class ZipEncodingHelperTest {

    @Test
    void testGetZipEncodingForDefault() {
        assertEquals(Charset.defaultCharset(), ((NioZipEncoding) ZipEncodingHelper.getZipEncoding(Charset.defaultCharset().name())).getCharset());
    }

    @Test
    void testGetZipEncodingForIllegalName() {
        assertEquals(Charset.defaultCharset(), ((NioZipEncoding) ZipEncodingHelper.getZipEncoding("")).getCharset());
    }

    @Test
    void testGetZipEncodingForNull() {
        assertEquals(Charset.defaultCharset(), ((NioZipEncoding) ZipEncodingHelper.getZipEncoding((Charset) null)).getCharset());
        assertEquals(Charset.defaultCharset(), ((NioZipEncoding) ZipEncodingHelper.getZipEncoding((String) null)).getCharset());
    }

    @Test
    void testGetZipEncodingForUnknown() {
        assertEquals(Charset.defaultCharset(), ((NioZipEncoding) ZipEncodingHelper.getZipEncoding("X")).getCharset());
    }
}

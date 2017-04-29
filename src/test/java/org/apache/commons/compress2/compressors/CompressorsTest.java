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
package org.apache.commons.compress2.compressors;


import org.apache.commons.compress2.formats.deflate.DeflateCompressionFormat;

import org.junit.Assert;
import org.junit.Test;

public class CompressorsTest {

    @Test
    public void shouldFindDeflateCompressionFormatByName() {
        CompressionFormat deflateFormat =
            new Compressors().getCompressionFormatByName(DeflateCompressionFormat.DEFLATE_FORMAT_NAME)
            .orElse(null);
        Assert.assertNotNull(deflateFormat);
        Assert.assertEquals(DeflateCompressionFormat.class, deflateFormat.getClass());
    }

    @Test
    public void shouldFindDeflateCompressionFormatWhenIterating() {
        shouldFind(DeflateCompressionFormat.class, new Compressors());
    }

    @Test
    public void shouldFindDeflateCompressionFormatAsWritableFormat() {
        shouldFind(DeflateCompressionFormat.class, new Compressors().getFormatsWithWriteSupport());
    }

    private void shouldFind(Class<?> compressionFormat, Iterable<CompressionFormat> i) {
        for (CompressionFormat a : i) {
            if (compressionFormat.equals(a.getClass())) {
                return;
            }
        }
        Assert.fail("Expected to find " + compressionFormat);
    }

    private void shouldNotFind(Class<?> compressionFormat, Iterable<CompressionFormat> i) {
        for (CompressionFormat a : i) {
            if (compressionFormat.equals(a.getClass())) {
                Assert.fail("Didn't expect to find " + compressionFormat);
            }
        }
    }
}

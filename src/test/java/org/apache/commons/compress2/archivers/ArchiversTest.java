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
package org.apache.commons.compress2.archivers;


import org.apache.commons.compress2.formats.ar.ArArchiveFormat;

import org.junit.Assert;
import org.junit.Test;

public class ArchiversTest {

    @Test
    public void shouldFindArArchiveFormatByName() {
        ArchiveFormat arFormat =
            new Archivers().getArchiveFormatByName(ArArchiveFormat.AR_FORMAT_NAME)
            .orElse(null);
        Assert.assertNotNull(arFormat);
        Assert.assertEquals(ArArchiveFormat.class, arFormat.getClass());
    }

    @Test
    public void shouldFindArArchiveFormatWhenIterating() {
        shouldFind(ArArchiveFormat.class, new Archivers());
    }

    @Test
    public void shouldFindArArchiveFormatAsWritableFormat() {
        shouldFind(ArArchiveFormat.class, new Archivers().getFormatsWithWriteSupport());
    }

    @Test
    public void shouldFindArArchiveFormatAsChannelWritableFormat() {
        shouldFind(ArArchiveFormat.class, new Archivers().getFormatsWithWriteSupportForNonSeekableChannels());
    }

    @Test
    public void shouldFindArArchiveFormatAsChannelReadableFormat() {
        shouldFind(ArArchiveFormat.class, new Archivers().getFormatsWithReadSupportForNonSeekableChannels());
    }

    @Test
    public void shouldNotFindArArchiveFormatAsRandomAccessFormat() {
        shouldNotFind(ArArchiveFormat.class, new Archivers().getFormatsWithRandomAccessInput());
    }

    private void shouldFind(Class<?> archiveFormat, Iterable<ArchiveFormat> i) {
        for (ArchiveFormat a : i) {
            if (archiveFormat.equals(a.getClass())) {
                return;
            }
        }
        Assert.fail("Expected to find " + archiveFormat);
    }

    private void shouldNotFind(Class<?> archiveFormat, Iterable<ArchiveFormat> i) {
        for (ArchiveFormat a : i) {
            if (archiveFormat.equals(a.getClass())) {
                Assert.fail("Didn't expect to find " + archiveFormat);
            }
        }
    }
}

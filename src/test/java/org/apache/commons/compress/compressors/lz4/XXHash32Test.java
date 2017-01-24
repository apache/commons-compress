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
package org.apache.commons.compress.compressors.lz4;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.IOUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runner.RunWith;

@RunWith(Parameterized.class)
public class XXHash32Test {

    private final File file;
    private final String expectedChecksum;

    public XXHash32Test(String fileName, String c) throws IOException {
        file = AbstractTestCase.getFile(fileName);
        expectedChecksum = c;
    }

    @Parameters
    public static Collection<Object[]> factory() {
        return Arrays.asList(new Object[][] {
            // reference checksums created with xxh32sum
            { "bla.tar", "fbb5c8d1" },
            { "bla.tar.xz", "4106a208" },
            { "8.posix.tar.gz", "9fce116a" },
        });
    }

    @Test
    public void verifyChecksum() throws IOException {
        XXHash32 h = new XXHash32();
        try (FileInputStream s = new FileInputStream(file)) {
            byte[] b = IOUtils.toByteArray(s);
            h.update(b, 0, b.length);
        }
        Assert.assertEquals("checksum for " + file.getName(), expectedChecksum, Long.toHexString(h.getValue()));
    }
}

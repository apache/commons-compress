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
package org.apache.commons.compress.archivers.cpio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CpioArchiveTest {

    @Parameters(name = "using {0}")
    public static Collection<Object[]> factory() {
        return Arrays.asList(new Object[][] {
                new Object[]  { CpioConstants.FORMAT_NEW },
                new Object[]  { CpioConstants.FORMAT_NEW_CRC },
                new Object[]  { CpioConstants.FORMAT_OLD_ASCII },
                new Object[]  { CpioConstants.FORMAT_OLD_BINARY },
            });
    }

    private final short format;

    public CpioArchiveTest(short format) {
        this.format = format;
    }

    @Test
    public void utf18RoundtripTest() throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CpioArchiveOutputStream os = new CpioArchiveOutputStream(baos, format, CpioConstants.BLOCK_SIZE,
                "UTF-16LE")) {
                CpioArchiveEntry entry = new CpioArchiveEntry(format, "T\u00e4st.txt", 4);
                if (format == CpioConstants.FORMAT_NEW_CRC) {
                    entry.setChksum(10);
                }
                os.putArchiveEntry(entry);
                os.write(new byte[] { 1, 2, 3, 4 });
                os.closeArchiveEntry();
            }
            baos.close();
            try (ByteArrayInputStream bin = new ByteArrayInputStream(baos.toByteArray());
                 CpioArchiveInputStream in = new CpioArchiveInputStream(bin, "UTF-16LE")) {
                CpioArchiveEntry entry = (CpioArchiveEntry) in.getNextEntry();
                Assert.assertNotNull(entry);
                Assert.assertEquals("T\u00e4st.txt", entry.getName());
                Assert.assertArrayEquals(new byte[] { 1, 2, 3, 4 }, IOUtils.toByteArray(in));
            }
        }
    }
}

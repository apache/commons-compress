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
package org.apache.commons.compress.compressors.bzip2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.junit.jupiter.api.Test;

public class BZip2NSelectorsOverflowTest extends AbstractTestCase {

    /**
     * See https://sourceware.org/ml/bzip2-devel/2019-q3/msg00007.html
     */
    @Test
    public void shouldDecompressBlockWithNSelectorOverflow() throws Exception {
        final File toDecompress = getFile("lbzip2_32767.bz2");
        try (final InputStream is = Files.newInputStream(toDecompress.toPath());
             final BZip2CompressorInputStream in = new BZip2CompressorInputStream(is)) {
            int l = 0;
            while (in.read() != -1) {
                l++;
            }
            assertEquals(5, l);
        }
    }
}

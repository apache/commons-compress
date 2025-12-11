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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.jupiter.api.Test;

/**
 * Tests https://issues.apache.org/jira/browse/COMPRESS-714
 */
public class ZipCompress714Test {

    @Test
    public void testIllegalPosition() throws IOException {
        final byte[] data = { 80, 75, 5, 6, -127, 80, 75, 5, 6, 7, -127, -127, -127, 80, 74, 7, 8, -127, -127, -127, -127, -127 };
        assertThrows(ArchiveException.class, () -> ZipFile.builder().setChannel(new SeekableInMemoryByteChannel(data)).get());
        assertThrows(ArchiveException.class, () -> ZipFile.builder().setByteArray(data).get());
    }
}

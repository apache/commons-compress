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

package org.apache.commons.compress.archivers;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class ListerTest {

    @Test
    public void testCompress654() throws ArchiveException, IOException {
//        Analysing ruff-aarch64-apple-darwin.tar
//        Created org.apache.commons.compress.archivers.tar.TarArchiveInputStream@17f052a3
//        ruff
//        Exception in thread "main" java.io.IOException: Truncated TAR archive
//        at org.apache.commons.compress.archivers.tar.TarArchiveInputStream.read(TarArchiveInputStream.java:694)
//        at org.apache.commons.compress.utils.IOUtils.readFully(IOUtils.java:244)
//        at org.apache.commons.compress.utils.IOUtils.skip(IOUtils.java:355)
//        at org.apache.commons.compress.archivers.tar.TarArchiveInputStream.getNextTarEntry(TarArchiveInputStream.java:451)
//        at org.apache.commons.compress.archivers.tar.TarArchiveInputStream.getNextEntry(TarArchiveInputStream.java:426)
//        at org.apache.commons.compress.archivers.tar.TarArchiveInputStream.getNextEntry(TarArchiveInputStream.java:50)
//        at org.apache.commons.compress.archivers.Lister.listStream(Lister.java:79)
//        at org.apache.commons.compress.archivers.Lister.main(Lister.java:133)
        Lister.main("src/test/resources/org/apache/commons/compress/COMPRESS-654/ruff-aarch64-apple-darwin.tar");
    }
}

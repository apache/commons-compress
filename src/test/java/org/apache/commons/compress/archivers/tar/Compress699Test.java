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

package org.apache.commons.compress.archivers.tar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.Test;

/**
 * Tests https://issues.apache.org/jira/browse/COMPRESS-699
 */
public class Compress699Test {

    @Test
    public void testTarArchive() throws Exception {
        final Path fileToTest = Paths.get("src/test/resources/org/apache/commons/compress/COMPRESS-699/icure_medical_device_dart_sdk-1.2.10.tar");
        try (BufferedInputStream fileInputStream = new BufferedInputStream(Files.newInputStream(fileToTest))) {
            assertEquals("tar", ArchiveStreamFactory.detect(fileInputStream));
        }
    }
}

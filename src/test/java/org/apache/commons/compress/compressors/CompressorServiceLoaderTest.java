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

package org.apache.commons.compress.compressors;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.compress.compressors.TestCompressorStreamProvider.InvocationConfirmationException;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

public class CompressorServiceLoaderTest {

    @Test
    public void testInputStream() {
        assertThrows(InvocationConfirmationException.class,
            () -> new CompressorStreamFactory().createCompressorInputStream("TestInput1", new ByteArrayInputStream(ArrayUtils.EMPTY_BYTE_ARRAY)));
    }

    @Test
    public void testOutputStream() {
        assertThrows(InvocationConfirmationException.class,
            () -> new CompressorStreamFactory().createCompressorOutputStream("TestOutput1", new ByteArrayOutputStream()));
    }

}

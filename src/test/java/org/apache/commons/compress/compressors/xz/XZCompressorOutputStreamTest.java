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
package org.apache.commons.compress.compressors.xz;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;


/**
 * Unit tests for class {@link XZCompressorOutputStream}.
 *
 * @date 16.06.2017
 * @see XZCompressorOutputStream
 **/
public class XZCompressorOutputStreamTest {


    @Test
    public void testWrite() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4590);
        XZCompressorOutputStream xZCompressorOutputStream = new XZCompressorOutputStream(byteArrayOutputStream);
        xZCompressorOutputStream.write(4590);

        assertEquals(24, byteArrayOutputStream.size());
        assertEquals("\uFFFD7zXZ\u0000\u0000\u0004\uFFFD\u05B4F\u0002\u0000!\u0001\u0016\u0000\u0000\u0000t/\uFFFD", byteArrayOutputStream.toString());

    }


}
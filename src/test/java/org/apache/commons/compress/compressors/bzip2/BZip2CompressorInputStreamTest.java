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

import static org.apache.commons.compress.AbstractTestCase.getFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class BZip2CompressorInputStreamTest {

    @Test(expected = IOException.class)
    public void shouldThrowAnIOExceptionWhenAppliedToAZipFile() throws Exception {
        FileInputStream in = new FileInputStream(getFile("bla.zip"));
        try {
            new BZip2CompressorInputStream(in);
        } finally {
            in.close();
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-309"
     */
    @Test
    public void readOfLength0ShouldReturn0() throws Exception {
        // Create a big random piece of data
        byte[] rawData = new byte[1048576];
        for (int i=0; i < rawData.length; ++i) {
            rawData[i] = (byte) Math.floor(Math.random()*256);
        }

        // Compress it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BZip2CompressorOutputStream bzipOut = new BZip2CompressorOutputStream(baos);
        bzipOut.write(rawData);
        bzipOut.flush();
        bzipOut.close();
        baos.flush();
        baos.close();

        // Try to read it back in
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        BZip2CompressorInputStream bzipIn = new BZip2CompressorInputStream(bais);
        byte[] buffer = new byte[1024];
        Assert.assertEquals(1024, bzipIn.read(buffer, 0, 1024));
        Assert.assertEquals(0, bzipIn.read(buffer, 1024, 0));
        Assert.assertEquals(1024, bzipIn.read(buffer, 0, 1024));
        bzipIn.close();
    }

}

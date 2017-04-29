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
package org.apache.commons.compress2.formats.ar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.commons.compress2.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class ArArchiveFormatTest {

    @Test
    public void shouldDetectFormat() throws IOException {
        Assert.assertTrue(isAr("test-archives/default.ar"));
    }

    @Test
    public void shouldRejectXMLFile() throws IOException {
        Assert.assertFalse(isAr("test1.xml"));
    }


    private boolean isAr(String file) throws IOException {
        File f = RoundTripTest.getFile(file);
        FileInputStream c = new FileInputStream(f);
        try {
            byte[] b = new byte[10];
            IOUtils.readFully(c, b);
            return new ArArchiveFormat().matches(ByteBuffer.wrap(b));
        } finally {
            c.close();
        }
    }
}

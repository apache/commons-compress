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
package org.apache.commons.compress.archivers.sevenz;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import static org.junit.Assert.*;


/**
 * Unit tests for class {@link AES256SHA256Decoder}.
 *
 * @date 26.06.2017
 * @see AES256SHA256Decoder
 **/
public class AES256SHA256DecoderTest {


    @Test
    public void testDecodeWithNonEmptyString() throws IOException {

        AES256SHA256Decoder aES256SHA256Decoder = new AES256SHA256Decoder();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(null, 3138);
        Coder coder = new Coder();
        byte[] byteArray = new byte[8];
        byteArray[1] = (byte) (-72);
        coder.properties = byteArray;
        InputStream inputStream = aES256SHA256Decoder.decode("x", bufferedInputStream, 3138, coder, coder.properties);

        ObjectInputStream objectInputStream = null;

        try {
            objectInputStream = new ObjectInputStream(inputStream);
            fail("Expecting exception: IOException");
        } catch(Throwable e) {
            assertEquals("Salt size + IV size too long in x",e.getMessage());
            assertEquals("org.apache.commons.compress.archivers.sevenz.AES256SHA256Decoder$1", e.getStackTrace()[0].getClassName());
        }

    }


}

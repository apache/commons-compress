/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.sevenz;

import org.apache.commons.compress.AbstractTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Coders.DeflateDecoder.class)
public class SevenZNativeHeapTest extends AbstractTestCase {

    @InjectMocks
    Coders.DeflateDecoder deflateDecoder;

    @Test
    public void testEndDeflaterOnCloseStream() throws Exception {
        final Deflater deflater = PowerMockito.spy(new Deflater());
        PowerMockito.whenNew(Deflater.class).withAnyArguments().thenReturn(deflater);

        final OutputStream outputStream = deflateDecoder.encode(new ByteArrayOutputStream(), 9);
        outputStream.close();

        Mockito.verify(deflater).end();
    }

    @Test
    public void testEndInflaterOnCloseStream() throws Exception {
        final Inflater inflater = PowerMockito.spy(new Inflater());
        PowerMockito.whenNew(Inflater.class).withAnyArguments().thenReturn(inflater);

        final InputStream inputStream = deflateDecoder.decode("dummy",new ByteArrayInputStream(new byte[0]),0,null,null);
        inputStream.close();

        Mockito.verify(inflater).end();
    }
}

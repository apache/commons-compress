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
package org.apache.commons.compress.compressors.deflate64;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class Deflate64CompressorInputStreamTest {
   private final HuffmanDecoder nullDecoder = null;

   @Mock
   private HuffmanDecoder decoder;

   @Test
   public void readWhenClosed() throws Exception
   {
      long size = Integer.MAX_VALUE - 1;
      Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(nullDecoder, size);
      assertEquals(-1, input.read());
      assertEquals(-1, input.read(new byte[1]));
      assertEquals(-1, input.read(new byte[1], 0, 1));
   }

   @Test
   public void properSizeWhenClosed() throws Exception
   {
      long size = Integer.MAX_VALUE - 1;
      Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(nullDecoder, size);
      assertEquals(0, input.available());
   }

   @Test
   public void properSizeWhenInRange() throws Exception
   {
      long size = Integer.MAX_VALUE - 1;
      Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder, size);
      assertEquals(size, input.available());
   }

   @Test
   public void properSizeWhenOutOfRange() throws Exception
   {
      long size = Integer.MAX_VALUE + 1L;
      Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder, size);
      assertEquals(Integer.MAX_VALUE, input.available());
   }

   @Test
   public void properSizeAfterReading() throws Exception
   {
      byte[] buf = new byte[4096];
      int offset = 1000;
      int length = 3096;

      Mockito.when(decoder.decode(buf, offset, length)).thenReturn(2048);

      long size = Integer.MAX_VALUE + 2047L;
      Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder, size);
      assertEquals(2048, input.read(buf, offset, length));
      assertEquals(Integer.MAX_VALUE - 1, input.available());
   }

   @Test
   public void closeCallsDecoder() throws Exception
   {

      Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder, 10);
      input.close();

      Mockito.verify(decoder, times(1)).close();
   }

   @Test
   public void closeIsDelegatedJustOnce() throws Exception
   {

      Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder, 10);

      input.close();
      input.close();

      Mockito.verify(decoder, times(1)).close();
   }

   @Test
   public void uncompressedBlock() throws Exception
   {
      byte[] data = {
         1, 11, 0, -12, -1,
         'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'
      };

      try (Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(new ByteArrayInputStream(data), 11);
           BufferedReader br = new BufferedReader(new InputStreamReader(input)))
      {
         assertEquals("Hello World", br.readLine());
         assertEquals(null, br.readLine());
      }
   }

}

package org.apache.commons.compress.compressors.zip;

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
public class Deflate64InputStreamTest
{
   private final HuffmanDecoder nullDecoder = null;

   @Mock
   private HuffmanDecoder decoder;

   @Test
   public void readWhenClosed() throws Exception
   {
      long size = Integer.MAX_VALUE - 1;
      Deflate64InputStream input = new Deflate64InputStream(nullDecoder, size);
      assertEquals(-1, input.read());
      assertEquals(-1, input.read(new byte[1]));
      assertEquals(-1, input.read(new byte[1], 0, 1));
   }

   @Test
   public void properSizeWhenClosed() throws Exception
   {
      long size = Integer.MAX_VALUE - 1;
      Deflate64InputStream input = new Deflate64InputStream(nullDecoder, size);
      assertEquals(0, input.available());
   }

   @Test
   public void properSizeWhenInRange() throws Exception
   {
      long size = Integer.MAX_VALUE - 1;
      Deflate64InputStream input = new Deflate64InputStream(decoder, size);
      assertEquals(size, input.available());
   }

   @Test
   public void properSizeWhenOutOfRange() throws Exception
   {
      long size = Integer.MAX_VALUE + 1L;
      Deflate64InputStream input = new Deflate64InputStream(decoder, size);
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
      Deflate64InputStream input = new Deflate64InputStream(decoder, size);
      assertEquals(2048, input.read(buf, offset, length));
      assertEquals(Integer.MAX_VALUE - 1, input.available());
   }

   @Test
   public void closeCallsDecoder() throws Exception
   {

      Deflate64InputStream input = new Deflate64InputStream(decoder, 10);
      input.close();

      Mockito.verify(decoder, times(1)).close();
   }

   @Test
   public void closeIsDelegatedJustOnce() throws Exception
   {

      Deflate64InputStream input = new Deflate64InputStream(decoder, 10);

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

      try (Deflate64InputStream input = new Deflate64InputStream(new ByteArrayInputStream(data), 11);
           BufferedReader br = new BufferedReader(new InputStreamReader(input)))
      {
         assertEquals("Hello World", br.readLine());
         assertEquals(null, br.readLine());
      }
   }

}
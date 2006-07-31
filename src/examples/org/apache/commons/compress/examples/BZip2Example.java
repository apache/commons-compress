/*
 * Copyright 2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.compress.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.CompressException;
import org.apache.commons.compress.Compressor;
import org.apache.commons.compress.CompressorFactory;
/**
 * BZIP2 .Example
 */
public class BZip2Example {
	/**
	 * 
	 */
	public BZip2Example() {
		super();
	}
	
	/**
	 * Compression
	 */
	public void compress() {
		Compressor compressor;
		try {
			compressor = CompressorFactory.getInstance("bz2");
			compressor.compressToHere( 
							new File("C:\\Temp\\test.tar"));
		} catch (CompressException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	public void compressStream() {
		InputStream in = null;
		FileOutputStream out = null;
		FileInputStream param = null;
		try {
			// Get a filestream
			param = new FileInputStream(
						new File("C:\\Temp\\test.tar"));

			// get the compressor
			Compressor compressor = CompressorFactory.getInstance("bz2");

			// compress this stream and get back an readable inputstream
			in = compressor.compress(param);
				
			// write this stream to a destination of your desire
			File f = new File("C:\\Temp\\test.tar.example.bz2");
			out = new FileOutputStream(f);
			final byte[] buffer = new byte[ 8024 ];
	        int n = 0;
	        while( -1 != ( n = in.read( buffer ) ) ) {
	            out.write( buffer, 0, n );
	        }
		} catch (CompressException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
		        in.close();
		        param.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Decompress a file.
	 */
	public void decompress() {
		Compressor decompressor;
		try {
//			decompressor = DecompressorFactory.BZIP2.getInstance();
			decompressor = CompressorFactory.getInstance("bz2");
			
			decompressor.decompressTo( 
							new File("C:\\Temp\\asf-logo-huge.tar.bz2"),
							new File("C:\\Temp\\asf-logo-huge.tar"));
		} catch (CompressException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new BZip2Example().compress();
		new BZip2Example().decompress();
	}
}







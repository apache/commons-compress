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
package org.apache.commons.compress.compressors.bzip2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.AbstractCompressor;
import org.apache.commons.compress.CompressException;
import org.apache.commons.compress.CompressUtils;
/**
 * Implementation of the Compressor Interface for BZip2. 
 * 
 * @author christian.grobmeier
 */
public class BZip2Compressor extends AbstractCompressor {
	/* Header BZ as byte-Array */
	private static final byte[] HEADER = new byte[]{(byte)'B', (byte)'Z'};
	/* Name of this implementation */
	private static final String NAME = "bz2";
	/* Default file extension*/
	private static String DEFAULT_FILE_EXTENSION = "bz2";
	
	/**
	 * Constructor. 
	 */
	public BZip2Compressor() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Compressor#compress(java.io.FileInputStream, java.io.FileOutputStream)
	 */
	public void compressTo(FileInputStream inputStream, FileOutputStream outputStream) throws CompressException {
		BZip2OutputStream outputBZStream = null;
		try {
			outputBZStream = getPackedOutput( outputStream );
			CompressUtils.copy( inputStream, outputBZStream );
		} catch (FileNotFoundException e) {
			throw new CompressException("File could not be found", e);
		} catch (IOException e) {
			throw new CompressException("An IO Exception occured", e);
		} finally {
			try {
				outputBZStream.close();
			} catch (IOException e1) {
				throw new CompressException("An IO Exception occured while closing the streams", e1);
			}
		}
	}
	
	/* 
	 * This decompress method uses a special InputStream Class for BZ2
	 * @see org.apache.commons.compress.Compressor#decompress(java.io.FileInputStream, java.io.FileOutputStream)
	 */
	public void decompressTo(FileInputStream input, FileOutputStream outputStream) 
		throws CompressException {
		BZip2InputStream inputStream = null;
		try {
			inputStream = getPackedInput( input );
			CompressUtils.copy( inputStream, outputStream );
		} catch (IOException e) {
			throw new CompressException("An I/O Exception has occured", e);
		}
	}
	
	/**
	 * Skips the 'BZ' header bytes. required by the BZip2InputStream class.
	 * @param input input stream
	 * @return {@link BZip2InputStream} instance
	 * @throws IOException if an IO error occurs
	 */
	private BZip2InputStream getPackedInput( final InputStream input )
		throws IOException {
		// skips the 'BZ' header bytes required by the BZip2InputStream class
		final int b1 = input.read();
		final int b2 = input.read();
		return new BZip2InputStream( input );
	}
	
	/**
	 * Writes a 'BZ' header to the output stream, and creates a
	 * BZip2OutputStream object ready for use, as required by the
	 * BZip2OutputStream class.
	 * 
	 * @param output {@link Output} stream to add a header to
	 * @return {@link BZip2OutputStream} ready to write to
	 * @throws IOException if an IO error occurs
	 */
	private BZip2OutputStream getPackedOutput( final OutputStream output )
		throws IOException {
		output.write( HEADER );
		return new BZip2OutputStream( output );
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Compressor#getHeader()
	 */
	public byte[] getHeader() {
		return HEADER;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Compressor#getName()
	 */
	public String getName() {
		return NAME;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.AbstractCompressor#getDefaultFileExtension()
	 */
	public String getDefaultFileExtension() {
		return DEFAULT_FILE_EXTENSION;
	}
}

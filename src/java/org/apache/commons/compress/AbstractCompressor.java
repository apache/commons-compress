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
package org.apache.commons.compress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
/**
 * AbstractCompressor handles all compression/decompression 
 * actions on an abstract basis. 
 */
public abstract class AbstractCompressor 
	extends PackableObject 
	implements Compressor {

	public AbstractCompressor() {
		super();
	}

	/**
	 * Returns a String with the default file extension
	 * for this compressor. For example, a zip-files default
	 * file extension would be "zip" (without leading dot).
	 *  
	 * @return the default file extension
	 */
	public abstract String getDefaultFileExtension();
	
    
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Compressor#compressStream(java.io.FileInputStream)
	 */
	public InputStream compress(InputStream input) throws CompressException {
		FileOutputStream outputStream = null;
		FileOutputStream tempFileOutputStream = null;
		try {
			File temp = File.createTempFile("commons_","jkt");
			tempFileOutputStream = new FileOutputStream(temp);
			compressTo(input, tempFileOutputStream);
			return new FileInputStream(temp);
		} catch (IOException e) {
			throw new CompressException("An IO Exception has occured", e);
		} finally {
			try {
				tempFileOutputStream.close();
				outputStream.close();
			} catch (IOException e) {
				throw new CompressException("An IO Exception occured while closing the streams", e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Compressor#compress(java.io.File, java.io.File)
	 */
	public void compressTo(File input, File output) throws CompressException {
		FileOutputStream outputStream = null;
		FileInputStream inputStream = null;
		try {
			outputStream = new FileOutputStream( output );
			inputStream = new FileInputStream( input );
			this.compressTo(inputStream, outputStream);
		} catch (FileNotFoundException e) {
			throw new CompressException("File not found" ,e);
		} 
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Compressor#compress(java.io.File)
	 */
	public void compressToHere(File input) throws CompressException {
		String pathToFile = input.getAbsolutePath().concat(".").concat(getDefaultFileExtension());
		File output = new File(pathToFile);
		this.compressTo(input, output);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Compressor#compressStream(java.io.File)
	 */
	public InputStream compress(File input) throws CompressException {
		try {
			return this.compress(
				new FileInputStream(input));
		} catch (FileNotFoundException e) {
			throw new CompressException("File could not be found.",e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Decompressor#decompress(java.io.File)
	 */
	public InputStream decompress(File input) throws CompressException {
		File temp;
		InputStream result;
		try {
			temp = File.createTempFile("compress_", "jkt");
			this.decompressTo(input, temp);
			result = new FileInputStream(temp);
		} catch (IOException e) {
			throw new CompressException("Error while creating a temporary file", e);
		} 
		return result; 
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Decompressor#decompress(java.io.FileInputStream)
	 */
	public InputStream decompress(InputStream input) 
		throws CompressException {
		File temp;
		InputStream result;
		try {
			temp = File.createTempFile("compress_", "jkt");
			this.decompressTo(input, new FileOutputStream(temp));
			result = new FileInputStream(temp);
		} catch (IOException e) {
			throw new CompressException("Error while creating a temporary file", e);
		} 
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Compressor#decompress(java.io.File, java.io.File)
	 */
	public void decompressTo(File input, File output) 
		throws CompressException {
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream( output );
			inputStream = new FileInputStream( input );
			decompressTo(inputStream, outputStream);
		} catch (FileNotFoundException e) {
			throw new CompressException("File could not be found", e);
		} finally {
	      	try {
				inputStream.close();
				outputStream.close();
			} catch (IOException e1) {
				throw new CompressException("An I/O Exception has occured while closing a stream", e1);
			}
		}
	}
}

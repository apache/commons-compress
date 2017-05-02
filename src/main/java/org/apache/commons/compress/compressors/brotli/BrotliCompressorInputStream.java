/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.compress.compressors.brotli;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;

/**
 * {@link FilterInputStream} implementation to decode Brotli encoded stream.
 * Library relies on <a href="https://github.com/google/brotli">Google brotli</a>
 * 
 * @since 1.14
 */
public class BrotliCompressorInputStream extends CompressorInputStream {
    
    private org.brotli.dec.BrotliInputStream decIS;

    public BrotliCompressorInputStream(InputStream in) throws IOException {
        this.decIS = new org.brotli.dec.BrotliInputStream(in);
    }

    /**
     * @return
     * @throws IOException
     * @see java.io.InputStream#available()
     */
    @Override
    public int available() throws IOException {
        return decIS.available();
    }

    /**
     * @throws IOException
     * @see org.brotli.dec.BrotliInputStream#close()
     */
    @Override
    public void close() throws IOException {
        decIS.close();
    }

    /**
     * @return
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return decIS.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public int read(byte[] b) throws IOException {
        return decIS.read(b);
    }

    /**
     * @param obj
     * @return
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return decIS.equals(obj);
    }

    /**
     * @param n
     * @return
     * @throws IOException
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(long n) throws IOException {
        return decIS.skip(n);
    }

    /**
     * @param readlimit
     * @see java.io.InputStream#mark(int)
     */
    @Override
    public void mark(int readlimit) {
        decIS.mark(readlimit);
    }

    /**
     * @return
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return decIS.markSupported();
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        final int ret = decIS.read();
        count(ret == -1 ? 0 : 1);
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        final int ret = decIS.read(buf, off, len);
        count(ret);
        return ret;
    }

    /**
     * @return
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return decIS.toString();
    }

    /**
     * @throws IOException
     * @see java.io.InputStream#reset()
     */
    @Override
    public void reset() throws IOException {
        decIS.reset();
    }
    

    /**
     * There is no magic for Brotli
     * 
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true
     */
    static boolean matches(final byte[] signature, final int length) {
        return true;
    }
}

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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.sevenz.Coders.DeflateDecoder;
import org.apache.commons.compress.archivers.sevenz.Coders.DeflateDecoder.DeflateDecoderInputStream;
import org.apache.commons.compress.archivers.sevenz.Coders.DeflateDecoder.DeflateDecoderOutputStream;
import org.junit.Test;

public class SevenZNativeHeapTest extends AbstractTestCase {


    @Test
    public void testEndDeflaterOnCloseStream() throws Exception {
        Coders.DeflateDecoder deflateDecoder = new DeflateDecoder();

        final DeflateDecoderOutputStream outputStream =
            (DeflateDecoderOutputStream) deflateDecoder.encode(new ByteArrayOutputStream(), 9);
        DelegatingDeflater delegatingDeflater = new DelegatingDeflater(outputStream.deflater);
        outputStream.deflater = delegatingDeflater;
        outputStream.close();
        assertTrue(delegatingDeflater.isEnded.get());

    }

    @Test
    public void testEndInflaterOnCloseStream() throws Exception {
        Coders.DeflateDecoder deflateDecoder = new DeflateDecoder();
        final DeflateDecoderInputStream inputStream =
            (DeflateDecoderInputStream) deflateDecoder.decode("dummy",new ByteArrayInputStream(new byte[0]),0,null,null);
        DelegatingInflater delegatingInflater = new DelegatingInflater(inputStream.inflater);
        inputStream.inflater = delegatingInflater;
        inputStream.close();

        assertTrue(delegatingInflater.isEnded.get());
    }

    private class DelegatingInflater extends Inflater {

        private final Inflater inflater;

        public DelegatingInflater(Inflater inflater) {
            this.inflater = inflater;
        }
        AtomicBoolean isEnded = new AtomicBoolean();

        @Override
        public void end() {
            isEnded.set(true);
            inflater.end();
        }

        @Override
        public void setInput(byte[] b, int off, int len) {
            inflater.setInput(b, off, len);
        }

        @Override
        public void setInput(byte[] b) {
            inflater.setInput(b);
        }

        @Override
        public void setDictionary(byte[] b, int off, int len) {
            inflater.setDictionary(b, off, len);
        }

        @Override
        public void setDictionary(byte[] b) {
            inflater.setDictionary(b);
        }

        @Override
        public int getRemaining() {
            return inflater.getRemaining();
        }

        @Override
        public boolean needsInput() {
            return inflater.needsInput();
        }

        @Override
        public boolean needsDictionary() {
            return inflater.needsDictionary();
        }

        @Override
        public boolean finished() {
            return inflater.finished();
        }

        @Override
        public int inflate(byte[] b, int off, int len) throws DataFormatException {
            return inflater.inflate(b, off, len);
        }

        @Override
        public int inflate(byte[] b) throws DataFormatException {
            return inflater.inflate(b);
        }

        @Override
        public int getAdler() {
            return inflater.getAdler();
        }

        @Override
        public int getTotalIn() {
            return inflater.getTotalIn();
        }

        @Override
        public long getBytesRead() {
            return inflater.getBytesRead();
        }

        @Override
        public int getTotalOut() {
            return inflater.getTotalOut();
        }

        @Override
        public long getBytesWritten() {
            return inflater.getBytesWritten();
        }

        @Override
        public void reset() {
            inflater.reset();
        }

    }

    private class DelegatingDeflater extends Deflater {

        private final Deflater deflater;

        public DelegatingDeflater(Deflater deflater) {
            this.deflater = deflater;
        }

        AtomicBoolean isEnded = new AtomicBoolean();

        @Override
        public void end() {
            isEnded.set(true);
            deflater.end();
        }

        @Override
        public void setInput(byte[] b, int off, int len) {
            deflater.setInput(b, off, len);
        }

        @Override
        public void setInput(byte[] b) {
            deflater.setInput(b);
        }

        @Override
        public void setDictionary(byte[] b, int off, int len) {
            deflater.setDictionary(b, off, len);
        }

        @Override
        public void setDictionary(byte[] b) {
            deflater.setDictionary(b);
        }

        @Override
        public void setStrategy(int strategy) {
            deflater.setStrategy(strategy);
        }

        @Override
        public void setLevel(int level) {
            deflater.setLevel(level);
        }

        @Override
        public boolean needsInput() {
            return deflater.needsInput();
        }

        @Override
        public void finish() {
            deflater.finish();
        }

        @Override
        public boolean finished() {
            return deflater.finished();
        }

        @Override
        public int deflate(byte[] b, int off, int len) {
            return deflater.deflate(b, off, len);
        }

        @Override
        public int deflate(byte[] b) {
            return deflater.deflate(b);
        }

        @Override
        public int deflate(byte[] b, int off, int len, int flush) {
            return deflater.deflate(b, off, len, flush);
        }

        @Override
        public int getAdler() {
            return deflater.getAdler();
        }

        @Override
        public int getTotalIn() {
            return deflater.getTotalIn();
        }

        @Override
        public long getBytesRead() {
            return deflater.getBytesRead();
        }

        @Override
        public int getTotalOut() {
            return deflater.getTotalOut();
        }

        @Override
        public long getBytesWritten() {
            return deflater.getBytesWritten();
        }

        @Override
        public void reset() {
            deflater.reset();
        }


    }
}

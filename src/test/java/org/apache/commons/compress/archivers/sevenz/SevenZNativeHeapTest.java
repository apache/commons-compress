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

import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.apache.commons.compress.utils.ByteUtils;
import org.junit.jupiter.api.Test;

public class SevenZNativeHeapTest extends AbstractTestCase {

    private static class DelegatingDeflater extends Deflater {

        private final Deflater deflater;

        final AtomicBoolean isEnded = new AtomicBoolean();

        public DelegatingDeflater(final Deflater deflater) {
            this.deflater = deflater;
        }

        @Override
        public int deflate(final byte[] b) {
            return deflater.deflate(b);
        }

        @Override
        public int deflate(final byte[] b, final int off, final int len) {
            return deflater.deflate(b, off, len);
        }

        @Override
        public int deflate(final byte[] b, final int off, final int len, final int flush) {
            return deflater.deflate(b, off, len, flush);
        }

        @Override
        public void end() {
            isEnded.set(true);
            deflater.end();
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
        public int getAdler() {
            return deflater.getAdler();
        }

        @Override
        public long getBytesRead() {
            return deflater.getBytesRead();
        }

        @Override
        public long getBytesWritten() {
            return deflater.getBytesWritten();
        }

        @Override
        public int getTotalIn() {
            return deflater.getTotalIn();
        }

        @Override
        public int getTotalOut() {
            return deflater.getTotalOut();
        }

        @Override
        public boolean needsInput() {
            return deflater.needsInput();
        }

        @Override
        public void reset() {
            deflater.reset();
        }

        @Override
        public void setDictionary(final byte[] b) {
            deflater.setDictionary(b);
        }

        @Override
        public void setDictionary(final byte[] b, final int off, final int len) {
            deflater.setDictionary(b, off, len);
        }

        @Override
        public void setInput(final byte[] b) {
            deflater.setInput(b);
        }

        @Override
        public void setInput(final byte[] b, final int off, final int len) {
            deflater.setInput(b, off, len);
        }

        @Override
        public void setLevel(final int level) {
            deflater.setLevel(level);
        }

        @Override
        public void setStrategy(final int strategy) {
            deflater.setStrategy(strategy);
        }


    }

    private static class DelegatingInflater extends Inflater {

        private final Inflater inflater;

        final AtomicBoolean isEnded = new AtomicBoolean();
        public DelegatingInflater(final Inflater inflater) {
            this.inflater = inflater;
        }

        @Override
        public void end() {
            isEnded.set(true);
            inflater.end();
        }

        @Override
        public boolean finished() {
            return inflater.finished();
        }

        @Override
        public int getAdler() {
            return inflater.getAdler();
        }

        @Override
        public long getBytesRead() {
            return inflater.getBytesRead();
        }

        @Override
        public long getBytesWritten() {
            return inflater.getBytesWritten();
        }

        @Override
        public int getRemaining() {
            return inflater.getRemaining();
        }

        @Override
        public int getTotalIn() {
            return inflater.getTotalIn();
        }

        @Override
        public int getTotalOut() {
            return inflater.getTotalOut();
        }

        @Override
        public int inflate(final byte[] b) throws DataFormatException {
            return inflater.inflate(b);
        }

        @Override
        public int inflate(final byte[] b, final int off, final int len) throws DataFormatException {
            return inflater.inflate(b, off, len);
        }

        @Override
        public boolean needsDictionary() {
            return inflater.needsDictionary();
        }

        @Override
        public boolean needsInput() {
            return inflater.needsInput();
        }

        @Override
        public void reset() {
            inflater.reset();
        }

        @Override
        public void setDictionary(final byte[] b) {
            inflater.setDictionary(b);
        }

        @Override
        public void setDictionary(final byte[] b, final int off, final int len) {
            inflater.setDictionary(b, off, len);
        }

        @Override
        public void setInput(final byte[] b) {
            inflater.setInput(b);
        }

        @Override
        public void setInput(final byte[] b, final int off, final int len) {
            inflater.setInput(b, off, len);
        }

    }

    @Test
    public void testEndDeflaterOnCloseStream() throws Exception {
        final Coders.DeflateDecoder deflateDecoder = new DeflateDecoder();
        final DelegatingDeflater delegatingDeflater;
        try (final DeflateDecoderOutputStream outputStream = (DeflateDecoderOutputStream) deflateDecoder
            .encode(new ByteArrayOutputStream(), 9)) {
            delegatingDeflater = new DelegatingDeflater(outputStream.deflater);
            outputStream.deflater = delegatingDeflater;
        }
        assertTrue(delegatingDeflater.isEnded.get());

    }

    @Test
    public void testEndInflaterOnCloseStream() throws Exception {
        final Coders.DeflateDecoder deflateDecoder = new DeflateDecoder();
        final DelegatingInflater delegatingInflater;
        try (final DeflateDecoderInputStream inputStream = (DeflateDecoderInputStream) deflateDecoder.decode("dummy",
            new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY), 0, null, null, Integer.MAX_VALUE)) {
            delegatingInflater = new DelegatingInflater(inputStream.inflater);
            inputStream.inflater = delegatingInflater;
        }

        assertTrue(delegatingInflater.isEnded.get());
    }
}

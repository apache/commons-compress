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

package org.apache.commons.compress.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.junit.Assert;
import org.junit.Test;

public class IOUtilsTest {

    private interface StreamWrapper {
        InputStream wrap(InputStream toWrap);
    }

    @Test
    public void skipUsingSkip() throws Exception {
        skip(new StreamWrapper() {
                @Override
                public InputStream wrap(final InputStream toWrap) {
                    return toWrap;
                }
            });
    }

    @Test
    public void skipUsingRead() throws Exception {
        skip(new StreamWrapper() {
                @Override
                public InputStream wrap(final InputStream toWrap) {
                    return new FilterInputStream(toWrap) {
                        @Override
                        public long skip(final long s) {
                            return 0;
                        }
                    };
                }
            });
    }

    @Test
    public void skipUsingSkipAndRead() throws Exception {
        skip(new StreamWrapper() {
                @Override
                public InputStream wrap(final InputStream toWrap) {
                    return new FilterInputStream(toWrap) {
                        boolean skipped;
                        @Override
                        public long skip(final long s) throws IOException {
                            if (!skipped) {
                                toWrap.skip(5);
                                skipped = true;
                                return 5;
                            }
                            return 0;
                        }
                    };
                }
            });
    }

    @Test
    public void readFullyOnChannelReadsFully() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(20);
        final byte[] source = new byte[20];
        for (byte i = 0; i < 20; i++) {
            source[i] = i;
        }
        readFully(source, b);
        Assert.assertArrayEquals(source, b.array());
    }

    @Test(expected = EOFException.class)
    public void readFullyOnChannelThrowsEof() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(21);
        final byte[] source = new byte[20];
        for (byte i = 0; i < 20; i++) {
            source[i] = i;
        }
        readFully(source, b);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyThrowsOnZeroBufferSize() throws IOException {
        IOUtils.copy(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), 0);
    }

    private static void readFully(final byte[] source, ByteBuffer b) throws IOException {
        IOUtils.readFully(new ReadableByteChannel() {
                private int idx;
                @Override
                public int read(ByteBuffer buf) {
                    if (idx >= source.length) {
                        return -1;
                    }
                    buf.put(source[idx++]);
                    return 1;
                }
                @Override
                public void close() { }
                @Override
                public boolean isOpen() {
                    return true;
                }
            }, b);
    }

    private void skip(final StreamWrapper wrapper) throws Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
            });
        final InputStream sut = wrapper.wrap(in);
        Assert.assertEquals(10, IOUtils.skip(sut, 10));
        Assert.assertEquals(11, sut.read());
    }
}

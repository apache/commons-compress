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
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class IOUtilsTest {

    private interface StreamWrapper {
        InputStream wrap(InputStream toWrap);
    }

    @Test
    public void skipUsingSkip() throws Exception {
        skip(new StreamWrapper() {
                public InputStream wrap(InputStream toWrap) {
                    return toWrap;
                }
            });
    }

    @Test
    public void skipUsingRead() throws Exception {
        skip(new StreamWrapper() {
                public InputStream wrap(InputStream toWrap) {
                    return new FilterInputStream(toWrap) {
                        @Override
                        public long skip(long s) {
                            return 0;
                        }
                    };
                }
            });
    }

    @Test
    public void skipUsingSkipAndRead() throws Exception {
        skip(new StreamWrapper() {
                public InputStream wrap(final InputStream toWrap) {
                    return new FilterInputStream(toWrap) {
                        boolean skipped;
                        @Override
                        public long skip(long s) throws IOException {
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

    private void skip(StreamWrapper wrapper) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
            });
        InputStream sut = wrapper.wrap(in);
        Assert.assertEquals(10, IOUtils.skip(sut, 10));
        Assert.assertEquals(11, sut.read());
    }

}

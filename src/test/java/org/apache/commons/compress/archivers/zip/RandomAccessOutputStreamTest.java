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
 */
package org.apache.commons.compress.archivers.zip;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.apache.commons.compress.AbstractTempDirTest;
import org.junit.jupiter.api.Test;


public class RandomAccessOutputStreamTest extends AbstractTempDirTest {

    @Test
    public void testWrite() throws IOException {
        RandomAccessOutputStream delegate = mock(RandomAccessOutputStream.class);

        RandomAccessOutputStream stream = new RandomAccessOutputStream() {
            @Override
            public long position() throws IOException {
                return delegate.position();
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                delegate.write(b, off, len);
            }

            @Override
            void writeFullyAt(byte[] b, int off, int len, long position) throws IOException {
                delegate.writeFullyAt(b, off, len, position);
            }
        };

        stream.write('\n');

        verify(delegate, times(1))
                .write(any(), eq(0), eq(1));
    }
}

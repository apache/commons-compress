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

package org.apache.commons.compress.archivers.zip;

import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Not really a test for Commons Compress but rather one for the JDK.
 */
@Ignore
public class DeflaterInflaterTest {
    /**
     * Verify Deflater.getBytesRead returns a number &gt; 0xFFFFFFFF
     * if the uncompressed size is bigger than that.
     */
    @Test public void deflaterBytesRead() throws Throwable {
        Deflater def = new Deflater();
        try {
            byte[] in = new byte[4096];
            byte[] out = new byte[8192];
            final int max = 1024 * 1024 + 1;
            for (int i = 0; i < max; i++) {
                def.setInput(in);
                while (!def.needsInput()) {
                    def.deflate(out, 0, out.length);
                }
            }
            def.finish();
            while (!def.finished()) {
                def.deflate(out, 0, out.length);
            }
            assertEquals(0x100001000L, def.getBytesRead());
        } finally {
            def.end();
        }
    }

    /**
     * Verify Inflater.getBytesWritten returns a number &gt;
     * 0xFFFFFFFF if the uncompressed size is bigger than that.
     */
    @Test public void inflaterBytesWritten() throws Throwable {
        Deflater def = new Deflater();
        Inflater inf = new Inflater();
        try {
            byte[] in = new byte[4096];
            byte[] out = new byte[8192];
            byte[] out2 = new byte[8192];
            final int max = 1024 * 1024 + 1;
            for (int i = 0; i < max; i++) {
                def.setInput(in);
                while (!def.needsInput()) {
                    int len = def.deflate(out, 0, out.length);
                    if (len > 0) {
                        inf.setInput(out, 0, len);
                        while (!inf.needsInput()) {
                            inf.inflate(out2, 0, out2.length);
                        }
                    }
                }
            }
            def.finish();
            while (!def.finished()) {
                int len = def.deflate(out, 0, out.length);
                if (len > 0) {
                    inf.setInput(out, 0, len);
                    while (!inf.needsInput()) {
                        inf.inflate(out2, 0, out2.length);
                    }
                }
            }
            def.end();
            while (!inf.finished()) {
                inf.inflate(out2, 0, out2.length);
            }
            assertEquals(0x100001000L, inf.getBytesWritten());
        } finally {
            inf.end();
        }
    }
}

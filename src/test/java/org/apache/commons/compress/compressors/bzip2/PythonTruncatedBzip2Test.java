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
package org.apache.commons.compress.compressors.bzip2;

import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Testcase porting a test from Python's testsuite.
 * @see "https://issues.apache.org/jira/browse/COMPRESS-253"
 */
public class PythonTruncatedBzip2Test {

    private static String TEXT = "root:x:0:0:root:/root:/bin/bash\nbin:x:1:1:bin:/bin:\ndaemon:x:2:2:daemon:/sbin:\nadm:x:3:4:adm:/var/adm:\nlp:x:4:7:lp:/var/spool/lpd:\nsync:x:5:0:sync:/sbin:/bin/sync\nshutdown:x:6:0:shutdown:/sbin:/sbin/shutdown\nhalt:x:7:0:halt:/sbin:/sbin/halt\nmail:x:8:12:mail:/var/spool/mail:\nnews:x:9:13:news:/var/spool/news:\nuucp:x:10:14:uucp:/var/spool/uucp:\noperator:x:11:0:operator:/root:\ngames:x:12:100:games:/usr/games:\ngopher:x:13:30:gopher:/usr/lib/gopher-data:\nftp:x:14:50:FTP User:/var/ftp:/bin/bash\nnobody:x:65534:65534:Nobody:/home:\npostfix:x:100:101:postfix:/var/spool/postfix:\nniemeyer:x:500:500::/home/niemeyer:/bin/bash\npostgres:x:101:102:PostgreSQL Server:/var/lib/pgsql:/bin/bash\nmysql:x:102:103:MySQL server:/var/lib/mysql:/bin/bash\nwww:x:103:104::/var/www:/bin/false\n";

    private static byte[] DATA;
    private static byte[] TRUNCATED_DATA;
    private ReadableByteChannel bz2Channel;

    @BeforeClass
    public static void initializeTestData() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BZip2CompressorOutputStream bz2out = new BZip2CompressorOutputStream(out);
        bz2out.write(TEXT.getBytes(), 0, TEXT.getBytes().length);
        bz2out.close();
        DATA = out.toByteArray();

        // Drop the eos_magic field (6 bytes) and CRC (4 bytes).
        TRUNCATED_DATA = copyOfRange(DATA, 0, DATA.length - 10);
    }

    @Before
    public void initializeChannel() throws IOException {
        InputStream source = new ByteArrayInputStream(TRUNCATED_DATA);
        this.bz2Channel = makeBZ2C(source);
    }

    @After
    public void closeChannel() throws IOException {
        bz2Channel.close();
        bz2Channel = null;
    }

    @Test(expected = IOException.class)
    public void testTruncatedData() throws IOException {
        //with BZ2File(self.filename) as f:
        //    self.assertRaises(EOFError, f.read)
        System.out.println("Attempt to read the whole thing in, should throw ...");
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        bz2Channel.read(buffer);
    }

    @Test
    public void testPartialReadTruncatedData() throws IOException {
        //with BZ2File(self.filename) as f:
        //    self.assertEqual(f.read(len(self.TEXT)), self.TEXT)
        //    self.assertRaises(EOFError, f.read, 1)

        final int length = TEXT.length();
        ByteBuffer buffer = ByteBuffer.allocate(length);
        bz2Channel.read(buffer);

        assertArrayEquals(copyOfRange(TEXT.getBytes(), 0, length),
                buffer.array());

        // subsequent read should throw
        buffer = ByteBuffer.allocate(1);
        try {
            bz2Channel.read(buffer);
            Assert.fail("The read should have thrown.");
        } catch (IOException e) {
            // pass
        }
    }

    private static ReadableByteChannel makeBZ2C(InputStream source) throws IOException {
        BufferedInputStream bin = new BufferedInputStream(source);
        BZip2CompressorInputStream bZin = new BZip2CompressorInputStream(bin, true);

        return Channels.newChannel(bZin);
    }

    // Helper method since Arrays#copyOfRange is Java 1.6+
    // Does not check parameters, so may fail if they are incompatible
    private static byte[] copyOfRange(byte[] original, int from, int to) {
        int length = to - from;
        byte buff[] = new byte[length];
        System.arraycopy(original, from, buff, 0, length);
        return buff;
    }
}

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
package org.apache.commons.compress.performance;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStreamBreak;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStreamXenoAmess;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Test to show whether using BitSet for removeAll() methods is faster than using HashSet.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
public class BZip2CompressorOutputStreamXenoAmessTest {

    private static final String TEXT = "root:x:0:0:root:/root:/bin/bash\nbin:x:1:1:bin:/bin:\ndaemon:x:2:2:daemon" +
            ":/sbin:\nadm:x:3:4:adm:/var/adm:\nlp:x:4:7:lp:/var/spool/lpd:\nsync:x:5:0:sync:/sbin:/bin/sync\nshutdown" +
            ":x:6:0:shutdown:/sbin:/sbin/shutdown\nhalt:x:7:0:halt:/sbin:/sbin/halt\nmail:x:8:12:mail:/var/spool/mail" +
            ":\nnews:x:9:13:news:/var/spool/news:\nuucp:x:10:14:uucp:/var/spool/uucp:\noperator:x:11:0:operator:/root" +
            ":\ngames:x:12:100:games:/usr/games:\ngopher:x:13:30:gopher:/usr/lib/gopher-data:\nftp:x:14:50:FTP " +
            "User:/var/ftp:/bin/bash\nnobody:x:65534:65534:Nobody:/home:\npostfix:x:100:101:postfix:/var/spool" +
            "/postfix:\nniemeyer:x:500:500::/home/niemeyer:/bin/bash\npostgres:x:101:102:PostgreSQL " +
            "Server:/var/lib/pgsql:/bin/bash\nmysql:x:102:103:MySQL " +
            "server:/var/lib/mysql:/bin/bash\nwww:x:103:104::/var/www:/bin/false\n";

    @Benchmark
    public void testOld() throws IOException {
        final ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        final BZip2CompressorOutputStream bz2out1 = new BZip2CompressorOutputStream(out1);
        bz2out1.write(TEXT.getBytes(), 0, TEXT.getBytes().length);
        bz2out1.close();
    }

    @Benchmark
    public void testOldWithBreak() throws IOException {
        final ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        final BZip2CompressorOutputStreamBreak bz2out1 = new BZip2CompressorOutputStreamBreak(out1);
        bz2out1.write(TEXT.getBytes(), 0, TEXT.getBytes().length);
        bz2out1.close();
    }

    @Benchmark
    public void testXenoAmess() throws IOException {
        final ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        final BZip2CompressorOutputStreamXenoAmess bz2out2 = new BZip2CompressorOutputStreamXenoAmess(out2);
        bz2out2.write(TEXT.getBytes(), 0, TEXT.getBytes().length);
        bz2out2.close();
    }

}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * <p>
 * Provides stream classes for compressing and decompressing streams using the Pack200 algorithm used to compress Java archives.
 * </p>
 * <p>
 * The streams of this package only work on JAR archives, i.e. a {@link org.apache.commons.compress.compressors.pack200.Pack200CompressorOutputStream
 * Pack200CompressorOutputStream} expects to be wrapped around a stream that a valid JAR archive will be written to and a
 * {@link org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream Pack200CompressorInputStream} provides a stream to read from a JAR
 * archive.
 * </p>
 * <p>
 * JAR archives compressed with Pack200 will in general be different from the original archive when decompressed again. For details see the
 * <a href="https://download.oracle.com/javase/1.5.0/docs/api/java/util/jar/Pack200.html">API documentation of Pack200</a>.
 * </p>
 * <p>
 * The streams of this package work on non-deflated streams, i.e. archives like those created with the {@code --no-gzip} option of the JDK's
 * {@code pack200} command line tool. If you want to work on deflated streams you must use an additional stream layer - for example by using Apache Commons
 * Compress' gzip package.
 * </p>
 * <p>
 * The Pack200 API provided by the Java class library doesn't lend itself to real stream processing. {@code Pack200CompressorInputStream} will uncompress
 * its input immediately and then provide an {@code InputStream} to a cached result. Likewise {@code Pack200CompressorOutputStream} will not write
 * anything to the given OutputStream until {@code finish} or {@code close} is called - at which point the cached output written so far gets
 * compressed.
 * </p>
 * <p>
 * Two different caching modes are available - "in memory", which is the default, and "temporary file". By default data is cached in memory but you should
 * switch to the temporary file option if your archives are really big.
 * </p>
 * <p>
 * Given there always is an intermediate result the {@code getBytesRead} and {@code getCount} methods of {@code Pack200CompressorInputStream} are
 * meaningless (read from the real stream or from the intermediate result?) and always return 0.
 * </p>
 * <p>
 * During development of the initial version several attempts have been made to use a real streaming API based for example on
 * {@code Piped(In|Out)putStream} or explicit stream pumping like Commons Exec's {@code InputStreamPumper} but they have all failed because they rely
 * on the output end to be consumed completely or else the {@code (un)pack} will block forever. Especially for {@code Pack200InputStream} it is very
 * likely that it will be wrapped in a {@code ZipArchiveInputStream} which will never read the archive completely as it is not interested in the ZIP
 * central directory data at the end of the JAR archive.
 * </p>
 */
package org.apache.commons.compress.compressors.pack200;

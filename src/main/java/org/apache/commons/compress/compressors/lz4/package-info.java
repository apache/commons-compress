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

/**
 * Provides stream classes for the <a href="http://lz4.github.io/lz4/">LZ4</a> algorithm.
 * <p>
 * The block LZ4 format which only contains the compressed data is supported by the <code>BlockLZ4Compressor*putStream</code> classes while the frame format is
 * implemented by <code>FramedLZ4Compressor*putStream</code>. The implementation in Commons Compress is based on the specifications "Last revised: 2015-03-26"
 * for the block format and version "1.5.1 (31/03/2015)" for the frame format.
 * </p>
 * <p>
 * Only the frame format can be auto-detected this means you have to speficy the format explicitly if you want to read a block LZ4 stream via
 * <code>CompressorStreamFactory</code>.
 * </p>
 */
package org.apache.commons.compress.compressors.lz4;

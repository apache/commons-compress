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
 * Provides stream classes for the <a href="https://github.com/google/snappy">Snappy</a> algorithm.
 * <p>
 * The raw Snappy format which only contains the compressed data is supported by the <code>SnappyCompressor*putStream</code> classes while the so called
 * "framing format" is implemented by <code>FramedSnappyCompressor*putStream</code>. Note there have been different versions of the framing format
 * specification, the implementation in Commons Compress is based on the specification "Last revised: 2013-10-25".
 * </p>
 * <p>
 * Only the "framing format" can be auto-detected this means you have to speficy the format explicitly if you want to read a "raw" Snappy stream via
 * <code>CompressorStreamFactory</code>.
 * </p>
 */
package org.apache.commons.compress.compressors.snappy;

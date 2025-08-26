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

package org.apache.commons.compress.compressors.bzip2;

/**
 * Constants for both the compress and decompress BZip2 classes.
 */
interface BZip2Constants {

    /**
     * Constant {@value}.
     */
    int BASEBLOCKSIZE = 100_000;

    /**
     * Constant {@value}.
     */
    int MAX_ALPHA_SIZE = 258;

    /**
     * Maximum allowed length of a Huffman code in BZip2.
     * <p>
     *     In the original bzip2 C implementation, a defensive limit of 23 is set to prevent buffer overflows
     *     (see <a href="https://github.com/libarchive/bzip2/blob/master/bzlib_private.h">{@code bzip_private.h}</a>).
     *     However, the actual enforced maximum during decompression is 20
     *     (see <a href="https://github.com/libarchive/bzip2/blob/master/decompress.c">BZ2_decompress in {@code decompress.c}</a>),
     *     and since version 1.0.6, compression uses a maximum of 17.
     * </p>
     * <p>
     *     The Java implementation does not require the higher defensive limit, so this constant is set to the true maximum of 20.
     * </p>
     */
    int MAX_CODE_LEN = 20;

    /**
     * Constant {@value}.
     */
    int RUNA = 0;

    /**
     * Constant {@value}.
     */
    int RUNB = 1;

    /**
     * Constant {@value}.
     */
    int N_GROUPS = 6;

    /**
     * Constant {@value}.
     */
    int G_SIZE = 50;

    /**
     * Constant {@value}.
     */
    int N_ITERS = 4;

    /**
     * Constant {@value}.
     */
    int MAX_SELECTORS = 2 + 900_000 / G_SIZE;

    /**
     * Constant {@value}.
     */
    int NUM_OVERSHOOT_BYTES = 20;

}

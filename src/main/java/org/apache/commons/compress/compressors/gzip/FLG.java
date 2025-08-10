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

package org.apache.commons.compress.compressors.gzip;

/**
 * Declares constants for the header {@code FLG} (FLaGs) byte from <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File
 * Format Specification May 1996</a> section 2.3.1. Member header and trailer.
 * <p>
 * The {@code FLG} (FLaGs) byte is divided into individual bits as follows:
 * </p>
 *
 * <pre>
 * FLG (FLaGs)
 *
 *     bit 0   FTEXT
 *     bit 1   FHCRC
 *     bit 2   FEXTRA
 *     bit 3   FNAME
 *     bit 4   FCOMMENT
 *     bit 5   reserved
 *     bit 6   reserved
 *     bit 7   reserved
 * </pre>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File Format Specification section 2.3.1</a>
 */
final class FLG {

    /**
     * Header flag indicating a comment follows the header.
     * <p>
     * If set, a zero-terminated file comment is present, not interpreted, and only intended for human consumption. The comment must contain ISO 8859-1
     * (LATIN-1) characters. Line breaks should be encoded as a single line feed character (10 decimal).
     * </p>
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File Format Specification section 2.3.1</a>
     */
    static final int FCOMMENT = 0x10;

    /**
     * Header flag indicating an EXTRA subfields collection follows the header.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File Format Specification section 2.3.1</a>
     */
    static final int FEXTRA = 0x04;

    /**
     * Header flag indicating a header CRC follows the header.
     * <p>
     * If FHCRC is set, then a CRC16 for the gzip header is present, immediately before the compressed data. The CRC16 consists of the two least significant
     * bytes of the CRC32 for all bytes of the gzip header up to and not including the CRC16.
     * </p>
     * <p>
     * The FHCRC bit was never set by versions of gzip up to 1.2.4, even though it was documented with a different meaning in gzip 1.2.4.
     * </p>
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File Format Specification section 2.3.1</a>
     */
    static final int FHCRC = 0x02;

    /**
     * Header flag indicating a file name follows the header.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File Format Specification section 2.3.1</a>
     */
    static final int FNAME = 0x08;

    /**
     * Reserved {@code FLG} bits must be zero.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File Format Specification section 2.3.1</a>
     */
    static final int FRESERVED = 0xE0;

    private FLG() {
        // No instance.
    }
}

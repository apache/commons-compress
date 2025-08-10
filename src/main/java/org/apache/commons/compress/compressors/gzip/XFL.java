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
 * Declares constants for the header {@code XFL} (eXtra FLags) byte from <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP
 * File Format Specification May 1996</a> section 2.3.1. Member header and trailer.
 * <p>
 * These flags are available for use by specific compression methods. The "deflate" method (CM = 8) sets these flags as follows:
 * </p>
 *
 * <pre>
 *   XFL = 2 - compressor used maximum compression,
 *             slowest algorithm
 *   XFL = 4 - compressor used fastest algorithm
 * </pre>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File Format Specification section 2.3.1</a>
 */
final class XFL {

    /**
     * Member header XFL (eXtra FLags) when the "deflate" method (CM = 8) is set, then XFL = 2 means the compressor used maximum compression (slowest
     * algorithm).
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File Format Specification section 2.3.1</a>
     */
    static final byte MAX_COMPRESSION = 2;

    /**
     * Member header XFL (eXtra FLags) when the "deflate" method (CM = 8) is set, then XFL = 4 means the compressor used the fastest algorithm.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#section-2.3.1">RFC 1952 GZIP File Format Specification section 2.3.1</a>
     */
    static final byte MAX_SPEED = 4;

    static final byte UNKNOWN = 0;

    private XFL() {
        // No instances.
    }
}

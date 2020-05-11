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
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.sevenz;

import java.io.IOException;
import java.util.zip.CRC32;

/**
 * Represents a buffer for a {@link SevenZFile} header.
 *
 * @since 1.21
 */
interface HeaderBuffer {
    void get(byte[] dst) throws IOException;

    int getInt() throws IOException;

    long getLong() throws IOException;

    int getUnsignedByte() throws IOException;

    boolean hasCRC();

    CRC32 getCRC() throws IOException;

    long skipBytesFully(long bytesToSkip) throws IOException;
}

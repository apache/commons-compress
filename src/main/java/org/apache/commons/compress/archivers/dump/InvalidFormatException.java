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
package org.apache.commons.compress.archivers.dump;

/**
 * Invalid Format Exception. There was an error decoding a tape segment header.
 */
public class InvalidFormatException extends DumpArchiveException {
    private static final long serialVersionUID = 1L;

    /**
     * The offset where the problem was detected.
     */
    protected long offset;

    /**
     * Constructs a {@code InvalidFormatException}.
     */
    public InvalidFormatException() {
        super("there was an error decoding a tape segment");
    }

    /**
     * Constructs a {@code InvalidFormatException}.
     *
     * @param offset offset where the problem was detected.
     */
    public InvalidFormatException(final long offset) {
        super("there was an error decoding a tape segment header at offset " + offset + ".");
        this.offset = offset;
    }

    /**
     * Gets the offset where the problem was detected.
     *
     * @return the offset where the problem was detected.
     */
    public long getOffset() {
        return offset;
    }
}

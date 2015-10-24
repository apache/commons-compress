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
package org.apache.commons.compress2.archivers.spi;

import org.apache.commons.compress2.archivers.ArchiveInput;
import org.apache.commons.compress2.archivers.ArchiveEntry;

/**
 * Base class implementations may use.
 * @NotThreadSafe
 */
public abstract class AbstractArchiveInput<A extends ArchiveEntry> implements ArchiveInput<A> {

    /** holds the number of bytes read from this channel */
    private long bytesRead = 0;

    @Override
    public long getBytesRead() {
        return bytesRead;
    }
    
    /**
     * {@inheritDoc}
     * <p>This implementation always returns true.</p>
     */
    @Override
    public boolean canReadEntryData(A archiveEntry) {
        return true;
    }

    /**
     * Increments the counter of already read bytes.
     * Doesn't increment if the EOF has been hit (read == -1)
     * 
     * @param read the number of bytes read
     */
    protected void count(long read) {
        if (read != -1) {
            bytesRead = bytesRead + read;
        }
    }

    /**
     * Decrements the counter of already read bytes.
     * 
     * @param pushedBack the number of bytes pushed back.
     */
    protected void pushedBackBytes(long pushedBack) {
        bytesRead -= pushedBack;
    }

}

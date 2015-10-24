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

import org.apache.commons.compress2.archivers.ArchiveOutput;
import org.apache.commons.compress2.archivers.ArchiveEntry;

/**
 * Base class implementations may use.
 * @NotThreadSafe
 */
public abstract class AbstractArchiveOutput<A extends ArchiveEntry> implements ArchiveOutput<A> {

    /** holds the number of bytes written to this channel */
    private long bytesWritten = 0;

    @Override
    public long getBytesWritten() {
        return bytesWritten;
    }
    
    /**
     * {@inheritDoc}
     * <p>This implementation always returns true.</p>
     */
    @Override
    public boolean canWriteEntryData(A archiveEntry) {
        return true;
    }

    /**
     * Increments the counter of written bytes.
     * 
     * @param written the number of bytes written
     */
    protected void count(long written) {
        bytesWritten += written;
    }

}

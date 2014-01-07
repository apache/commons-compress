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

import java.util.Date;

import org.apache.commons.compress2.archivers.ArchiveEntry;
import org.apache.commons.compress2.archivers.ArchiveEntryParameters;
import org.apache.commons.compress2.archivers.OwnerInformation;

/**
 * Container for the basic information of an {@link ArchiveEntry}.
 * @Immutable
 */
public class SimpleArchiveEntry implements ArchiveEntry {
    private final String name;
    private final long size;
    private final boolean dirFlag;
    private final Date lastModified;
    private final OwnerInformation owner;

    /**
     * Creates a SimpleArchiveEntry from a parameter object.
     * @param params the parameters describing the archive entry.
     */
    public SimpleArchiveEntry(ArchiveEntryParameters params) {
        this.name = params.getName();
        this.size = params.getSize();
        this.dirFlag = params.isDirectory();
        this.lastModified = params.getLastModifiedDate();
        this.owner = params.getOwnerInformation();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean isDirectory() {
        return dirFlag;
    }

    @Override
    public Date getLastModifiedDate() {
        return clone(lastModified);
    }

    @Override
    public OwnerInformation getOwnerInformation() {
        return owner;
    }

    // TODO second instance (after ArchiveEntryParameters)
    private static Date clone(Date d) {
        return d == null ? null : (Date) d.clone();
    }
}

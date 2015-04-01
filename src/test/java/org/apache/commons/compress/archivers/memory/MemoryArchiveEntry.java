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
package org.apache.commons.compress.archivers.memory;

import java.util.Date;

import org.apache.commons.compress.archivers.ArchiveEntry;

public final class MemoryArchiveEntry implements ArchiveEntry {

    private final String name;

    public MemoryArchiveEntry(final String pName) {
        name = pName;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isDirectory() {
        // TODO Auto-generated method stub
        return false;
    }

    public Date getLastModifiedDate() {
        return new Date();
    }
}

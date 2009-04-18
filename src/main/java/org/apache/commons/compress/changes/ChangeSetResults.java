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
package org.apache.commons.compress.changes;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the results of an performed ChangeSet operation.
 */
public class ChangeSetResults {
    private List addedFromChangeSet = new ArrayList();
    private List addedFromStream = new ArrayList();
    private List deleted = new ArrayList();
    
    void deleted(String fileName) {
        deleted.add(fileName);
    }
    
    void addedFromStream(String fileName) {
        addedFromStream.add(fileName);
    }
    
    void addedFromChangeSet(String fileName) {
        addedFromChangeSet.add(fileName);
    }

    /**
     * @return the addedFromChangeSet
     */
    public List getAddedFromChangeSet() {
        return addedFromChangeSet;
    }

    /**
     * @return the addedFromStream
     */
    public List getAddedFromStream() {
        return addedFromStream;
    }

    /**
     * @return the deleted
     */
    public List getDeleted() {
        return deleted;
    }
    
    boolean hasBeenAdded(String filename) {
        if(addedFromChangeSet.contains(filename) || addedFromStream.contains(filename)) {
            return true;
        } 
        return false;
    }
}

package org.apache.commons.compress.changes;

import java.util.ArrayList;
import java.util.List;

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
}

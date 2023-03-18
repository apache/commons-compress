/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.pack200;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides generic JavaBeans support for the Pack/UnpackAdapters
 */
public abstract class Pack200Adapter {

    protected static final int DEFAULT_BUFFER_SIZE = 8192;

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private final SortedMap<String, String> properties = new TreeMap<>();

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     * Completion between 0..1.
     *
     * @param value Completion between 0..1.
     */
    protected void completed(final double value) {
        firePropertyChange("pack.progress", null, String.valueOf((int) (100 * value))); //$NON-NLS-1$
    }

    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        support.firePropertyChange(propertyName, oldValue, newValue);
    }

    public SortedMap<String, String> properties() {
        return properties;
    }

    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
}

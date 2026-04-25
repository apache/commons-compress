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
package org.apache.commons.compress.harmony.pack200;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides generic JavaBeans support for the Pack/UnpackAdapters
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public abstract class Pack200Adapter {

    /**
     * Default buffer size.
     */
    protected static final int DEFAULT_BUFFER_SIZE = 8192;

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private final SortedMap<String, String> properties = new TreeMap<>();

    /**
     * Constructs a new Pack200Adapter.
     */
    public Pack200Adapter() {
    }

    /**
     * Adds a property change listener.
     *
     * @param listener the listener to add.
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     * Completion between 0..1.
     *
     * @param value Completion between 0..1.
     * @throws IOException if the value cannot be parsed.
     */
    protected void completed(final double value) throws IOException {
        firePropertyChange("pack.progress", null, String.valueOf((int) (100 * value))); //$NON-NLS-1$
    }

    /**
     * Reports a property update to listeners.
     *
     * @param propertyName name of property being updated.
     * @param oldValue old property value.
     * @param newValue new property value.
     * @throws IOException if the values cannot be parsed.
     */
    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) throws IOException {
        support.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Gets the properties map.
     *
     * @return the properties.
     */
    public SortedMap<String, String> properties() {
        return properties;
    }

    /**
     * Removes a property change listener.
     *
     * @param listener the listener to remove.
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
}

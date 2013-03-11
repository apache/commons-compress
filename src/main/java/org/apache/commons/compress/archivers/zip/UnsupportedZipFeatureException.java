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
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.zip;

import java.util.zip.ZipException;

/**
 * Exception thrown when attempting to read or write data for a zip
 * entry that uses ZIP features not supported by this library.
 * @since 1.1
 */
public class UnsupportedZipFeatureException extends ZipException {

    private final Feature reason;
    private final ZipArchiveEntry entry;
    private static final long serialVersionUID = 20130101L;

    /**
     * Creates an exception.
     * @param reason the feature that is not supported
     * @param entry the entry using the feature
     */
    public UnsupportedZipFeatureException(Feature reason,
                                          ZipArchiveEntry entry) {
        super("unsupported feature " + reason +  " used in entry "
              + entry.getName());
        this.reason = reason;
        this.entry = entry;
    }

    /**
     * Creates an exception for archives that use an unsupported
     * compression algorithm.
     * @param method the method that is not supported
     * @param entry the entry using the feature
     * @since 1.5
     */
    public UnsupportedZipFeatureException(ZipMethod method,
                                          ZipArchiveEntry entry) {
        super("unsupported feature method '" + method.name()
              +  "' used in entry " + entry.getName());
        this.reason = Feature.METHOD;
        this.entry = entry;
    }

    /**
     * Creates an exception when the whole archive uses an unsupported
     * feature.
     *
     * @param reason the feature that is not supported
     * @since 1.5
     */
    public UnsupportedZipFeatureException(Feature reason) {
        super("unsupported feature " + reason +  " used in archive.");
        this.reason = reason;
        this.entry = null;
    }

    /**
     * The unsupported feature that has been used.
     */
    public Feature getFeature() {
        return reason;
    }

    /**
     * The entry using the unsupported feature.
     */
    public ZipArchiveEntry getEntry() {
        return entry;
    }

    /**
     * ZIP Features that may or may not be supported.
     * @since 1.1
     */
    public static class Feature {
        /**
         * The entry is encrypted.
         */
        public static final Feature ENCRYPTION = new Feature("encryption");
        /**
         * The entry used an unsupported compression method.
         */
        public static final Feature METHOD = new Feature("compression method");
        /**
         * The entry uses a data descriptor.
         */
        public static final Feature DATA_DESCRIPTOR = new Feature("data descriptor");
        /**
         * The archive uses splitting or spanning.
         * @since 1.5
         */
        public static final Feature SPLITTING = new Feature("splitting");

        private final String name;

        private Feature(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

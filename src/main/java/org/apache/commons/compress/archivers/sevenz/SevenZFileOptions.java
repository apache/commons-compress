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
package org.apache.commons.compress.archivers.sevenz;

/**
 * Collects options for reading 7z archives.
 *
 * @since 1.19
 * @Immutable
 */
public class SevenZFileOptions {
    private static final int DEFAUL_MEMORY_LIMIT_IN_KB = Integer.MAX_VALUE;
    private static final boolean DEFAULT_USE_DEFAULTNAME_FOR_UNNAMED_ENTRIES= false;

    private final int maxMemoryLimitInKb;
    private final boolean useDefaultNameForUnnamedEntries;

    private SevenZFileOptions(int maxMemoryLimitInKb, boolean useDefaultNameForUnnamedEntries) {
        this.maxMemoryLimitInKb = maxMemoryLimitInKb;
        this.useDefaultNameForUnnamedEntries = useDefaultNameForUnnamedEntries;
    }

    /**
     * The default options.
     *
     * <ul>
     *   <li>no memory limit</li>
     *   <li>don't modifiy the name of unnamed entries</li>
     * </ul>
     */
    public static final SevenZFileOptions DEFAULT = new SevenZFileOptions(DEFAUL_MEMORY_LIMIT_IN_KB,
        DEFAULT_USE_DEFAULTNAME_FOR_UNNAMED_ENTRIES);

    /**
     * Obtains a builder for SevenZFileOptions.
     * @return a builder for SevenZFileOptions.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the maximum amount of memory to use for extraction. Not
     * all codecs will honor this setting. Currently only lzma and
     * lzma2 are supported.
     * @return the maximum amount of memory to use for extraction
     */
    public int getMaxMemoryLimitInKb() {
        return maxMemoryLimitInKb;
    }

    /**
     * Gets whether entries without a name should get their names set
     * to the archive's default file name.
     * @return whether entries without a name should get their names
     * set to the archive's default file name
     */
    public boolean getUseDefaultNameForUnnamedEntries() {
        return useDefaultNameForUnnamedEntries;
    }

    /**
     * Mutable builder for the immutable {@link SevenZFileOptions}.
     *
     * @since 1.19
     */
    public static class Builder {
        private int maxMemoryLimitInKb = DEFAUL_MEMORY_LIMIT_IN_KB;
        private boolean useDefaultNameForUnnamedEntries = DEFAULT_USE_DEFAULTNAME_FOR_UNNAMED_ENTRIES;
        /**
         * Sets the maximum amount of memory to use for
         * extraction. Not all codecs will honor this
         * setting. Currently only lzma and lzma2 are supported.
         *
         * @param maxMemoryLimitInKb limit of the maximum amount of memory to use
         * @return the reconfigured builder
         */
        public Builder withMaxMemoryLimitInKb(int maxMemoryLimitInKb) {
            this.maxMemoryLimitInKb = maxMemoryLimitInKb;
            return this;
        }

        /**
         * Sets whether entries without a name should get their names
         * set to the archive's default file name.
         *
         * @param useDefaultNameForUnnamedEntries if true the name of
         * unnamed entries will be set to the archive's default name
         * @return the reconfigured builder
         */
        public Builder withUseDefaultNameForUnnamedEntries(boolean useDefaultNameForUnnamedEntries) {
            this.useDefaultNameForUnnamedEntries = useDefaultNameForUnnamedEntries;
            return this;
        }

        /**
         * Create the {@link SevenZFileOptions}.
         *
         * @return configured {@link SevenZFileOptions}.
         */
        public SevenZFileOptions build() {
            return new SevenZFileOptions(maxMemoryLimitInKb, useDefaultNameForUnnamedEntries);
        }
    }
}

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
package org.apache.commons.compress.archivers.zip;

import java.util.zip.ZipException;

/**
 * Handles extra field data that doesn't follow the recommended
 * pattern for extra fields with a two-byte key and a two-byte length.
 *
 * @since 1.19
 */
public interface UnparseableExtraFieldBehavior {
    /**
     * Decides what to do with extra field data that doesn't follow the recommended pattern.
     *
     * @param data the array of extra field data
     * @param off offset into data where the unparseable data starts
     * @param len the length of unparseable data
     * @param local whether the extra field data stems from the local
     * file header. If this is false then the data is part if the
     * central directory header extra data.
     * @param claimedLength length of the extra field claimed by the
     * third and forth byte if it did follow the recommended pattern
     *
     * @return null if the data should be ignored or an extra field
     * implementation that represents the data
     * @throws ZipException if an error occurs or unparseable extra
     * fields must not be accepted
     */
    ZipExtraField onUnparseableExtraField(byte[] data, int off, int len, boolean local,
        int claimedLength) throws ZipException;
}

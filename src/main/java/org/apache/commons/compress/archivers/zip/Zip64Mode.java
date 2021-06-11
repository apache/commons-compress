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

/**
 * The different modes {@link ZipArchiveOutputStream} can operate in.
 *
 * @see ZipArchiveOutputStream#setUseZip64
 *
 * @since 1.3
 */
public enum Zip64Mode {
    /**
     * Use Zip64 extensions for all entries, even if it is clear it is
     * not required.
     */
    Always,
    /**
     * Don't use Zip64 extensions for any entries.
     *
     * <p>This will cause a {@link Zip64RequiredException} to be
     * thrown if {@link ZipArchiveOutputStream} detects it needs Zip64
     * support.</p>
     */
    Never,
    /**
     * Use Zip64 extensions for all entries where they are required,
     * don't use them for entries that clearly don't require them.
     */
    AsNeeded,
    /**
     * Always use Zip64 extensions for LFH and central directory as
     * {@link Zip64Mode#Always} did, and at the meantime encode
     * the relative offset of LFH and disk number start as needed in
     * CFH as {@link Zip64Mode#AsNeeded} did.
     * <p>
     * This is a compromise for some libraries including 7z and
     * Expand-Archive Powershell utility(and likely Excel).
     */
    AlwaysWithCompatibility
}

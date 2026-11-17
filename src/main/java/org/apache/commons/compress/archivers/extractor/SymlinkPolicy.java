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
package org.apache.commons.compress.archivers.extractor;

/**
 * Controls how symbolic-link entries in an archive are handled during extraction. For untrusted archives, {@link #REJECT}
 * (the default) and {@link #SKIP} are the safe choices; {@link #ALLOW_WITHIN_ROOT} enforces only a best-effort lexical
 * containment check, as described on that constant.
 *
 * @since 1.29.0
 */
public enum SymlinkPolicy {

    /**
     * Fail extraction if the archive contains any symbolic-link entry. This is the default and the safest option for
     * untrusted archives.
     */
    REJECT,

    /**
     * Silently ignore symbolic-link entries; nothing is created for them.
     */
    SKIP,

    /**
     * Create a symbolic link only when its target lexically resolves inside the extraction root, and reject a link whose
     * target escapes. This containment check is best-effort: it is evaluated lexically against the archive-declared target at
     * creation time, so it does not prevent a target reached through another symbolic link, nor a chain of individually
     * in-root links that escapes only once the operating system resolves them together. Extraction itself never follows these
     * links (every path component is resolved with {@link java.nio.file.LinkOption#NOFOLLOW_LINKS}), so nothing is written
     * outside the root while extracting; the residual risk is a created link that a later consumer follows. For untrusted
     * archives prefer {@link #REJECT} (the default) or {@link #SKIP}.
     */
    ALLOW_WITHIN_ROOT,

    /**
     * Create symbolic links verbatim, including links whose target escapes the extraction root. Only appropriate for fully
     * trusted archives.
     */
    ALLOW_ALL
}

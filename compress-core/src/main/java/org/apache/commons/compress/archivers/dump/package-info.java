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

/**
 * This package provides stream classes for reading archives using the Unix DUMP format.
 * <p>
 * This format is similar to (and contemporary with) TAR but reads the raw filesystem directly. This means that writers are filesystem-specific even though the
 * created archives are filesystem-agnostic.
 * </p>
 *
 * <p>
 * Unlike other formats DUMP offers clean support for sparse files, extended attributes, and other file metadata. In addition DUMP supports incremental dump
 * files can capture (most) file deletion. It also provides a native form of compression and will soon support native encryption as well.
 * </p>
 * <p>
 * In practice TAR archives are used for both distribution and backups. DUMP archives are used exclusively for backups.
 * </p>
 * <p>
 * Like any 30+-year-old application there are a number of variants. For pragmatic reasons we will only support archives with the 'new' tape header and inode
 * formats. Other restrictions:
 * </p>
 * <ul>
 * <li>We only support ZLIB compression. The format also permits LZO and BZLIB compression.</li>
 * <li>Sparse files will have the holes filled.</li>
 * <li>MacOS finder and resource streams are ignored.</li>
 * <li>Extended attributes are not currently provided.</li>
 * <li>SELinux labels are not currently provided.</li>
 * </ul>
 * <p>
 * As of Apache Commons Compress 1.3 support for the dump format is read-only.
 * </p>
 */
package org.apache.commons.compress.archivers.dump;

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
/**
 * Safe, symlink-resistant extraction of archives into a directory.
 * <p>
 * {@link org.apache.commons.compress.archivers.extractor.Extractor#newExtractor(java.nio.file.Path)} returns the strongest
 * implementation the platform supports: a race-safe extractor backed by {@link java.nio.file.SecureDirectoryStream} where the
 * file system provides one (Linux), otherwise a best-effort extractor that still resists symlink-slip from the archive but
 * cannot fully close a concurrent TOCTOU race. Every path component is resolved with {@link java.nio.file.LinkOption#NOFOLLOW_LINKS}
 * and regular files are written {@code CREATE_NEW} so a planted or archived symlink is never followed.
 * </p>
 *
 * @since 1.29.0
 */
package org.apache.commons.compress.archivers.extractor;

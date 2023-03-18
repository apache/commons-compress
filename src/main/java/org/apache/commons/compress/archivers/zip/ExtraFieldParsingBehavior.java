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
 * Controls details of parsing ZIP extra fields.
 *
 * @since 1.19
 */
public interface ExtraFieldParsingBehavior extends UnparseableExtraFieldBehavior {
    /**
     * Creates an instance of ZipExtraField for the given id.
     *
     * <p>A good default implementation would be {@link
     * ExtraFieldUtils#createExtraField}.</p>
     *
     * @param headerId the id for the extra field
     * @return an instance of ZipExtraField, must not be {@code null}
     * @throws ZipException if an error occurs
     * @throws InstantiationException if unable to instantiate the class
     * @throws IllegalAccessException if not allowed to instantiate the class
     */
    ZipExtraField createExtraField(final ZipShort headerId)
        throws ZipException, InstantiationException, IllegalAccessException;

    /**
     * Fills in the extra field data for a single extra field.
     *
     * <p>A good default implementation would be {@link
     * ExtraFieldUtils#fillExtraField}.</p>
     *
     * @param field the extra field instance to fill
     * @param data the array of extra field data
     * @param off offset into data where this field's data starts
     * @param len the length of this field's data
     * @param local whether the extra field data stems from the local
     * file header. If this is false then the data is part if the
     * central directory header extra data.
     * @return the filled field. Usually this is the same as {@code
     * field} but it could be a replacement extra field as well. Must
     * not be {@code null}.
     * @throws ZipException if an error occurs
     */
    ZipExtraField fill(ZipExtraField field, byte[] data, int off, int len, boolean local)
        throws ZipException;
}

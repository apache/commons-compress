/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.apache.commons.compress.utils;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * Generic Archive utilities
 */
public class ArchiveUtils {

    /**
     * Generates a string containing the name, isDirectory setting and size of an entry.
     * <p>
     * For example:<br/>
     * <tt>-    2000 main.c</tt><br/>
     * <tt>d     100 testfiles</tt><br/>
     * 
     * @return the representation of the entry
     */
    public static String toString(ArchiveEntry entry){
        StringBuffer sb = new StringBuffer();
        sb.append(entry.isDirectory()? 'd' : '-');// c.f. "ls -l" output
        String size = Long.toString((entry.getSize()));
        sb.append(' ');
        // Pad output to 7 places, leading spaces
        for(int i=7; i > size.length(); i--){
            sb.append(' ');
        }
        sb.append(size);
        sb.append(' ').append(entry.getName());
        return sb.toString();
    }
}

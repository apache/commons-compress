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

package org.apache.commons.compress.utils;

import org.apache.commons.compress.compressors.FileNameUtil;

import java.io.File;
import java.util.Comparator;

public class ZipSplitSegmentComparator implements Comparator<File> {
    @Override
    public int compare(File file1, File file2) {
        String extension1 = FileNameUtil.getExtension(file1.getPath());
        String extension2 = FileNameUtil.getExtension(file2.getPath());

        if(!extension1.startsWith("z")) {
            return -1;
        }

        if(!extension2.startsWith("z")) {
            return 1;
        }

        Integer splitSegmentNumber1 = Integer.parseInt(extension1.substring(1));
        Integer splitSegmentNumber2 = Integer.parseInt(extension2.substring(1));

        return splitSegmentNumber1.compareTo(splitSegmentNumber2);
    }
}

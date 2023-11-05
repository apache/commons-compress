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

package org.apache.commons.compress.changes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;

final class TestFixtures {

    static Set<String> getEmptyOutputArchiveNames() {
        final Set<String> outputStreamArchiveNames = getOutputArchiveNames();
        outputStreamArchiveNames.remove(ArchiveStreamFactory.TAR); // TODO BUG?
        outputStreamArchiveNames.remove(ArchiveStreamFactory.CPIO); // TODO BUG?
        return outputStreamArchiveNames;
    }

    static Set<String> getOutputArchiveNames() {
        final Set<String> outputStreamArchiveNames = ArchiveStreamFactory.DEFAULT.getOutputStreamArchiveNames();
        outputStreamArchiveNames.remove(ArchiveStreamFactory.AR); // TODO BUG?
        outputStreamArchiveNames.remove(ArchiveStreamFactory.SEVEN_Z); // TODO Does not support streaming.
        return outputStreamArchiveNames;
    }

    static List<String> getZipOutputArchiveNames() {
        final List<String> names = new ArrayList<>();
        names.add(ArchiveStreamFactory.ZIP);
        names.add(ArchiveStreamFactory.JAR);
        return names;
    }

}

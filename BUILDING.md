<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
# Building Apache Commons Compress

In order to build Commons Compress a JDK implementation 8 or higher
and Apache Maven 3.x are required. **Note that Commons Compress
currently doesn't build on JDK 14+, we will address this before
releasing Compress 1.21**.

To install the jars into your local Maven repository simply run

    mvn clean install

which will also run the unit tests.

Some tests are only run when specific profiles are enabled, these
tests require a lot of disk space as they test behavior for very large
archives.

    mvn test -Prun-tarit

runs tests for tar archives and requires more than 8GiB of disk space.

    mvn test -Prun-zipit

runs tests for zip archives that require up to 20 GiB of disk
space. In addition the tests will run for a long time (more than ten
minutes, maybe even longer depending on your hardware) and heavily
load the CPU at times.

## Building the Site

The site build produces license release audit (aka RAT) reports as
well as PMD and findbugs reports. Clirr didn't work for us anymore so
we switched to japicmp, the same is true for Cobertura which we had to
replace with jacoco.

japicmp requires the jar to be present when the site is built,
therefore the package goal must be executed before creating the site.

    mvn package site -Pjacoco

builds the site.

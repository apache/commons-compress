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
package org.apache.commons.compress;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class OsgiITest {

    @Configuration
    public Option[] config() {
        return new Option[] {
            systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr")
                .version("2.0.14"),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin")
                .version("1.8.16"),
            composite(systemProperty("pax.exam.invoker").value("junit"),
                      bundle("link:classpath:META-INF/links/org.ops4j.pax.tipi.junit.link"),
                      bundle("link:classpath:META-INF/links/org.ops4j.pax.exam.invoker.junit.link"),
                      mavenBundle().groupId("org.apache.servicemix.bundles")
                          .artifactId("org.apache.servicemix.bundles.hamcrest").version("1.3_1")),
            bundle("reference:file:target/classes/").start()
       };
    }

    @Test
    public void loadBundle() {
    }
}

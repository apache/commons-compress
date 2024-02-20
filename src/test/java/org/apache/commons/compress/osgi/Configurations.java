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
package org.apache.commons.compress.osgi;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;


final class Configurations {

    /**
     * @return The maven bundle for Apache commons-codec
     */
    private static MavenArtifactProvisionOption getCommonsCodec() {
        return mavenBundle().groupId("commons-codec").artifactId("commons-codec").version("1.16.0");
    }

    public static Option[] getDefaultConfig() {
        return new Option[]{systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                systemProperty("org.ops4j.pax.url.mvn.useFallbackRepositories").value("false"),
                systemProperty("org.ops4j.pax.url.mvn.repositories").value("https://repo.maven.apache.org/maven2"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr").version("2.0.14"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.8.16"),
                getCommonsCodec(),
                mavenBundle().groupId("commons-io").artifactId("commons-io").version("2.15.1"),
                composite(systemProperty("pax.exam.invoker").value("junit"), bundle("link:classpath:META-INF/links/org.ops4j.pax.tipi.junit.link"),
                        bundle("link:classpath:META-INF/links/org.ops4j.pax.exam.invoker.junit.link"),
                        mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").version("1.3_1")),
                bundle("reference:file:target/classes/").start()};
    }

    public static Option[] getConfigWithoutCommonsCodec() {
        final Option[] defaultConfig = getDefaultConfig();
        final Option[] result = Arrays.stream(defaultConfig)
                .filter(o -> !getCommonsCodec().equals(o))
                .toArray(Option[]::new);
        Assertions.assertTrue(result.length < defaultConfig.length, "Expected to have removed an option.");
        return result;
    }

    private Configurations() {

    }
}

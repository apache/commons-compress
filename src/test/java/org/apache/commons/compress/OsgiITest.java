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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public class OsgiITest {

    private static final String EXPECTED_BUNDLE_NAME = "org.apache.commons.commons-compress";

    @Inject
    private BundleContext ctx;

    @Test
    public void canLoadBundle() {
        assertNotNull("Expected to find bundle " + EXPECTED_BUNDLE_NAME, loadBundle());
    }

    @Configuration
    public Option[] config() {
        return new Option[] {
            systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
            systemProperty("org.ops4j.pax.url.mvn.useFallbackRepositories").value("false"),
            systemProperty("org.ops4j.pax.url.mvn.repositories").value("https://repo.maven.apache.org/maven2"),
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

    private Bundle loadBundle() {
        for (final Bundle b : ctx.getBundles()) {
            if (EXPECTED_BUNDLE_NAME.equals(b.getSymbolicName())) {
                return b;
            }
        }
        return null;
    }

    @Test
    public void properlyDetectsRunningInsideOsgiEnv() throws Exception {
        final Class<?> osgiUtils = loadBundle().loadClass("org.apache.commons.compress.utils.OsgiUtils");
        assertNotNull("Can load OsgiUtils via bundle", osgiUtils);

        final Method m = osgiUtils.getMethod("isRunningInOsgiEnvironment");
        assertNotNull("Can access isRunningInOsgiEnvironment method", m);

        assertTrue("Compress detects OSGi environment", (Boolean) m.invoke(null));
    }
}

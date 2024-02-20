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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
public class OsgiWithoutOptionalDepsITest {

    private static final String EXPECTED_BUNDLE_NAME = "org.apache.commons.commons-compress";

    @Inject
    private BundleContext ctx;

    @Configuration
    public Option[] config() {
        return Configurations.WITHOUT_COMMONS_CODEC;
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
    public void testCanLoadBundle() {
        assertNotNull("Expected to find bundle " + EXPECTED_BUNDLE_NAME, loadBundle());
    }

    @Test
    public void testProperlyDetectsRunningInsideOsgiEnv() throws Exception {
        final Class<?> osgiUtils = loadBundle().loadClass("org.apache.commons.compress.utils.OsgiUtils");
        assertNotNull("Can load OsgiUtils via bundle", osgiUtils);

        final Method method = osgiUtils.getMethod("isRunningInOsgiEnvironment");
        assertNotNull("Can access isRunningInOsgiEnvironment method", method);

        assertTrue("Compress detects OSGi environment", (Boolean) method.invoke(null));
    }
}

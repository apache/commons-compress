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
package org.apache.commons.compress.osgi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Tests if the library can be loaded with an OSGi environment provided by {@link #config()}.
 */
abstract class AbstractOsgiITest {

    private static final String EXPECTED_BUNDLE_NAME = "org.apache.commons.commons-compress";

    @Inject
    private BundleContext ctx;

    /**
     * Gets the OSGi configuration to use for the test.
     *
     * @return the OSGi configuration to use for the test.
     * @implNote Concrete implementation needs the @Configuration annotation.
     */
    public abstract Option[] config();

    private Bundle loadBundle() {
        for (final Bundle b : ctx.getBundles()) {
            if (EXPECTED_BUNDLE_NAME.equals(b.getSymbolicName())) {
                return b;
            }
        }
        return null;
    }

    @Test
    void testCanLoadBundle() {
        assertNotNull("Expected to find bundle " + EXPECTED_BUNDLE_NAME, loadBundle());
    }

    @Test
    void testProperlyDetectsRunningInsideOsgiEnv() throws Exception {
        final String className = "org.apache.commons.compress.utils.OsgiUtils";
        final Class<?> osgiUtils = loadBundle().loadClass(className);
        assertNotNull("Can't load " + className + " via bundle", osgiUtils);
        final Method method = osgiUtils.getMethod("isRunningInOsgiEnvironment");
        assertNotNull("Can't access isRunningInOsgiEnvironment method", method);
        assertTrue("Compress can't detect OSGi environment", (Boolean) method.invoke(null));
    }
}

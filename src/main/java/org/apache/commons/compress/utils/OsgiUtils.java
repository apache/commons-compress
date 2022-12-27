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

/**
 * Utilities for dealing with OSGi environments.
 *
 * @since 1.21
 */
public class OsgiUtils {

    private static final boolean inOsgiEnvironment;

    static {
        final Class<?> classloaderClass = OsgiUtils.class.getClassLoader().getClass();
        inOsgiEnvironment = isBundleReference(classloaderClass);
    }

    private static boolean isBundleReference(final Class<?> clazz) {
        Class<?> c = clazz;
        while (c != null) {
            if (c.getName().equals("org.osgi.framework.BundleReference")) {
                return true;
            }
            for (final Class<?> ifc : c.getInterfaces()) {
                if (isBundleReference(ifc)) {
                    return true;
                }
            }
            c = c.getSuperclass();
        }
        return false;
    }

    /**
     * Tests if Commons Compress running as an OSGi bundle?
     * @return true if Commons Compress running as an OSGi bundle.
     */
    public static boolean isRunningInOsgiEnvironment() {
        return inOsgiEnvironment;
    }

}

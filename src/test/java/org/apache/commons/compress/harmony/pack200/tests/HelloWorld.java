/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.pack200.tests;

/**
 * This is intended to be used as a test class for unpacking a packed Jar file.
 */
public class HelloWorld {

    public static void main(final String[] args) {
        System.out.println("Hello world");
    }
    int i = 97, j = 42, k = 12345;
    float f = 3.142f, g = 2.718f;
    long l = 299792458;

    double d = 4.0d;

    public HelloWorld[][] method(final int a, final int b, final int c) {
        return null;
    }
}

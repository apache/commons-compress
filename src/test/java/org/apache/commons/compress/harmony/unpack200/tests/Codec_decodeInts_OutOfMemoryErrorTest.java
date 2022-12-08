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
package org.apache.commons.compress.harmony.unpack200.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;
import org.apache.commons.compress.compressors.pack200.Pack200Strategy;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests https://issues.apache.org/jira/browse/COMPRESS-599.
 *
 * <pre>{@code
 * java.lang.OutOfMemoryError: Java heap space
  at org.apache.commons.compress.harmony.unpack200.CpBands.parseCpUtf8(CpBands.java:365)
  at org.apache.commons.compress.harmony.unpack200.CpBands.read(CpBands.java:111)
  at org.apache.commons.compress.harmony.unpack200.Segment.readSegment(Segment.java:351)
  at org.apache.commons.compress.harmony.unpack200.Segment.unpackRead(Segment.java:459)
  at org.apache.commons.compress.harmony.unpack200.Segment.unpack(Segment.java:436)
  at org.apache.commons.compress.harmony.unpack200.Archive.unpack(Archive.java:155)
  at org.apache.commons.compress.harmony.unpack200.Pack200UnpackerAdapter.unpack(Pack200UnpackerAdapter.java:49)
  at org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream.<init>(Pack200CompressorInputStream.java:183)
  at org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream.<init>(Pack200CompressorInputStream.java:77)
  at org.apache.commons.compress.harmony.unpack200.tests.Codec_decodeInts_OutOfMemoryErrorTest.test(Codec_decodeInts_OutOfMemoryErrorTest.java:36)
  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
  at java.lang.reflect.Method.invoke(Method.java:498)
  at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
  at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
  at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
  at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
  at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
  at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
  at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
  at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
  at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
  at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
  at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
  at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
  at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
  at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
  at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
  at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
  at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93)
  at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40)}
 * </pre>
 */
@Disabled
public class Codec_decodeInts_OutOfMemoryErrorTest {
    private static final String BASE64_BYTES = "yv7QDQeW0ABgfwDuwOn8QwIGAAIBAQAAd9zc3Nzc3Nzc3Nzc3Nzc3NxuZXR3YXJl3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3GluZG93cwAAAwMUAxUDZmVzdA0K";

    @Test
    public void test() throws IOException {
        final byte[] input = java.util.Base64.getDecoder().decode(BASE64_BYTES);
        try (InputStream is = new Pack200CompressorInputStream(new ByteArrayInputStream(input), Pack200Strategy.TEMP_FILE)) {
            // do nothing
        }

    }
}

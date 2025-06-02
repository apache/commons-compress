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

package org.apache.commons.compress.archivers.tar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests https://issues.apache.org/jira/browse/COMPRESS-699
 */
public class Compress700Test {

    private static final Path PATH = Paths.get("src/test/resources/org/apache/commons/compress/COMPRESS-700/flutter_awesome_buttons-0.1.0.tar");

    @Test
    public void testFirstTarArchiveEntry() throws Exception {
        try (TarArchiveInputStream inputStream = new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(PATH)))) {
            final TarArchiveEntry entry = inputStream.getNextEntry();
            assertNull(entry.getCreationTime());
            assertEquals(-1, entry.getDataOffset());
            assertEquals(0, entry.getDevMajor());
            assertEquals(0, entry.getDevMinor());
            assertEquals(Collections.emptyMap(), entry.getExtraPaxHeaders());
            assertEquals(0, entry.getGroupId());
            assertEquals("", entry.getGroupName());
            assertNull(entry.getLastAccessTime());
            assertEquals(Date.parse("Tue Dec 03 05:08:42 EST 2019"), entry.getLastModifiedDate().getTime());
            assertEquals(FileTime.from(Instant.parse("2019-12-03T10:08:42Z")), entry.getLastModifiedTime());
            assertEquals(48, entry.getLinkFlag());
            assertEquals("", entry.getLinkName());
            assertEquals(0, entry.getLongGroupId());
            assertEquals(0, entry.getLongUserId());
            assertEquals(33279, entry.getMode());
            assertEquals(Date.parse("Tue Dec 03 05:08:42 EST 2019"), entry.getModTime().getTime());
            assertEquals("build/app.dill", entry.getName());
            assertNull(entry.getPath());
            assertEquals(0, entry.getRealSize());
            assertEquals(0, entry.getSize());
            assertNull(entry.getStatusChangeTime());
            assertEquals(0, entry.getUserId());
            assertEquals("", entry.getUserName());
            assertTrue(entry.isFile());
            assertFalse(entry.isBlockDevice());
            assertFalse(entry.isCharacterDevice());
            assertTrue(entry.isCheckSumOK());
            assertFalse(entry.isDirectory());
            assertFalse(entry.isExtended());
            assertFalse(entry.isFIFO());
            assertFalse(entry.isGlobalPaxHeader());
            assertFalse(entry.isGNULongLinkEntry());
            assertFalse(entry.isGNULongNameEntry());
            assertFalse(entry.isGNUSparse());
            assertFalse(entry.isLink());
            assertFalse(entry.isOldGNUSparse());
            assertFalse(entry.isPaxGNU1XSparse());
            assertFalse(entry.isPaxGNUSparse());
            assertFalse(entry.isPaxHeader());
            assertFalse(entry.isSparse());
            assertFalse(entry.isStarSparse());
            assertTrue(entry.isStreamContiguous());
            assertFalse(entry.isSymbolicLink());
            assertTrue(entry.isTypeFlagUstar());
        }
    }

    @Test
    public void testListEntries() throws IOException {
        final List<Object[]> list = Arrays.asList(
                new Object[] {0,     "build/app.dill"}, // 0
                new Object[] {105,   "CHANGELOG.md"},
                new Object[] {2119,  "example/android/app/build.gradle"},
                new Object[] {339,   "example/android/app/src/debug/AndroidManifest.xml"},
                new Object[] {1745,  "example/android/app/src/main/AndroidManifest.xml"},
                new Object[] {559,   "example/android/app/src/main/java/io/flutter/plugins/GeneratedPluginRegistrant.java"},
                new Object[] {353,   "example/android/app/src/main/kotlin/com/example/test_package/MainActivity.kt"},
                new Object[] {446,   "example/android/app/src/main/res/drawable/launch_background.xml"},
                new Object[] {544,   "example/android/app/src/main/res/mipmap-hdpi/ic_launcher.png"},
                new Object[] {442,   "example/android/app/src/main/res/mipmap-mdpi/ic_launcher.png"},
                new Object[] {721,   "example/android/app/src/main/res/mipmap-xhdpi/ic_launcher.png"}, // 10
                new Object[] {1031,  "example/android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png"},
                new Object[] {1443,  "example/android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"},
                new Object[] {369,   "example/android/app/src/main/res/values/styles.xml"},
                new Object[] {339,   "example/android/app/src/profile/AndroidManifest.xml"},
                new Object[] {613,   "example/android/build.gradle"},
                new Object[] {32,    "example/android/gradle.properties"},
                new Object[] {4971,  "example/android/gradlew"},
                new Object[] {2404,  "example/android/gradlew.bat"},
                new Object[] {53636, "example/android/gradle/wrapper/gradle-wrapper.jar"},
                new Object[] {240,   "example/android/gradle/wrapper/gradle-wrapper.properties"}, // 20
                new Object[] {150,   "example/android/local.properties"},
                new Object[] {499,   "example/android/settings.gradle"},
                new Object[] {1630,  "example/android/test_package_android.iml"},
                new Object[] {820,   "example/ios/Flutter/AppFrameworkInfo.plist"},
                new Object[] {31,    "example/ios/Flutter/Debug.xcconfig"},
                new Object[] {461,   "example/ios/Flutter/flutter_export_environment.sh"},
                new Object[] {380,   "example/ios/Flutter/Generated.xcconfig"},
                new Object[] {31,    "example/ios/Flutter/Release.xcconfig"},
                new Object[] {21606, "example/ios/Runner.xcodeproj/project.pbxproj"},
                new Object[] {159,   "example/ios/Runner.xcodeproj/project.xcworkspace/contents.xcworkspacedata"}, // 30
                new Object[] {3382,  "example/ios/Runner.xcodeproj/xcshareddata/xcschemes/Runner.xcscheme"},
                new Object[] {159,   "example/ios/Runner.xcworkspace/contents.xcworkspacedata"},
                new Object[] {417,   "example/ios/Runner/AppDelegate.swift"},
                new Object[] {2641,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Contents.json"},
                new Object[] {10932, "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-1024x1024@1x.png"},
                new Object[] {564,   "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-20x20@1x.png"},
                new Object[] {1283,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-20x20@2x.png"},
                new Object[] {1588,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-20x20@3x.png"},
                new Object[] {1025,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-29x29@1x.png"},
                new Object[] {1716,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-29x29@2x.png"},
                new Object[] {1920,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-29x29@3x.png"},
                new Object[] {1283,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-40x40@1x.png"},
                new Object[] {1895,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-40x40@2x.png"},
                new Object[] {2665,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-40x40@3x.png"},
                new Object[] {2665,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-60x60@2x.png"},
                new Object[] {3831,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-60x60@3x.png"},
                new Object[] {1888,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-76x76@1x.png"},
                new Object[] {3294,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-76x76@2x.png"},
                new Object[] {3612,  "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-83.5x83.5@2x.png"},
                new Object[] {414,   "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/Contents.json"},
                new Object[] {68,    "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/LaunchImage.png"},
                new Object[] {68,    "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/LaunchImage@2x.png"},
                new Object[] {68,    "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/LaunchImage@3x.png"},
                new Object[] {340,   "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/README.md"},
                new Object[] {2414,  "example/ios/Runner/Base.lproj/LaunchScreen.storyboard"},
                new Object[] {1631,  "example/ios/Runner/Base.lproj/Main.storyboard"},
                new Object[] {310,   "example/ios/Runner/GeneratedPluginRegistrant.h"},
                new Object[] {204,   "example/ios/Runner/GeneratedPluginRegistrant.m"},
                new Object[] {1576,  "example/ios/Runner/Info.plist"},
                new Object[] {37,    "example/ios/Runner/Runner-Bridging-Header.h"},
                new Object[] {8996,  "example/lib/main.dart"},
                new Object[] {2759,  "example/pubspec.yaml"},
                new Object[] {9879,  "example/README.md"},
                new Object[] {1081,  "example/test/widget_test.dart"},
                new Object[] {913,   "example/test_package.iml"},
                new Object[] {1000,  "flutter_buttons.iml"},
                new Object[] {36861, "lib/flutter_awesome_buttons.dart"},
                new Object[] {30,    "LICENSE"},
                new Object[] {1751,  "pubspec.yaml"},
                new Object[] {9879,  "README.md"},
                new Object[] {433,   "test/flutter_buttons_test.dart"});
        // @formatter:on
        try (TarArchiveInputStream inputStream = new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(PATH)))) {
            final AtomicInteger i = new AtomicInteger();
            for (final Object[] pair : list) {
                final TarArchiveEntry entry = inputStream.getNextEntry();
                assertNotNull(entry, entry.getName());
                // System.out.println(entry);
                final String name = (String) pair[1];
                assertEquals(name, entry.getName(), () -> String.format("[%d] %s", i.get(), entry));
                final int size = ((Integer) pair[0]).intValue();
                assertEquals(size, entry.getSize(), () -> String.format("[%d] %s", i.get(), entry));
                assertEquals(size, entry.getRealSize(), () -> String.format("[%d] %s", i.get(), entry));
                i.incrementAndGet();
            }
        }
    }

    //@Disabled
    //@Ignore
    @Test
    public void testTarArchive() throws Exception {
        try (BufferedInputStream fileInputStream = new BufferedInputStream(Files.newInputStream(PATH))) {
            assertEquals(ArchiveStreamFactory.TAR, ArchiveStreamFactory.detect(fileInputStream));
        }
    }
}

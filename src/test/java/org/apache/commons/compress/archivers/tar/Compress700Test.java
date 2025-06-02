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
        // @formatter:off
        final List<String> names = Arrays.asList(
                "build/app.dill",
                "CHANGELOG.md",
                "example/android/app/build.gradle",
                "example/android/app/src/debug/AndroidManifest.xml",
                "example/android/app/src/main/AndroidManifest.xml",
                "example/android/app/src/main/java/io/flutter/plugins/GeneratedPluginRegistrant.java",
                "example/android/app/src/main/kotlin/com/example/test_package/MainActivity.kt",
                "example/android/app/src/main/res/drawable/launch_background.xml",
                "example/android/app/src/main/res/mipmap-hdpi/ic_launcher.png",
                "example/android/app/src/main/res/mipmap-mdpi/ic_launcher.png",
                "example/android/app/src/main/res/mipmap-xhdpi/ic_launcher.png",
                "example/android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png",
                "example/android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
                "example/android/app/src/main/res/values/styles.xml",
                "example/android/app/src/profile/AndroidManifest.xml",
                "example/android/build.gradle",
                "example/android/gradle.properties",
                "example/android/gradlew",
                "example/android/gradlew.bat",
                "example/android/gradle/wrapper/gradle-wrapper.jar",
                "example/android/gradle/wrapper/gradle-wrapper.properties",
                "example/android/local.properties",
                "example/android/settings.gradle",
                "example/android/test_package_android.iml",
                "example/ios/Flutter/AppFrameworkInfo.plist",
                "example/ios/Flutter/Debug.xcconfig",
                "example/ios/Flutter/flutter_export_environment.sh",
                "example/ios/Flutter/Generated.xcconfig",
                "example/ios/Flutter/Release.xcconfig",
                "example/ios/Runner.xcodeproj/project.pbxproj",
                "example/ios/Runner.xcodeproj/project.xcworkspace/contents.xcworkspacedata",
                "example/ios/Runner.xcodeproj/xcshareddata/xcschemes/Runner.xcscheme",
                "example/ios/Runner.xcworkspace/contents.xcworkspacedata",
                "example/ios/Runner/AppDelegate.swift",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Contents.json",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-1024x1024@1x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-20x20@1x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-20x20@2x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-20x20@3x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-29x29@1x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-29x29@2x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-29x29@3x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-40x40@1x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-40x40@2x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-40x40@3x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-60x60@2x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-60x60@3x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-76x76@1x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-76x76@2x.png",
                "example/ios/Runner/Assets.xcassets/AppIcon.appiconset/Icon-App-83.5x83.5@2x.png",
                "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/Contents.json",
                "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/LaunchImage.png",
                "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/LaunchImage@2x.png",
                "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/LaunchImage@3x.png",
                "example/ios/Runner/Assets.xcassets/LaunchImage.imageset/README.md",
                "example/ios/Runner/Base.lproj/LaunchScreen.storyboard",
                "example/ios/Runner/Base.lproj/Main.storyboard",
                "example/ios/Runner/GeneratedPluginRegistrant.h",
                "example/ios/Runner/GeneratedPluginRegistrant.m",
                "example/ios/Runner/Info.plist",
                "example/ios/Runner/Runner-Bridging-Header.h",
                "example/lib/main.dart",
                "example/pubspec.yaml",
                "example/README.md",
                "example/test/widget_test.dart",
                "example/test_package.iml",
                "flutter_buttons.iml",
                "lib/flutter_awesome_buttons.dart",
                "LICENSE",
                "pubspec.yaml",
                "README.md",
                "test/flutter_buttons_test.dart");
        // @formatter:on
        try (TarArchiveInputStream inputStream = new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(PATH)))) {
            for (final String name : names) {
                final TarArchiveEntry entry = inputStream.getNextEntry();
                assertNotNull(entry, entry.getName());
                // System.out.println(entry);
                assertEquals(name, entry.getName());
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

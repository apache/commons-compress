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
package org.apache.commons.compress.archivers.sevenz;

final class NID {
    static final int kEnd = 0x00;
    static final int kHeader = 0x01;
    static final int kArchiveProperties = 0x02;
    static final int kAdditionalStreamsInfo = 0x03;
    static final int kMainStreamsInfo = 0x04;
    static final int kFilesInfo = 0x05;
    static final int kPackInfo = 0x06;
    static final int kUnpackInfo = 0x07;
    static final int kSubStreamsInfo = 0x08;
    static final int kSize = 0x09;
    static final int kCRC = 0x0A;
    static final int kFolder = 0x0B;
    static final int kCodersUnpackSize = 0x0C;
    static final int kNumUnpackStream = 0x0D;
    static final int kEmptyStream = 0x0E;
    static final int kEmptyFile = 0x0F;
    static final int kAnti = 0x10;
    static final int kName = 0x11;
    static final int kCTime = 0x12;
    static final int kATime = 0x13;
    static final int kMTime = 0x14;
    static final int kWinAttributes = 0x15;
    static final int kComment = 0x16;
    static final int kEncodedHeader = 0x17;
    static final int kStartPos = 0x18;
    static final int kDummy = 0x19;
}

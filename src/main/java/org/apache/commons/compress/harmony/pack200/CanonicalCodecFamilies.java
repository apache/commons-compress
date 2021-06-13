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
package org.apache.commons.compress.harmony.pack200;

/**
 * Sets of codecs that share characteristics. Mainly used for different effort compression heuristics in BandSet.
 */
public class CanonicalCodecFamilies {

    // Families of codecs for bands of positive integers that do not correlate
    // well (i.e. would not benefit from delta encoding)

    public static BHSDCodec[] nonDeltaUnsignedCodecs1 = {
        // (1,256) is a special case and is considered separately so shouldn't be included here
//        CodecEncoding.getCanonicalCodec(1), // (1,256)
        CodecEncoding.getCanonicalCodec(5), // (2,256)
        CodecEncoding.getCanonicalCodec(9), // (3,256)
        CodecEncoding.getCanonicalCodec(13) // (4,256)
    };

    public static BHSDCodec[] nonDeltaUnsignedCodecs2 = {CodecEncoding.getCanonicalCodec(17), // (5,4)
        CodecEncoding.getCanonicalCodec(20), // (5,16)
        CodecEncoding.getCanonicalCodec(23), // (5,32)
        CodecEncoding.getCanonicalCodec(26), // (5,64)
        CodecEncoding.getCanonicalCodec(29) // (5,128)
    };

    public static BHSDCodec[] nonDeltaUnsignedCodecs3 = {CodecEncoding.getCanonicalCodec(47), // (2,192)
        CodecEncoding.getCanonicalCodec(48), // (2,224)
        CodecEncoding.getCanonicalCodec(49), // (2,240)
        CodecEncoding.getCanonicalCodec(50), // (2,248)
        CodecEncoding.getCanonicalCodec(51) // (2,252)
    };

    public static BHSDCodec[] nonDeltaUnsignedCodecs4 = {CodecEncoding.getCanonicalCodec(70), // (3,192)
        CodecEncoding.getCanonicalCodec(71), // (3,224)
        CodecEncoding.getCanonicalCodec(72), // (3,240)
        CodecEncoding.getCanonicalCodec(73), // (3,248)
        CodecEncoding.getCanonicalCodec(74) // (3,252)
    };

    public static BHSDCodec[] nonDeltaUnsignedCodecs5 = {CodecEncoding.getCanonicalCodec(93), // (4,192)
        CodecEncoding.getCanonicalCodec(94), // (4,224)
        CodecEncoding.getCanonicalCodec(95), // (4,240)
        CodecEncoding.getCanonicalCodec(96), // (4,248)
        CodecEncoding.getCanonicalCodec(97) // (4,252)
    };

    // Families of codecs for bands of positive integers that do correlate well
    // and would benefit from delta encoding

    public static BHSDCodec[] deltaUnsignedCodecs1 = {CodecEncoding.getCanonicalCodec(3), // (1,256,0,1)
        CodecEncoding.getCanonicalCodec(7), // (2,256,0,1)
        CodecEncoding.getCanonicalCodec(11), // (3,256,0,1)
        CodecEncoding.getCanonicalCodec(15) // (4,256,0,1)
    };

    public static BHSDCodec[] deltaUnsignedCodecs2 = {CodecEncoding.getCanonicalCodec(32), // (5,4,0,1)
        CodecEncoding.getCanonicalCodec(35), // (5,16,0,1)
        CodecEncoding.getCanonicalCodec(38), // (5,32,0,1)
        CodecEncoding.getCanonicalCodec(41), // (5,64,0,1)
        CodecEncoding.getCanonicalCodec(44) // (5,128,0,1)
    };

    public static BHSDCodec[] deltaUnsignedCodecs3 = {CodecEncoding.getCanonicalCodec(52), // (2,8,0,1)
        CodecEncoding.getCanonicalCodec(54), // (2,16,0,1)
        CodecEncoding.getCanonicalCodec(56), // (2,32,0,1)
        CodecEncoding.getCanonicalCodec(58), // (2,64,0,1)
        CodecEncoding.getCanonicalCodec(60), // (2,128,0,1)
        CodecEncoding.getCanonicalCodec(62), // (2,192,0,1)
        CodecEncoding.getCanonicalCodec(64), // (2,224,0,1)
        CodecEncoding.getCanonicalCodec(66), // (2,240,0,1)
        CodecEncoding.getCanonicalCodec(68) // (2,248,0,1)
    };

    public static BHSDCodec[] deltaUnsignedCodecs4 = {CodecEncoding.getCanonicalCodec(75), // (3,8,0,1)
        CodecEncoding.getCanonicalCodec(77), // (3,16,0,1)
        CodecEncoding.getCanonicalCodec(79), // (3,32,0,1)
        CodecEncoding.getCanonicalCodec(81), // (3,64,0,1)
        CodecEncoding.getCanonicalCodec(83), // (3,128,0,1)
        CodecEncoding.getCanonicalCodec(85), // (3,192,0,1)
        CodecEncoding.getCanonicalCodec(87), // (3,224,0,1)
        CodecEncoding.getCanonicalCodec(89), // (3,240,0,1)
        CodecEncoding.getCanonicalCodec(91) // (3,248,0,1)
    };

    public static BHSDCodec[] deltaUnsignedCodecs5 = {CodecEncoding.getCanonicalCodec(98), // (4,8,0,1)
        CodecEncoding.getCanonicalCodec(100), // (4,16,0,1)
        CodecEncoding.getCanonicalCodec(102), // (4,32,0,1)
        CodecEncoding.getCanonicalCodec(104), // (4,64,0,1)
        CodecEncoding.getCanonicalCodec(106), // (4,128,0,1)
        CodecEncoding.getCanonicalCodec(108), // (4,192,0,1)
        CodecEncoding.getCanonicalCodec(110), // (4,224,0,1)
        CodecEncoding.getCanonicalCodec(112), // (4,240,0,1)
        CodecEncoding.getCanonicalCodec(114) // (4,248,0,1)
    };

    // Families of codecs for bands containing positive and negative integers
    // that do correlate well (i.e. delta encoding is used)

    public static BHSDCodec[] deltaSignedCodecs1 = {CodecEncoding.getCanonicalCodec(4), // (1,256,1,1)
        CodecEncoding.getCanonicalCodec(8), // (2,256,1,1)
        CodecEncoding.getCanonicalCodec(12), // (3,256,1,1)
        CodecEncoding.getCanonicalCodec(16) // (4,256,1,1)
    };

    public static BHSDCodec[] deltaSignedCodecs2 = {CodecEncoding.getCanonicalCodec(33), // (5,4,1,1)
        CodecEncoding.getCanonicalCodec(36), // (5,16,1,1)
        CodecEncoding.getCanonicalCodec(39), // (5,32,1,1)
        CodecEncoding.getCanonicalCodec(42), // (5,64,1,1)
        CodecEncoding.getCanonicalCodec(45) // (5,128,1,1)
    };

    public static BHSDCodec[] deltaSignedCodecs3 = {CodecEncoding.getCanonicalCodec(53), // (2,8,1,1)
        CodecEncoding.getCanonicalCodec(55), // (2,16,1,1)
        CodecEncoding.getCanonicalCodec(57), // (2,32,1,1)
        CodecEncoding.getCanonicalCodec(59), // (2,64,1,1)
        CodecEncoding.getCanonicalCodec(61), // (2,128,1,1)
        CodecEncoding.getCanonicalCodec(63), // (2,192,1,1)
        CodecEncoding.getCanonicalCodec(65), // (2,224,1,1)
        CodecEncoding.getCanonicalCodec(67), // (2,240,1,1)
        CodecEncoding.getCanonicalCodec(69) // (2,248,1,1)
    };

    public static BHSDCodec[] deltaSignedCodecs4 = {CodecEncoding.getCanonicalCodec(76), // (3,8,1,1)
        CodecEncoding.getCanonicalCodec(78), // (3,16,1,1)
        CodecEncoding.getCanonicalCodec(80), // (3,32,1,1)
        CodecEncoding.getCanonicalCodec(82), // (3,64,1,1)
        CodecEncoding.getCanonicalCodec(84), // (3,128,1,1)
        CodecEncoding.getCanonicalCodec(86), // (3,192,1,1)
        CodecEncoding.getCanonicalCodec(88), // (3,224,1,1)
        CodecEncoding.getCanonicalCodec(90), // (3,240,1,1)
        CodecEncoding.getCanonicalCodec(92) // (3,248,1,1)
    };

    public static BHSDCodec[] deltaSignedCodecs5 = {CodecEncoding.getCanonicalCodec(99), // (4,8,1,1)
        CodecEncoding.getCanonicalCodec(101), // (4,16,1,1)
        CodecEncoding.getCanonicalCodec(103), // (4,32,1,1)
        CodecEncoding.getCanonicalCodec(105), // (4,64,1,1)
        CodecEncoding.getCanonicalCodec(107), // (4,128,1,1)
        CodecEncoding.getCanonicalCodec(109), // (4,192,1,1)
        CodecEncoding.getCanonicalCodec(111), // (4,224,1,1)
        CodecEncoding.getCanonicalCodec(113), // (4,240,1,1)
        CodecEncoding.getCanonicalCodec(115) // (4,248,1,1)
    };

    public static BHSDCodec[] deltaDoubleSignedCodecs1 = {CodecEncoding.getCanonicalCodec(34), // (5,4,2,1)
        CodecEncoding.getCanonicalCodec(37), // (5,16,2,1)
        CodecEncoding.getCanonicalCodec(40), // (5,32,2,1)
        CodecEncoding.getCanonicalCodec(43), // (5,64,2,1)
        CodecEncoding.getCanonicalCodec(46) // (5,128,2,1)
    };

    // Families of codecs for bands containing positive and negative values that
    // do not correlate well (i.e. delta encoding is not used)

    public static BHSDCodec[] nonDeltaSignedCodecs1 = {CodecEncoding.getCanonicalCodec(2), // (1,256,1)
        CodecEncoding.getCanonicalCodec(6), // (2,256,1)
        CodecEncoding.getCanonicalCodec(10), // (3,256,1)
        CodecEncoding.getCanonicalCodec(14) // (4,256,1)
    };

    public static BHSDCodec[] nonDeltaSignedCodecs2 = {CodecEncoding.getCanonicalCodec(18), // (5,4,1)
        CodecEncoding.getCanonicalCodec(21), // (5,16,1)
        CodecEncoding.getCanonicalCodec(24), // (5,32,1)
        CodecEncoding.getCanonicalCodec(27), // (5,64,1)
        CodecEncoding.getCanonicalCodec(30) // (5,128,1)
    };

    public static BHSDCodec[] nonDeltaDoubleSignedCodecs1 = {CodecEncoding.getCanonicalCodec(19), // (5,4,2)
        CodecEncoding.getCanonicalCodec(22), // (5,16,2)
        CodecEncoding.getCanonicalCodec(25), // (5,32,2)
        CodecEncoding.getCanonicalCodec(28), // (5,64,2)
        CodecEncoding.getCanonicalCodec(31) // (5,128,2)
    };

}

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
package org.apache.commons.compress.archivers.zip;

/**
 * Base class for all PKWare strong crypto extra headers.
 * 
 * <b>Algorithm IDs</b> - integer identifier of the encryption algorithm from
 * the following range
 * 
 * <ul>
 * <li>0x6601 - DES</li>
 * <li>0x6602 - RC2 (version needed to extract < 5.2)</li>
 * <li>0x6603 - 3DES 168</li>
 * <li>0x6609 - 3DES 112</li>
 * <li>0x660E - AES 128</li>
 * <li>0x660F - AES 192</li>
 * <li>0x6610 - AES 256</li>
 * <li>0x6702 - RC2 (version needed to extract >= 5.2)</li>
 * <li>0x6720 - Blowfish</li>
 * <li>0x6721 - Twofish</li>
 * <li>0x6801 - RC4</li>
 * <li>0xFFFF - Unknown algorithm</li>
 * </ul>
 * 
 * <b>Hash Algorithms</b> - integer identifier of the hash algorithm from the
 * following range
 * 
 * <ul>
 * <li>0x0000 - none</li>
 * <li>0x0001 - CRC32</li>
 * <li>0x8003 - MD5</li>
 * <li>0x8004 - SHA1</li>
 * <li>0x8007 - RIPEMD160</li>
 * <li>0x800C - SHA256</li>
 * <li>0x800D - SHA384</li>
 * <li>0x800E - SHA512</li>
 * </ul>
 *
 * TODO: define enums for crypto and hash algorithms.
 */
public abstract class PKWareExtraHeader implements ZipExtraField {

}

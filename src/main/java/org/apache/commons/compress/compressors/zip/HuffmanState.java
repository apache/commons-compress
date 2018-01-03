package org.apache.commons.compress.compressors.zip;

enum HuffmanState {
    INITIAL,
    STORED,
    DYNAMIC_CODES,
    FIXED_CODES
}

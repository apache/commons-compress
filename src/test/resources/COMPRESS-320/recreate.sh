#!/bin/bash

rm *.7z
for COMPRESSION in "LZMA" "LZMA2" "PPMd" "BZip2" "Deflate" "Copy"; do
  # New solid block every 10 files.
  7za a -m0=$COMPRESSION -ms10f  $COMPRESSION-solid.7z ../../../../src/main/java/org/apache/commons/compress/compressors
  # Each file in isolation
  7za a -m0=$COMPRESSION -ms=off $COMPRESSION.7z       ../../../../src/main/java/org/apache/commons/compress/compressors
done

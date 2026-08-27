<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# bzip2 decompression performance notes

Working notes for the single-threaded `BZip2CompressorInputStream` rewrite (branch `bzip2-llm-rewrite`).
Goal: a measured 1.4-1.8x (or better) decompression speedup, byte-for-byte compatible with the previous
decoder, which is kept verbatim as the test-only differential oracle
`src/test/java/org/apache/commons/compress/compressors/bzip2/LegacyBZip2Decoder.java`.

## Environment

- CPU: 64 logical cores, 125 GB RAM (single-threaded benchmarks; the JMH runs use one fork, one thread).
- JDK: Temurin 25.0.3 (`--release 8` compilation via the `java-9-up` profile). Maven 3.9.16.
- Native reference: `bzip2 -dc` of the full corpus below takes 74 s wall = 45 MB/s of output.

## Corpora

The user-provided corpus is `~/Downloads/enwiki_general-20260823-00000.json.bz2` (484,289,618 bytes,
level 9, decompresses to 3,364,783,751 bytes of JSON). The benchmark slices live in
`~/Downloads/bzip2-bench/` and were generated with the native `bzip2` (reference encoder, not ours):

```sh
B=~/Downloads/bzip2-bench; mkdir -p $B; cd $B
bzip2 -dc ~/Downloads/enwiki_general-20260823-00000.json.bz2 | head -c 67108864 > enwiki-64M.txt
bzip2 -9 -k -c enwiki-64M.txt > enwiki-64M.9.bz2
bzip2 -1 -k -c enwiki-64M.txt > enwiki-64M.1.bz2
tar cf - -C ~/.sdkman/candidates/java/25.0.3-tem lib | head -c 67108864 > binary-64M.tar   # ELF, jimage, jars
bzip2 -9 -k -c binary-64M.tar > binary-64M.9.bz2
sha256sum enwiki-64M.txt binary-64M.tar > corpora.sha256
```

| corpus              | compressed bytes | decompressed bytes |
|---------------------|-----------------:|-------------------:|
| enwiki-64M.9.bz2    |        9,864,422 |         67,108,864 |
| enwiki-64M.1.bz2    |       13,883,271 |         67,108,864 |
| binary-64M.9.bz2    |       23,501,431 |         67,108,864 |
| full enwiki (-9)    |      484,289,618 |      3,364,783,751 |

## How to run

```sh
# correctness
mvn -q test -Dtest=BZip2DifferentialTest
# JMH (per-decode time in ms; MB/s = 67.1 / (ms / 1000))
mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp target/test-classes:target/classes:$(cat target/cp.txt) org.openjdk.jmh.Main BZip2DecompressionBenchmark \
    -jvmArgs -Dbzip2.bench.dir=$HOME/Downloads/bzip2-bench
# full-file, single shot (wall time, MB/s, CRC-32 of the output)
java -cp target/test-classes:target/classes:$(cat target/cp.txt) org.apache.commons.compress.compressors.bzip2.BZip2DecodeMain \
    ~/Downloads/enwiki_general-20260823-00000.json.bz2 current
```

## Results (MB/s of decompressed output, JMH average of 5 iterations unless noted)

| step                                   | enwiki -9 | enwiki -1 | binary -9 | full file (single shot) |
|----------------------------------------|----------:|----------:|----------:|------------------------:|
| baseline (legacy, commit f8106cd28)    |      39.9 |      39.3 |      28.7 |                    38.8 |
| step 1: bit reader + Huffman tables    |      50.8 |      56.2 |      41.8 |                         |
| step 2: packed tt + fused bulk read    |      53.4 |      57.2 |      42.3 |                   (tbd) |
| native `bzip2 -dc`                     |         - |         - |         - |                    45.5 |

Baseline JMH raw numbers (ms per 64 MiB decode): 1681 ± 131, 1706 ± 97, 2342 ± 114 (`jmh-step0-baseline.json`
in the corpus directory). Full file: 86.65 s, CRC-32 `e4469d34` (matches `bzip2 -dc`). Note the previous
decoder was already at 85% of native libbzip2 on this corpus, much closer than the original brief assumed.

JFR profile of the baseline on enwiki-64M -9 (top frames): `CRC.compute` 41%, `getAndMoveToFrontDecode` 18%,
`BitInputStream.readCachedBits/ensureCache` 20%, `setupBlock` 8%, `Arrays.fill` 6%, `HuffmanDecoder.decodeSymbol`
3%. The CRC share is mostly attribution skid from the dependent `ll8[tPos]`/`tt[tPos]` cache misses whose
result feeds the CRC table lookup, i.e. the inverse-BWT output path and the Huffman/MTF path each cost
roughly half.

## Deviations from the previous decoder

- After an exception caused by *corrupt* (not truncated) data inside a block, the underlying input stream
  may have been read up to 8 bytes further than before (bit-buffer lookahead). Unobservable on valid or
  truncated streams; `getCompressedCount()` is unchanged in all cases.
- A corrupt `origPtr` (outside the block, `origPtr > last`) now fails immediately with "Stream corrupted".
  The previous decoder only rejected `origPtr >= tt.length`, where `tt.length` was the largest block seen so
  far in the stream, and otherwise returned garbage bytes before failing with "BZip2 CRC error". This
  matches libbzip2 (`origPtr >= nblock`).

## Log

- Step 0: harness. `LegacyBZip2Decoder` (verbatim copy), `BZip2DifferentialTest` (generated inputs at
  block sizes 1 and 9, all fixtures, read-pattern matrix, concatenation, trailing garbage, truncation,
  bit flips, randomised blocks via `RandomisedBZip2Streams`, read-after-close), `BZip2DecompressionBenchmark`
  (JMH), `BZip2DecodeMain` (full-file runner).
- Step 1: `BZip2BitReader` (64-bit buffer, lazy exact reads for headers/trailers, greedy capped refill inside
  block bodies) replaces `BitInputStream`; per-group 10-bit Huffman lookup tables with a canonical bit-by-bit
  slow path replace `HuffmanDecoder`; the MTF/RLE2 loop keeps its state in locals with a single decode site.
  The per-byte output state machine is unchanged. JMH (ms per 64 MiB, current vs legacy in the same run):
  enwiki -9 1322 ± 63 vs 1685 ± 149 (1.27x), enwiki -1 1194 ± 93 vs 1603 ± 38 (1.34x), binary -9 1605 ± 75 vs
  2179 ± 50 (1.36x) (`jmh-step1.json`). Harness findings while getting here: the compressed-byte count after a
  failed read must report every byte pulled (the old `BoundedInputStream` counter did), and after a data
  error inside a block the underlying stream may now be up to 8 bytes further ahead (see deviations).
- Step 2 (plan steps 2a+2b merged so the hot loop is written once; -1 vs -9 separate the effects): `ll8` is
  folded into `tt` (byte in the low 8 bits, successor index in the upper 24), `read(byte[])` runs the
  `setupNoRandPartA/B/C` state machine in a local loop writing straight into the caller's buffer, CRC is
  computed over the written slice, `origPtr` is validated against the block length before the in-place
  transform. JMH: enwiki -9 1256 ± 111 vs 1642 ± 128 (1.31x), enwiki -1 1173 ± 46 vs 1638 ± 136 (1.40x),
  binary -9 1587 ± 69 vs 2158 ± 79 (1.36x) (`jmh-step2.json`). Smaller step than hoped: the profile shows the
  `tt[tPos]` chase is now 42% of the time and latency bound (one dependent load per byte); `Arrays.fill`
  for RUNA/RUNB runs 14%, the CRC loop 13%, the inverse-BWT setup pass 10%, the MTF loop ~16%, slow-path
  Huffman 2%.

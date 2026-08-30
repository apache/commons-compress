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
| step 2: packed tt + fused bulk read    |      53.4 |      57.2 |      42.3 |                    54.2 |
| step 3a: fills, CRC slicing, dead checks |    58.7 |      62.2 |      44.7 |                         |
| step 3b: multi-chain inverse BWT (paired, pinned) | 74.0 |  64.1 |      51.6 |                    69.9 |
| step 3d: refill/table-section reads          |  74.6 |      65.5 |      52.1 |                    73.0 |
| step 5: bulk input for markable/concatenated sources | 74.7 |  66.2 |      54.0 |                         |
| legacy in the same pinned run          |      42.1 |      37.3 |      30.6 |                         |
| native `bzip2 -dc`                     |         - |         - |         - |                    45.5 |

Baseline JMH raw numbers (ms per 64 MiB decode): 1681 ± 131, 1706 ± 97, 2342 ± 114 (`jmh-step0-baseline.json`
in the corpus directory). Full file: 86.65 s, CRC-32 `e4469d34` (matches `bzip2 -dc`). Note the previous
decoder was already at 85% of native libbzip2 on this corpus, much closer than the original brief assumed.

JFR profile of the baseline on enwiki-64M -9 (top frames): `CRC.compute` 41%, `getAndMoveToFrontDecode` 18%,
`BitInputStream.readCachedBits/ensureCache` 20%, `setupBlock` 8%, `Arrays.fill` 6%, `HuffmanDecoder.decodeSymbol`
3%. The CRC share is mostly attribution skid from the dependent `ll8[tPos]`/`tt[tPos]` cache misses whose
result feeds the CRC table lookup, i.e. the inverse-BWT output path and the Huffman/MTF path each cost
roughly half.

## Future work (not done)

- Bulk input buffering for sources that neither support `mark`/`reset` nor are consumed to the end (a bare
  `FileInputStream` with `decompressConcatenated=false`): not possible without over-reading; wrap such
  sources in a `BufferedInputStream` (which is markable and therefore gets the bulk path).
- The randomised-block path is still byte-at-a-time (it was never fast and no encoder produced such blocks
  since bzip2 0.9.5).
- Transparent huge pages (`-XX:+UseTransparentHugePages`) should help the random accesses over the 3.6 MB
  `tt` at block size 9; a JVM flag, not a code change, so not measured here.

## Compression (step 7): micro-level changes to the encoder, output byte-identical

Native `bzip2 -9` compresses the 64 MiB text in 4.27 s (15.7 MB/s); the Java encoder took 6.85 s. JFR at -9:
`BlockSort.mainSimpleSort` 41% (the inlined `mainGtU` rotation compare), `mainQSort3` 21%, `mainSort` 10%,
`generateMTFValues` 5%, the Huffman cost fitting (`sendMTFValues1`) 5%, `CRC.update(int, repeat)` 4%, the
per-byte RLE1 entry (`write0`) 2%. All sort methods are C2-compiled (no huge-method cliff). Harness:
`LegacyBZip2Encoder` + `LegacyBlockSort` (verbatim copies) and `BZip2CompressionDifferentialTest` (110 cases:
generated inputs and fixtures at block sizes 1 and 9, four write patterns; compressed bytes must be identical
and must round-trip), `BZip2CompressionBenchmark` (JMH single shot, 64 MiB text and binary, -9 and -1).
Single-shot runs of ~6 s vary by 3-5% between sessions even pinned, so only consistent moves across all four
combinations count.

- Change 1: `mainSimpleSort` compares the first six bytes of two rotations as one word (`ByteBuffer.getLong`
  on the block, top 48 bits, unsigned compare = lexicographic order) and then four bytes plus the four
  quadrant values per step as an `int` and a packed `long`, deciding by the first differing element in the
  original interleaved order (byte k before quadrant k, via leading-zero counts). `NUM_OVERSHOOT_BYTES`
  20 -> 32 for the 8-byte loads (layout only). Assembling the words from byte loads instead of the
  `ByteBuffer` was both slower (7.05 vs 6.25 s) and, in its first version, wrong for bytes >= 0x80 (an int
  sub-expression sign-extended into the long) - caught by the differential test. `Buffer.checkIndex` still
  costs ~13% of samples; there is no Java 8 way around it.
- Change 2: the six code lengths of a symbol packed into one `long` with 10-bit lanes (a group is at most
  50 symbols of at most 20 bits, so lanes cannot overflow), so the cost of a group is one load and one add
  per symbol instead of six; `write(byte[], int, int)` runs the RLE1 state machine with the run in locals.
- Timings (ms, single shot, pinned; baseline / change 1 / change 2): enwiki -9 6458 / 6253 / 5999;
  binary -9 6840 / 6348 / 5973; enwiki -1 5648 / 5322 / 5089; binary -1 6045 / 5645 / 5739. Paired run of
  the final state, legacy vs current in one session: 6522 vs 6116, 6593 vs 6218, 5696 vs 5134, 6114 vs 5865
  (-6%, -6%, -10%, -4%; `jmh-compress-final.json`). The remaining time is the sort's branch mispredictions
  and dependent loads, which need a different sorting algorithm (or parallel blocks) rather than
  micro-optimisation; the Java encoder is still ~1.4x slower than native bzip2.

## Compression (step 8, experiment): suffix-array rotation sort

`SuffixArraySort` (SA-IS, Nong/Zhang/Chan induced sorting, linear time, textbook version with explicit type
and bucket arrays) on the doubled block plus a sentinel: the suffixes of `B·B` that start in the first half,
truncated to `n`, are the rotations of `B`, so filtering the suffix array gives bzip2's cyclic order without a
cyclic comparison variant. Selected per stream by the system property
`org.apache.commons.compress.bzip2.suffixArraySort`. Correct: identical compressed bytes to the original
sort on every aperiodic input (27 cases against the original encoder and a naive rotation sort); for
periodic blocks (equal rotations) only the origin pointer differs and the stream round-trips. One
integration trap: the encoder reads `block[0]` (the wrap-around byte) which the original `mainSort` sets as
a side effect. The code was dropped after the measurement (this entry is the record; it lived briefly as
`SuffixArraySort` behind a system property).

Result: about 2x slower than the block sort. Single shot, 64 MiB, ms: enwiki -9 11589 vs 6088, binary -9
11584 vs 6023, enwiki -1 9675 vs 5129, binary -1 9617 vs 5784 (`jmh-compress-sais.json`). Sorting takes
~130 ms per 900 KB block with SA-IS versus ~60 ms with libbzip2's radix + quicksort + shell sort. The
doubling accounts for roughly half of that; the rest is the induced-sorting passes over 2n+1 ints of text
and suffix array (~14 MB at -9) plus the LMS naming pass, all random access. A textbook SA-IS in Java does
not beat a tuned block sort at this block size; only a libsais-class implementation (weeks of work) or
avoiding the doubling might reach or exceed parity, and neither promises the 2-3x that the C literature
suggests, because the block sort already exploits the 900 KB block size well. Not adopted.

## Parallel decompression (step 9): block-parallel decoding on an `ExecutorService`

A new constructor `BZip2CompressorInputStream(InputStream, boolean decompressConcatenated, ExecutorService,
int maxConcurrentInFlight)` decodes blocks concurrently (`ParallelBZip2Decoder`). bzip2 blocks are
independent apart from the bit-level packing and the combined CRC, so the coordinator scans the compressed
stream for the 48-bit block/EOS magics at all 8 bit shifts (sliding 64-bit window over a rolling 512 KB
buffer), slices the stream into per-block bit segments, and hands each segment to the executor as a
synthesized single-block bzip2 stream (`BZh<n>` + the segment's bits shifted to offset 32 + EOS + the
block's stored CRC as the combined CRC). Each worker runs the regular single-threaded decoder on its
synthesized stream, so the whole step reuses the sequential code path, checks and messages; results are
delivered in order via `Future`s. The stored per-block CRCs are folded on the coordinator
(`combined = rotl1(combined) ^ blockCRC`) and checked against the stream's combined CRC at EOS, so
corruption detection is equivalent to the sequential decoder's. A 48-bit magic can appear by chance inside
a block (or a block can be corrupt): a failed worker triggers a rescan that ignores the false delimiter and
re-splits from the failed block's start; parse errors found while reading ahead are deferred until every
block before them has decoded, since a false-positive delimiter makes the parser read garbage. The number
of blocks read ahead (and hence the memory bound: compressed bits plus up to 900 KB of output per in-flight
block) is the explicit `maxConcurrentInFlight` argument - the executor supplies the threads, the caller
controls the concurrency. The parallel stream reads ahead of the logical end of the bzip2 stream, so it
does not keep the sequential constructor's positioning guarantee (documented in the javadoc).

Results (full 484 MB corpus, `BZip2DecodeMain`, machine otherwise idle): sequential 46.9 s = 71.7 MB/s;
16 threads 16.6 s = 203 MB/s (2.8x); 32 threads 15.7 s = 214 MB/s (3.0x); output CRC-32 identical. The
scaling flattens because the single consumer thread (segment scan + bit-shift copies + delivery + the
benchmark's own CRC over 3.3 GB) becomes the bottleneck. The sequential path only gained a null field
check per read call: pinned JMH 869 +- 10 / 1028 +- 4 / 1249 +- 12 ms vs 899 / 1013 / 1243 before, i.e.
within the +-2-3% session-to-session spread of these runs, with the -9 corpora inside the error bars.
Tests: `ParallelBZip2DecompressionTest` (generated inputs x block sizes x pool shapes, byte-at-a-time
reads, counters, concatenated streams, 25 seeded bit flips + truncations + a CRC flip, all fixtures, a
randomised block, and an injected-spurious-magic rescan via a test hook).

## Port to libbzip2 (C), uncommitted in `~/mounts/ocean/work/others/bzip2`

The same three ideas were applied to bzip2 1.0.8 (`decompress.c`, `bzlib.c`, `crctable.c`, `huffman.c`,
`bzlib_private.h`; left uncommitted there): per-group 10-bit Huffman lookup tables with a fall-back to the
original resumable bit-by-bit loop (`GET_MTF_VAL`), the 8-chain inverse BWT into a materialised block buffer
with the RLE1 loop streaming from it (randomised blocks and "small" mode keep the old paths), the block CRC
by slicing-by-8 over each output slice, and the bit buffer / input pointer of `BZ2_decompress` kept in
locals with a single write-back. Verified against the original binary on 109 inputs (stdout hash, exit code,
stderr) and, through the library API with 1-byte input/output slices, against the original library on all
generated valid, truncated and corrupt inputs (0 differences), plus `make test`. Pinned timings of
`bzip2 -dc > /dev/null` (s): enwiki -9 1.44 -> 1.15 (tables) -> 0.94 (unwind + CRC) -> 0.91 (locals);
enwiki -1 1.38 -> 1.01 -> 1.02 -> 0.99; binary -9 1.86 -> 1.45 -> 1.23 -> 1.20; full 3.4 GB file 74 -> 44.8 s.
The Java decoder and the patched C library end up within a few percent of each other.

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
  Huffman 2%. Full file: 62.09 s, CRC-32 matches.
- Step 3a: manual fill for RUNA/RUNB runs shorter than 32 (was `Arrays.fill`, 14% of samples), CRC slicing-by-8
  over the output slice (`CRCTest` checks it against the byte-wise update and the CRC-32/BZIP2 check value),
  branchless RUNA/RUNB accumulation (`runLength += runWeight << nextSym`), removal of the provably unreachable
  `'yy'`, `'nextSym'` and `'tt index'` bounds checks (comments explain why). JMH (current only): enwiki -9
  1143 ± 116, enwiki -1 1079 ± 92, binary -9 1502 ± 60 (`jmh-step3a.json`), i.e. -9%, -8%, -5%.
- Step 3b: multi-chain inverse BWT (`BZip2InverseBwt`). The `tt[tPos]` chase is a single dependency chain
  with ~10 ns of L3 latency per byte for 900k blocks (3.6 MB `tt`, L2 is 512 KB on this Zen 2 box). The
  block is now unwound by 8 chains walked in lockstep: each chain starts at a flagged row and stops when it
  reaches another chain's start row, then restarts at a row nobody has visited (two spare bits of the `tt`
  entries serve as flags), and the segments are stitched in cycle order into `data.raw`. The RLE1 state
  machine then streams over `raw` instead of chasing pointers. Output is identical by construction, including
  the corrupt-input case where the permutation has several cycles (the old traversal repeats the first cycle;
  `raw[n]` reproduces the repeat-count byte it would read after the last byte). Isolated micro-benchmark on a
  random 900k permutation: chase 10.9 ns/byte, first version (chain state in arrays) 5.7, final version
  (chain state in locals, 8 unrolled chains) 3.2; 4 chains 4.0, 16 chains worse (JIT). 100k rows: no change
  (L2 resident). JMH (current only): enwiki -9 944 ± 135, enwiki -1 1072 ± 65, binary -9 1316 ± 82
  (`jmh-step3b2.json`), i.e. -17%, 0%, -12% vs step 3a. Memory per decoder at -9 grows from 4.5 MB (old
  `tt` + `ll8`) to about 6 MB (`tt`, `raw`, chain pool). A bug found by the existing `BZip2NSelectorsOverflowTest`
  (a 5-byte block hung the walk: fewer rows than chains) is covered by `BZip2InverseBwtTest` now.
  Final paired run for step 3, both decoders in one JMH invocation pinned to one core (`taskset -c 6`, which
  shrinks the error bars from ~10% to <1%): enwiki -9 907 ± 4 vs 1595 ± 3 ms (1.76x), enwiki -1 1047 ± 8 vs
  1799 ± 35 (1.72x), binary -9 1301 ± 6 vs 2194 ± 286 (1.69x) (`jmh-step3.json`). Full file: 48.12 s =
  69.9 MB/s (1.80x the previous decoder, 1.54x native `bzip2 -dc`), CRC-32 matches.
- Quality gates at the end of step 3: full `mvn test` green, `checkstyle:check`, `pmd:check`, `apache-rat:check`
  clean, SpotBugs findings identical to master (none in the bzip2 package).
- Step 3d (kept, at the threshold): refill the bit buffer when fewer than `FAST_BITS + 1` bits remain (6
  bytes per refill instead of 4-5), and read the whole table section of a block (bitmap, selectors, code
  lengths) with the greedy in-block refill plus a leading-ones unary decoder for the selector MTF codes
  (`BZip2BitReader.readBitsInBlock`, `readUnaryInBlock`; the section precedes the block's Huffman data, so
  the 80-bit trailer argument covers it). Pinned JMH: 900 ± 6 / 1024 ± 6 / 1289 ± 11 ms vs 907 / 1047 /
  1301 (-0.8%, -2.2%, -0.9%) (`jmh-step3d.json`). Final full-file run (pinned, machine otherwise idle):
  46.07 s = 73.0 MB/s, 1.88x the previous decoder and 1.61x native `bzip2 -dc`; CRC-32 `e4469d34` matches.
  Final gates: full `mvn test`, `checkstyle:check`, `pmd:check`, `apache-rat:check`, `japicmp:cmp` all pass;
  SpotBugs unchanged versus master.
- Step 4a (Java 25 check): the identical sources compiled with `--release 25` (scratch build, same JDK 25
  runtime) give 910 ± 5 / 1038 ± 14 / 1304 ± 16 ms vs 907 / 1047 / 1301 for the `--release 8` build: no
  difference. The hot loops are plain int/array code, so the bytecode level cannot matter; only new APIs
  could (see step 4b).
- Step 3c (tried, reverted): keep the MTF output in a `byte[]` again and let the pointer pass store complete
  entries (`tt[cftab[b]++] = i << 8 | b`, i.e. each entry carries its successor's byte and the walk starts at
  `origPtr`), to cut the 4-byte-per-symbol write/read traffic of the packed layout. Pinned JMH: 897 ± 14 /
  1020 ± 38 / 1297 ± 13 ms vs 907 / 1047 / 1301, i.e. -1%, -2.6%, 0% for 900 KB more memory per decoder.
  Not worth it (`jmh-step3c.json`).
- Step 4b (Java 25 APIs, decision): after step 3 the profile is: MTF/Huffman loop ~37%, multi-chain walk
  19%, inverse-BWT pointer pass 15%, RLE1 output loop 8%, bit-buffer refill 7%, header/table bit reads 4%,
  CRC 3%. The only parts a newer API could touch are the refill (a `VarHandle` long view instead of the
  byte-assembly loop; needs an input buffer with 8 bytes of lookahead, i.e. `mark`/`reset` on the source
  stream to keep the end-of-stream positioning guarantee, plus a fallback path), the CRC (`java.util.zip.CRC32`
  is intrinsified but computes the reflected CRC; bzip2's is the non-reflected one, so every byte would have
  to be bit-reversed first, which costs about as much as the sliced CRC itself) and, marginally, the header
  reads. Their combined share is ~14%, and realistic savings are a third of that. The hot loops themselves
  (MTF, walk, pointer pass) are scalar int/array code with no Java 9+ counterpart, and the Vector API is an
  incubator module a library cannot depend on. That is well below the 10-15% bar set for a multi-release
  jar, so no `src/main/java25` variant is added; the `mark`/`reset` bulk-input idea is Java 8 compatible
  anyway and listed under future work.
- Step 4c (measured, scratch only, not committed): two variants of `BZip2BitReader` with a 64 KB internal
  input buffer, refilled 8 bytes at a time: A assembles the long from bytes by hand (Java 8), B uses
  `MethodHandles.byteArrayViewVarHandle(long[].class, BIG_ENDIAN)` (`--release 9`). Both over-read the source,
  so they are valid only where positioning does not matter (`decompressConcatenated=true`, or with
  `mark`/`reset`). Pinned JMH vs the tree's 900 / 1024 / 1289 ms: A 880 ± 12 / 980 ± 8 / 1248 ± 18 (-2 to -4%),
  B 874 ± 6 / 997 ± 6 / 1223 ± 5 (-3 to -5%); A vs B is within ±2% with mixed sign, so the VarHandle itself
  buys nothing measurable and the whole gain is the bulk input read. Where it does matter is an unbuffered
  source: on `Files.newInputStream` (warm, 64 MiB -9) the tree does 33.9 MB/s vs 71.1 when the caller wraps
  it in a `BufferedInputStream`; A/B do 75 MB/s either way (the old decoder: 7.5 vs 37.0). Conclusion: a
  Java 8 internal buffer for the cases where over-reading is allowed is worth doing as a follow-up; a Java 9+
  variant is not (`jmh-step4-variantA.json`, `jmh-step4-variantB.json`).
- Step 5 (bulk input, Java 8): `BZip2BitReader` gains a bulk mode with a 64 KB input buffer and 8-byte refills
  (the variant A code from step 4c). It is used when reading ahead of the bzip2 stream cannot matter, i.e.
  `decompressConcatenated=true` (the input is consumed to its end), and when the source supports
  `mark`/`reset` (`BufferedInputStream`, byte-array streams): the source is then re-synchronised to the
  first unconsumed byte at every chunk boundary (`reset`, `skip`, drop the whole lookahead bytes from the bit
  buffer, `mark`, read the next chunk), at the end of the bzip2 stream and after an exception, so it ends up
  exactly where the exact reader leaves it, including after data errors (the exact reader may be up to 8
  bytes ahead there). Other sources keep the exact reader. The differential harness runs every case with a
  markable and a non-markable input; for markable inputs it demands the exact source position even after
  errors, which caught a stale write-back of the MTF loop's local bit-buffer copy after an exception from
  the slow Huffman path (the reader then under-counted the consumed bits; now the write-back is skipped
  while the reader owns the state). Also found on the way: re-syncing at a chunk boundary must keep reading
  until the chunk holds more than the re-read lookahead bytes, or a source with only those bytes left is
  re-read forever. Pinned JMH, byte-array source (markable), `concatenated=false`: 899 ± 9 / 1013 ± 6 /
  1243 ± 8 ms vs 900 / 1024 / 1289 (`jmh-step5.json`); a `BufferedInputStream` over the 64 MiB file: 73.4 MB/s
  (71.1 with the exact reader); an unbuffered `Files.newInputStream` is unchanged at 33.5 MB/s (not markable).
  `concatenated=true` (bulk without re-syncing): 905 ± 7 / 1022 ± 6 / 1246 ± 9 ms (`jmh-step5-concat.json`).
- Not pursued: `FAST_BITS` 11/12 (the slow Huffman path is 2% of samples at 10, below the benchmark noise),
  a two-level MTF list (the MTF shift loop is ~9%, mostly for small moves where it is already a few
  instructions).

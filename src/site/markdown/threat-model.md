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
# Apache Commons Compress Threat Model (draft)

This is a work in progress.

## Introduction

This document amends the
[Apache Commons Threat Model 0.1.0](https://commons.apache.org/threat-model-0.1.0.html)
(the "Commons model") for Apache Commons Compress.
Everything the Commons model says applies to Compress
unless this document says otherwise.
Definitions, user types, the JDK-parity principle,
the trust-boundary rules and the triage dispositions
are taken from the Commons model and are not repeated here.

A condensed version is available in the
[threat model summary](threat-model-short.html).
Security researchers should use this document;
developers should use the summary.

For information about reporting or asking questions about security,
see the [Apache Commons Security page](https://commons.apache.org/security.html).
Vulnerabilities fixed in released versions are listed on the
[Commons Compress Security Reports page](https://commons.apache.org/proper/commons-compress/security.html).

## Scope

Commons Compress belongs to the "Parsers / format readers" group of the Commons model.
Its recurring untrusted-input surface is
the bytes of archives and compressed streams
that a host application reads:
packages, uploads, downloads, backups and container layers.

This document concerns **reading**:
`ArchiveInputStream` subclasses, `ZipFile`, `TarFile`, `SevenZFile`,
`CompressorInputStream` subclasses,
and the `ArchiveStreamFactory` and `CompressorStreamFactory` methods that create them.
Writing archives from host-supplied data follows the Commons model unchanged:
the input of a writer is trusted.

### Covered formats

The resource guarantees in "Security properties" below
apply only to the formats, entry points and compression methods
marked **Yes** in the tables of this section and only from the Compress version specified in the "Since" column.

Formats marked **No** are intended for trusted input only.
They are legacy formats, structurally complex, and rarely used on untrusted input.
They remain available, and the properties that apply to all formats
(see "Properties for all formats") still hold for them.

**A format, entry point or compression method that does not appear in these tables is not covered.**
This includes formats added to Commons Compress after this document was last revised,
and formats contributed by third-party `ArchiveStreamProvider` or `CompressorStreamProvider` implementations.
Adding a format to Commons Compress does not add it to this model;
see "Conditions that would change this model."

#### Archivers

| Format | Factory name | Entry point              | Covered | Since | Notes                                                                                 |
|--------|--------------|--------------------------|---------|-------|---------------------------------------------------------------------------------------|
| AR     | `ar`         | `ArArchiveInputStream`   | Yes     | 1.0   | BSD and GNU variants, including `.deb` packages and static libraries.                 |
| ARJ    | `arj`        | `ArjArchiveInputStream`  | **No**  |       | Legacy.                                                                               |
| CPIO   | `cpio`       | `CpioArchiveInputStream` | Yes     | 1.0   | Old binary, odc, newc and crc variants, including RPM payloads and initramfs images.  |
| DUMP   | `dump`       | `DumpArchiveInputStream` | **No**  |       | Legacy.                                                                               |
| LHA    | `lha`        | `LhaArchiveInputStream`  | **No**  |       | Legacy. Detected heuristically, since the format has no magic bytes.                  |
| 7z     | `7z`         | `SevenZFile`             | Yes     | 1.6   | Every coder listed below, from the version given there.                               |
| TAR    | `tar`        | `TarArchiveInputStream`  | Yes     | 1.0   | Including PAX, GNU long-name and sparse extensions, from the version that added them. |
| TAR    | `tar`        | `TarFile`                | Yes     | 1.21  |                                                                                       |
| ZIP    | `zip`        | `ZipArchiveInputStream`  | Yes     | 1.0   | Every compression method listed below, from the version given there.                  |
| ZIP    | `zip`        | `ZipFile`                | Yes     | 1.0   | Same. Split archives, opened through the split-archive API, since 1.20.               |
| JAR    | `jar`        | `JarArchiveInputStream`  | Yes     | 1.0   | Read as ZIP.                                                                          |
| APK    | `apk`        | `JarArchiveInputStream`  | Yes     | 1.22  | Factory name for JAR.                                                                 |
| XAPK   | `xapk`       | `JarArchiveInputStream`  | Yes     | 1.22  | Factory name for JAR.                                                                 |
| APKS   | `apks`       | `JarArchiveInputStream`  | Yes     | 1.22  | Factory name for JAR.                                                                 |
| APKM   | `apkm`       | `JarArchiveInputStream`  | Yes     | 1.22  | Factory name for JAR.                                                                 |

#### Compression methods inside archives

An archive's contents can use any method the format allows,
and the author of the archive chooses it.
A covered archiver therefore covers every method it can decode.

| Archiver | Method                                              | Covered | Since  | Notes                                                              |
|----------|-----------------------------------------------------|---------|--------|--------------------------------------------------------------------|
| ZIP      | Stored                                              | Yes     | 1.0    |                                                                    |
| ZIP      | Deflated                                            | Yes     | 1.0    | Decoded by `java.util.zip.Inflater`.                               |
| ZIP      | Shrunk (unshrinking)                                | Yes     | 1.7    | Legacy PKWARE method, small fixed tables.                          |
| ZIP      | Imploded (exploding)                                | Yes     | 1.7    | Legacy PKWARE method, small fixed tables.                          |
| ZIP      | BZIP2                                               | Yes     | 1.11   |                                                                    |
| ZIP      | Enhanced deflated (Deflate64)                       | Yes     | 1.16   |                                                                    |
| ZIP      | Zstandard                                           | Yes     | 1.28.0 | See "Third-party decoders".                                        |
| ZIP      | XZ                                                  | Yes     | 1.28.0 | See "Third-party decoders".                                        |
| 7z       | Copy, Deflate, BZIP2                                | Yes     | 1.6    |                                                                    |
| 7z       | LZMA, LZMA2                                         | Yes     | 1.6    | Dictionary size declared in the input; see guarantee M2.           |
| 7z       | AES-256 + SHA-256                                   | Yes     | 1.6    | Key-derivation work factor declared in the input; see guarantee T. |
| 7z       | BCJ filters (x86, PPC, IA64, ARM, ARM Thumb, SPARC) | Yes     | 1.8    |                                                                    |
| 7z       | Delta                                               | Yes     | 1.8    |                                                                    |
| 7z       | Deflate64                                           | Yes     | 1.16   |                                                                    |

Other ZIP methods are rejected with `UnsupportedZipFeatureException`
and are therefore outside the model.

#### Compressors

| Format    | Factory name    | Entry point                         | Covered | Since | Memory determined by                                                              |
|-----------|-----------------|-------------------------------------|---------|-------|-----------------------------------------------------------------------------------|
| Brotli    | `br`            | `BrotliCompressorInputStream`       | Yes     | 1.14  | Window declared in the input, bounded by the format.                              |
| BZIP2     | `bzip2`         | `BZip2CompressorInputStream`        | Yes     | 1.0   | Block size fixed by the format (at most 900 kB).                                  |
| Deflate   | `deflate`       | `DeflateCompressorInputStream`      | Yes     | 1.9   | 32 KiB window.                                                                    |
| Deflate64 | `deflate64`     | `Deflate64CompressorInputStream`    | Yes     | 1.16  | 64 KiB window.                                                                    |
| GZIP      | `gz`            | `GzipCompressorInputStream`         | Yes     | 1.0   | 32 KiB window.                                                                    |
| LZ4       | `lz4-block`     | `BlockLZ4CompressorInputStream`     | Yes     | 1.14  | 64 KiB window.                                                                    |
| LZ4       | `lz4-framed`    | `FramedLZ4CompressorInputStream`    | Yes     | 1.14  | 64 KiB window and bounded block size; skippable frames are skipped, not buffered. |
| LZMA      | `lzma`          | `LZMACompressorInputStream`         | Yes     | 1.6   | Dictionary declared in the input; see guarantee M2.                               |
| Pack200   | `pack200`       | `Pack200CompressorInputStream`      | **No**  |       | Legacy; removed from the JDK in Java 14.                                          |
| Snappy    | `snappy-raw`    | `SnappyCompressorInputStream`       | Yes     | 1.7   | Window fixed by the format.                                                       |
| Snappy    | `snappy-framed` | `FramedSnappyCompressorInputStream` | Yes     | 1.7   | Window and chunk size fixed by the format.                                        |
| XZ        | `xz`            | `XZCompressorInputStream`           | Yes     | 1.4   | Dictionary declared in the input; see guarantee M2.                               |
| Z (LZW)   | `z`             | `ZCompressorInputStream`            | Yes     | 1.7   | Code table bounded by the format's maximum code size (16 bits).                   |
| Zstandard | `zstd`          | `ZstdCompressorInputStream`         | Yes     | 1.16  | Window declared in the input, bounded by the decoder's window limit.              |

#### Automatic format detection

`ArchiveStreamFactory` and `CompressorStreamFactory` can choose a format from the leading bytes of the input.
Those bytes are chosen by whoever wrote the input,
so automatic detection can select a format that is not covered.

- Automatic detection is covered only when the detected format is covered.
  Until the factories are restricted to covered formats by default,
  a host that applies automatic detection to untrusted input
  must check the result of `detect()` against the tables above
  before creating the stream.
- When a format is requested by name,
  the caller has chosen it and takes responsibility for that choice.
  Requesting a format that is not covered means the input must be trusted.

#### Third-party decoders

Some formats are decoded by external libraries:
XZ and LZMA by XZ for Java,
Zstandard by zstd-jni,
Brotli by the Brotli decoder (`org.brotli:dec`).
The guarantees cover how Commons Compress uses these libraries:
the limits it passes to them and the sizes it checks before calling them.
Defects inside those libraries are findings against the upstream project
(`OUT-OF-MODEL: upstream` in the Commons model).

## Adversary model

This section refines "Adversary model" in the Commons model.

- **In scope:**
  whoever authors an archive or compressed stream
  that the host reads with a covered format and entry point.
  For covered formats, the trust boundary for the properties below
  moves into Commons Compress,
  in the sense of the Commons model's "secure parsing" guarantee.
- **Attacker goals considered:**
  making Compress retain memory out of proportion to the input or to the configured limit,
  making it run forever,
  making it execute code,
  or making it read or connect to anything the caller did not designate.
- **Out of scope**, in addition to the Commons model:
  - input in a format that is not covered;
  - the size of the decompressed output itself (see "Output");
  - the host's own memory use, such as holding every entry's content in byte arrays.

## Security properties

### Properties for all formats

For every format, covered or not, Commons Compress does not:

- execute code because of the content of an archive or compressed stream;
- read from, or connect to, anything the caller did not designate.
  Opening a split archive through the split-archive API
  designates its segments.

Path traversal ("zip-slip") remains the host's responsibility,
as stated in the Commons model (CWE-22).
`ArchiveEntry.resolveIn` is provided for that purpose.

### Resource guarantees for covered formats

For covered formats, Commons Compress takes on a precise form of the Commons model's
"secure parsing" guarantee.
The guarantees below replace that guarantee's
"excessive CPU consumption" and "excessive memory allocation" clauses.

#### What is measured

The harm guarded against is **retained memory**:
memory that stays reachable from a reader,
or from an entry it returned,
while the reader is open.
One small input that pins a large amount of heap,
multiplied across concurrent requests,
degrades a server that otherwise has plenty of resources.

How long memory is retained differs by reader:
`ZipFile` and `SevenZFile` keep their central directory or header until `close()`;
`TarArchiveInputStream` keeps a sparse map until the next entry.
Short-lived temporary allocations count only towards guarantee M1's linearity rule.

**`OutOfMemoryError` is not a vulnerability in itself**, under any heap configuration.
It is the expected outcome when the JVM does not have the resources to process a given input.
A proof of concept that ends in `OutOfMemoryError`
only shows how much memory was requested,
and that amount is assessed against the guarantees below.
Likewise, Commons Compress does not try to predict `OutOfMemoryError`
from the current heap state;
such checks cannot be atomic and are not a security boundary.

#### M1. Memory is allocated only for data that is present

- Commons Compress does not allocate memory based on a length or count declared in the input
  unless the corresponding bytes have been read,
  or, for random-access input (`ZipFile`, `SevenZFile`, `TarFile`),
  the declared region has been verified to lie within the input.
- Where a size is not known in advance, buffers grow as data arrives.
- A parameter whose maximum the format itself specifies
  (for example, the maximum code size of a Z stream)
  is validated against that maximum before it is used to allocate memory.
- Retained memory is at most linear in the number of bytes consumed
  from the stream the reader reads.
  When the host places a compressor in front of an archiver
  (for example, a TAR archive inside a GZIP stream),
  that stream is the decompressed one;
  the default metadata limits (see guarantee M2) bound
  each item of metadata however it arrives.

#### M2. Configured limits are respected

- Where a memory limit or metadata limit is configured,
  it bounds the input-driven allocations made while reading.
  This includes:
  - `maxMemoryLimitKiB` on `SevenZFile`;
  - `memoryLimitInKiB` on `LZMACompressorInputStream`, `XZCompressorInputStream`, `ZCompressorInputStream` and `CompressorStreamFactory`;
  - `maxEntryNameLength` on every archiver builder;
  - `maxPaxHeaderSize` on the TAR readers;
  - allocations made by decompressors that Commons Compress creates internally,
    for example the coders of a 7z folder.
- Exceeding a limit raises `MemoryLimitException` or another `IOException`,
  not `OutOfMemoryError`.
- Dictionary and window sizes declared in the input
  (LZMA, LZMA2, XZ, Brotli, Zstandard)
  are governed by this guarantee, not by M1:
  they are bounded by the configured limit,
  or, where no limit is configured, by the format's maximum.
  Leaving a limit unset is the host's choice
  (see "Downstream responsibilities").
- The mechanisms that let the host bound the decompressed output
  (`InputStreamStatistics` and the memory limits)
  are part of this guarantee:
  a defect that makes them ineffective is a violation.

#### T. Progress

- For any finite input, every read call returns data,
  returns end of input,
  or throws an `IOException`,
  after consuming a bounded amount of input.
  No loop runs forever.
- No amount of work is multiplied by a factor taken from the input without a cap.
  Examples are iteration counts,
  declared repetitions,
  key-derivation rounds,
  and numeric exponents
  (as in CVE-2023-42503).
- Processing time that is super-linear in the size of the input
  (for example, quadratic)
  is a performance bug, not a vulnerability:
  the host controls it by limiting the size of the input it accepts.

#### Accuracy

Memory limits and size checks are estimates.
They account for the dominant allocations driven by archive metadata,
using estimated per-object costs,
and do not include I/O and decompressor buffers,
JVM object overhead,
or short-lived temporary objects.
A configured limit is therefore a reasonable bound, not an exact one:
actual retained memory can exceed it by a constant factor
and by a fixed amount for buffers and overhead.
Neither the factor nor the amount is specified.
What matters is that the excess does not grow with the input.

## Security properties not provided

In addition to the Commons model:

- **No limit on decompressed output.**
  Decompression can legitimately produce far more output than input.
  Commons Compress streams the output to the host
  and provides the means to stop
  (`InputStreamStatistics`, memory limits);
  using them is the host's job.
- **No resource guarantees for formats that are not covered.**
  Malicious-input findings against them are
  `OUT-OF-MODEL: trusted-input`.
- **No linear-time guarantee.**
  See guarantee T.
- **No protection against amplification by a compressor the host chained in front of an archiver**,
  beyond the default metadata limits.
- **No protection against an undersized JVM.**
  The guarantees assume a heap comfortably larger than the configured limits.

## Downstream responsibilities

In addition to the Commons model, a host that reads untrusted archives or compressed streams must:

1. **Use only covered formats and entry points**,
   and check the result of automatic detection against the tables above.
2. **Configure memory limits**
   for formats whose memory depends on a size declared in the input
   (7z, LZMA, XZ, Z),
   at a value the host's heap can accommodate.
3. **Bound the decompressed output**,
   using `InputStreamStatistics` or by counting the bytes read.
4. **Limit the size of the input it accepts and the time it allows.**
5. **Sanitize entry paths** before writing extracted files,
   preferably with `ArchiveEntry.resolveIn`.

## What is and is not a vulnerability

For covered formats, the following are vulnerabilities (`VALID`):

1. retained memory whose size is determined by a declared length, count or format parameter
   that is not backed by input data,
   or that exceeds the maximum specified by the format;
2. retained memory that grows faster than linearly in the input consumed;
3. an input-driven allocation path that does not apply the configured limit or the default metadata limit;
4. retained memory that exceeds the configured limit
   by an amount that grows with the input;
5. a defect that makes `InputStreamStatistics` or a memory limit ineffective;
6. a loop that never ends,
   or an amount of work multiplied by an uncapped factor taken from the input.

The following are not vulnerabilities.
They may be reported in public as ordinary bugs or hardening requests:

- retained memory that is linear in the input
  but has a larger constant factor than expected;
- retained memory that exceeds the configured limit
  by an amount that does not grow with the input
  (see "Accuracy");
- `OutOfMemoryError`, under any heap configuration,
  except as a measurement under the rules above;
- memory bounded only by a format's maximum dictionary or window size
  when the host configured no limit;
- processing time that is super-linear in the size of the input;
- any resource consumption in a format that is not covered.

### Report requirements

A report about resource use must include a reproducer
that measures retained memory
(for example, the size reachable from the reader after a full garbage collection,
measured with a heap dump or a tool such as JOL)
and compares it with the size of the input and the configured limit.
A report that only shows `OutOfMemoryError` under a small `-Xmx`
does not meet this requirement.

## Known non-findings

These can be given to scanners and automated triage as a suppression list,
in addition to those of the Commons model.
Where the PMC has published its analysis of a past report,
the entry links to it.
Such analyses are posted to the
[user mailing list](https://lists.apache.org/list.html?user@commons.apache.org)
with a `[NOT-VULNERABILITY]` or `[HARDENING]` subject prefix.

- **"A small archive decompresses to gigabytes (zip bomb)."**
  Output size is the host's to bound; see "Downstream responsibilities".
- **"`OutOfMemoryError` with `-Xmx64m` on a crafted file."**
  Assessed only as a measurement; see "What is measured".
- [**"Allocation of an LZMA dictionary declared in the header, with no memory limit configured."**](https://lists.apache.org/thread/ntwnmhnhw5trcqly4nknlqrqm948qm50)
  Governed by guarantee M2; configuring the limit is the host's job.
- **"Each entry, sparse fragment or extra field costs N times its size in the input."**
  Linear growth; a hardening request, not a vulnerability.
- **"Quadratic processing time on a large input."**
  A performance bug; see guarantee T.
- **"Resource exhaustion in ARJ, DUMP, LHA or Pack200."**
  Format not covered.
- **"Automatic detection selected ARJ, DUMP, LHA or Pack200."**
  Format not covered; see "Automatic format detection".

## Triage dispositions

The dispositions of the Commons model apply, with these refinements:

| Finding                                                                                        | Disposition                                                         |
|------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| Violates a guarantee above, in a covered format, method or entry point                         | `VALID`, in every release from its "Since" version that is affected |
| Linear growth with a large constant, or a bounded excess over the configured limit             | `VALID-HARDENING` or a public bug                                   |
| Any malicious-input finding in a format that is not covered                                    | `OUT-OF-MODEL: trusted-input`                                       |
| Allocation inside XZ for Java, zstd-jni or the Brotli decoder, with the limit passed correctly | `OUT-OF-MODEL: upstream`                                            |
| Unlimited dictionary or window with no limit configured                                        | `KNOWN-NON-FINDING`                                                 |

## Conditions that would change this model

- **A format becomes covered** when a committer volunteers to maintain these guarantees for it
  and the PMC agrees.
  The format is then added to the tables
  with the first release that contained it as its "Since" version,
  unless the PMC records a later one.
- **A format stops being covered**
  only in a new minor or major release,
  announced in the release notes.
- **A new format or compression method is added** to Commons Compress:
  it is not covered until the tables say so.
- **The factory defaults change**
  so that automatic detection selects only covered formats:
  "Automatic format detection" is updated.
- **A security-relevant default changes**
  (for example, a default memory limit for 7z, LZMA or XZ):
  the affected guarantees are re-ruled.
- **A report cannot be routed** to a single disposition above:
  it is a `MODEL-GAP` and triggers a revision of this document.

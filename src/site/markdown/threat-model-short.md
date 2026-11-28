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
# Apache Commons Compress Threat Model (summary, draft)

This is a work in progress.

## Introduction

This page is the short version of the
[Apache Commons Compress Threat Model](threat-model.html),
which is authoritative where the two differ.
Both amend the
[Apache Commons Threat Model 0.1.0](https://commons.apache.org/threat-model-0.1.0.html)
(summary: [threat-model-short-0.1.0](https://commons.apache.org/threat-model-short-0.1.0.html)),
which applies to Compress unless stated otherwise.

## The model in brief

Commons Compress reads archives and compressed streams that the host application hands it.
As in all of Commons, the caller is trusted and its input is trusted by default.
The exception is a list of **covered formats**,
which may be used on untrusted input:
for them, Commons Compress bounds the memory it retains
by the input it has actually read
and by the limits the host configures.

**Formats that are not listed as covered are not covered.**
This includes formats added to Commons Compress later
and formats from third-party providers.

## Covered formats

| Kind        | Covered                                                                                   | Not covered (trusted input only) |
|-------------|-------------------------------------------------------------------------------------------|----------------------------------|
| Archivers   | AR, CPIO, 7z, TAR, ZIP (including JAR and APK)                                            | ARJ, DUMP, LHA                   |
| Compressors | Brotli, BZIP2, Deflate, Deflate64, GZIP, LZ4, LZMA, Snappy, XZ, Z, Zstandard               | Pack200                          |

Coverage applies to every release that contains the format,
from the release in which it first appeared,
so vulnerabilities in past releases are treated as such.
A covered archiver covers every compression method it can decode.
Automatic format detection is covered only when the detected format is covered:
check the result of `detect()` before creating a stream from untrusted input.
The full model lists the entry points, compression methods and the release in which each one first appeared.

## What Commons Compress guarantees

For **every format**:

- no code execution because of the content of the input;
- no reads from, or connections to, anything the caller did not designate.

For **covered formats**, while reading:

- **M1. Memory only for data that is present.**
  No allocation is based on a declared length or count until the bytes are read,
  or verified to lie within random-access input;
  sizes the format bounds are validated against the format's maximum;
  retained memory is at most linear in the bytes consumed.
- **M2. Configured limits are respected.**
  Memory and metadata limits bound every input-driven allocation,
  including those of internal decompressors,
  and fail with an `IOException`, not `OutOfMemoryError`.
  Dictionary and window sizes declared in the input
  are bounded by the configured limit,
  or by the format's maximum when no limit is set.
  `InputStreamStatistics` and the memory limits work as documented.
- **T. Progress.**
  Every read call returns or throws an `IOException`;
  no loop runs forever,
  and no work is multiplied by an uncapped factor taken from the input.

Retained memory means memory reachable from a reader, or from an entry it returned, while the reader is open.
Limits are reasonable bounds, not exact ones:
retained memory may exceed a configured limit by an unspecified constant factor,
but not by an amount that grows with the input.

## What Commons Compress does not guarantee

- **`OutOfMemoryError` is not a vulnerability in itself**, under any heap configuration.
  A proof of concept that triggers it only measures how much memory was requested.
- **No limit on decompressed output** (zip bombs):
  bound it with `InputStreamStatistics` or by counting bytes.
- **No linear time:** super-linear processing time is a performance bug.
- **No resource guarantees for formats that are not covered.**
- **No path-traversal protection:** sanitize entry paths, preferably with `ArchiveEntry.resolveIn`.
- **No protection from defects inside XZ for Java, zstd-jni or the Brotli decoder;**
  those go upstream.

## What the host must do with untrusted input

1. Use only covered formats, and check automatic detection results.
2. Configure memory limits for 7z, LZMA, XZ and Z.
3. Bound the decompressed output.
4. Limit the size of the input and the time allowed.
5. Sanitize entry paths before writing extracted files.

## Reporting

A report is valid if, in a covered format,
a value in the input makes Commons Compress break M1, M2 or T.
Resource reports must include a reproducer that measures retained memory
against the input size and the configured limit;
`OutOfMemoryError` under a small `-Xmx` alone is not enough.
Report undisclosed vulnerabilities privately,
as described on the
[Apache Commons Security page](https://commons.apache.org/security.html).
The triage dispositions and known non-findings are in the [full threat model](threat-model.html).

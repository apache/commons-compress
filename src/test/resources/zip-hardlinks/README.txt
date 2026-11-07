PKWARE UNIX hard-link test vectors from google/mount-zip.
Pinned upstream commit: 4b82fc1db86857ce2e3e7ced7eb924b13675faad

https://raw.githubusercontent.com/google/mount-zip/4b82fc1db86857ce2e3e7ced7eb924b13675faad/tests/data/hlink-chain.zip
SHA-256: 67bfa039f62150c3e4766e178d9c2cc2ddf8c631c84a87a749e5242b79bd36a1

https://raw.githubusercontent.com/google/mount-zip/4b82fc1db86857ce2e3e7ced7eb924b13675faad/tests/data/hlink-before-target.zip
SHA-256: 7e4a23392e93580fafff58c809d528f45f6ceab26668d81e9c09bbf1e9aac245

https://raw.githubusercontent.com/google/mount-zip/4b82fc1db86857ce2e3e7ced7eb924b13675faad/tests/data/pkware-specials.zip
SHA-256: efb27035cfcd47063e6a98446634518f0cd1b2d5255ff7e3a7232f33a5a05150

All three original archives have their 0x000d fields only in central headers.
hlink-chain.zip: 0regular (10 bytes), hlink1 -> 0regular, hlink2 -> hlink1.
hlink-before-target.zip: 0hlink -> 1regular precedes its 10-byte target.
pkware-specials.zip: UNIX modes but creator platform FAT (0); tests isolate
regular (32 bytes), z-hardlink1 -> regular and z-hardlink2 -> regular by raw
copying. Devices and symlinks are not used to judge regular hard-link behavior.

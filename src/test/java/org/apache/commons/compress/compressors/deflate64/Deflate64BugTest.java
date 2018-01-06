package org.apache.commons.compress.compressors.deflate64;

import static org.apache.commons.compress.AbstractTestCase.getFile;

import java.io.InputStream;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.Test;

public class Deflate64BugTest {
  @Test
  public void readBeyondMemoryException() throws Exception {
    try (ZipFile zfile = new ZipFile(getFile("COMPRESS-380-deflatebug.zip"))) { 
      Enumeration<ZipArchiveEntry> entries = zfile.getEntries();
      while (entries.hasMoreElements()) {
        ZipArchiveEntry e = entries.nextElement();

        byte [] buf = new byte [1024 * 8];
        try (InputStream is = zfile.getInputStream(e)) {
          while (true) {
            int read = is.read(buf);
            if (read == -1) {
              break;
            }
          }
        }
      }
    }
  }
}

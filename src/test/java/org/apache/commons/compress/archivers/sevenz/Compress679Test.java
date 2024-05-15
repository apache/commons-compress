package org.apache.commons.compress.archivers.sevenz;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class Compress679Test {

    @Test
    @Disabled("Temp")
    public static void testCompress679() {
        final Path origin = Paths.get("file.7z");
        assertTrue(Files.exists(origin));
        final List<Exception> list = new CopyOnWriteArrayList<>();
        final Runnable runnable = () -> {
            try {
                try (SevenZFile sevenZFile = SevenZFile.builder().setPath(origin).get()) {
                    SevenZArchiveEntry sevenZArchiveEntry;
                    while ((sevenZArchiveEntry = sevenZFile.getNextEntry()) != null) {
                        if ("file4.txt".equals(sevenZArchiveEntry.getName())) { // The entry must not be the first of the ZIP archive to reproduce
                            final InputStream inputStream = sevenZFile.getInputStream(sevenZArchiveEntry);
                            // treatments...
                            break;
                        }
                    }
                }
            } catch (final Exception e) {
                // java.io.IOException: Checksum verification failed
                e.printStackTrace();
                list.add(e);
            }
        };
        IntStream.range(0, 30).forEach(i -> new Thread(runnable).start());
        assertTrue(list.isEmpty());
    }
}

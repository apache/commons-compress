import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Decompresses each .bz2 file given on the command line and prints the sum of its decompressed bytes (unsigned).
 */
public final class BZip2SumTest {

    public static void main(final String[] args) throws IOException {
        final byte[] buffer = new byte[1 << 16];
        long grandTotal = 0;
        for (final String file : args) {
            long sum = 0;
            long count = 0;
            final long start = System.nanoTime();
            // Files.newInputStream is unbuffered; the decoder refills a few bytes at a time, so buffer the source.
            try (InputStream in = new BZip2CompressorInputStream(new BufferedInputStream(Files.newInputStream(Paths.get(file)), 1 << 16))) {
                int n;
                while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                    for (int i = 0; i < n; i++) {
                        sum += buffer[i] & 0xff;
                    }
                    count += n;
                }
            }
            final double seconds = (System.nanoTime() - start) / 1e9;
            System.out.printf("%s: %,d bytes, sum = %,d (%.2f s, %.1f MB/s)%n", file, count, sum, seconds, count / seconds / 1e6);
            grandTotal += sum;
        }
        if (args.length > 1) {
            System.out.printf("total sum = %,d%n", grandTotal);
        }
    }

    private BZip2SumTest() {
    }
}

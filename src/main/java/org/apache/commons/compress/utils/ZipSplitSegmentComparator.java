package org.apache.commons.compress.utils;

import org.apache.commons.compress.compressors.FileNameUtil;

import java.io.File;
import java.util.Comparator;

public class ZipSplitSegmentComparator implements Comparator<File> {
    @Override
    public int compare(File file1, File file2) {
        String extension1 = FileNameUtil.getExtension(file1.getPath());
        String extension2 = FileNameUtil.getExtension(file2.getPath());

        if(!extension1.startsWith("z")) {
            return -1;
        }

        if(!extension2.startsWith("z")) {
            return 1;
        }

        Integer splitSegmentNumber1 = Integer.parseInt(extension1.substring(1));
        Integer splitSegmentNumber2 = Integer.parseInt(extension2.substring(1));

        return splitSegmentNumber1.compareTo(splitSegmentNumber2);
    }
}

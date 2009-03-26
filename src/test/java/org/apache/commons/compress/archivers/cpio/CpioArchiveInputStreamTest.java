package org.apache.commons.compress.archivers.cpio;

import java.io.FileInputStream;

import org.apache.commons.compress.AbstractTestCase;

public class CpioArchiveInputStreamTest extends AbstractTestCase {

    public void testCpioUnarchive() throws Exception {
        StringBuffer expected = new StringBuffer();
        expected.append("./test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>./test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");
        

        CpioArchiveInputStream in = 
                new CpioArchiveInputStream(new FileInputStream(getFile("bla.cpio")));
        CpioArchiveEntry entry= null;
        
        StringBuffer result = new StringBuffer();
        while ((entry = (CpioArchiveEntry) in.getNextEntry()) != null) {
            result.append(entry.getName());
            int tmp;
            while ((tmp = in.read()) != -1) {
                result.append((char) tmp);
             }
         }
         in.close();
         assertEquals(result.toString(), expected.toString());
    }    
}

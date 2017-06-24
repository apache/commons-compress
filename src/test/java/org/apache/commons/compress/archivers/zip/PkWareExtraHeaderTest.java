package org.apache.commons.compress.archivers.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.compress.archivers.zip.PKWareExtraHeader.EncryptionAlgorithm;
import org.apache.commons.compress.archivers.zip.PKWareExtraHeader.HashAlgorithm;
import org.junit.Test;

public class PkWareExtraHeaderTest {

    @Test public void testEncryptionAlgorithm() {
        String name = "AES256";
        int code = EncryptionAlgorithm.AES256.getCode();
        EncryptionAlgorithm e = EncryptionAlgorithm.valueOf(name);
        assertEquals(code,e.getCode());
        assertNotNull(e);
    }

    @Test public void testHashAlgorithm() {
        String name = "SHA256";
        int code = HashAlgorithm.SHA256.getCode();
        HashAlgorithm e = HashAlgorithm.valueOf(name);
        assertEquals(code,e.getCode());
        assertNotNull(e);
    }

}

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.pack200;

import java.util.List;

/**
 * Constant pool entry for a signature.
 */
public class CPSignature extends ConstantPoolEntry implements Comparable {

    private final CPUTF8 signatureForm;
    private final List<CPClass> classes;
    private final String signature;
    private final boolean formStartsWithBracket;

    public CPSignature(final String signature, final CPUTF8 signatureForm, final List<CPClass> classes) {
        this.signature = signature;
        this.signatureForm = signatureForm;
        this.classes = classes;
        formStartsWithBracket = signatureForm.toString().startsWith("(");
    }

    @Override
    public int compareTo(final Object arg0) {
        if (signature.equals(((CPSignature) arg0).signature)) {
            return 0;
        }
        if (formStartsWithBracket && !((CPSignature) arg0).formStartsWithBracket) {
            return 1;
        }
        if (((CPSignature) arg0).formStartsWithBracket && !formStartsWithBracket) {
            return -1;
        }
        if (classes.size() - ((CPSignature) arg0).classes.size() != 0) {
            return classes.size() - ((CPSignature) arg0).classes.size();
        }
        if (classes.size() > 0) {
            for (int i = classes.size() - 1; i >= 0; i--) {
                final CPClass cpClass = classes.get(i);
                final CPClass compareClass = ((CPSignature) arg0).classes.get(i);
                final int classComp = cpClass.compareTo(compareClass);
                if (classComp != 0) {
                    return classComp;
                }
            }
        }
        return signature.compareTo(((CPSignature) arg0).signature);
    }

    public List<CPClass> getClasses() {
        return classes;
    }

    public int getIndexInCpUtf8() {
        return signatureForm.getIndex();
    }

    public CPUTF8 getSignatureForm() {
        return signatureForm;
    }

    public String getUnderlyingString() {
        return signature;
    }

    @Override
    public String toString() {
        return signature;
    }
}

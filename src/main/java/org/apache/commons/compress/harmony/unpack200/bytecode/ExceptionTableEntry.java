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
package org.apache.commons.compress.harmony.unpack200.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * An entry in an exception table.
 */
public class ExceptionTableEntry {

    private final int startPC;
    private final int endPC;
    private final int handlerPC;
    private final CPClass catchType;

    private int startPcRenumbered;
    private int endPcRenumbered;
    private int handlerPcRenumbered;
    private int catchTypeIndex;

    /**
     * Create a new ExceptionTableEntry. Exception tables are of two kinds: either a normal one (with a Throwable as the
     * catchType) or a finally clause (which has no catchType). In the class file, the finally clause is represented
     * as catchType == 0.
     *
     * To create a finally clause with this method, pass in null for the catchType.
     *
     * @param startPC int
     * @param endPC int
     * @param handlerPC int
     * @param catchType CPClass (if it's a normal catch) or null (if it's a finally clause).
     */
    public ExceptionTableEntry(final int startPC, final int endPC, final int handlerPC, final CPClass catchType) {
        this.startPC = startPC;
        this.endPC = endPC;
        this.handlerPC = handlerPC;
        this.catchType = catchType;
    }

    public CPClass getCatchType() {
        return catchType;
    }

    public void renumber(final List<Integer> byteCodeOffsets) {
        startPcRenumbered = byteCodeOffsets.get(startPC).intValue();
        final int endPcIndex = startPC + endPC;
        endPcRenumbered = byteCodeOffsets.get(endPcIndex).intValue();
        final int handlerPcIndex = endPcIndex + handlerPC;
        handlerPcRenumbered = byteCodeOffsets.get(handlerPcIndex).intValue();
    }

    public void resolve(final ClassConstantPool pool) {
        if (catchType == null) {
            // If the catch type is a finally clause
            // the index is always 0.
            catchTypeIndex = 0;
            return;
        }
        catchType.resolve(pool);
        catchTypeIndex = pool.indexOf(catchType);
    }

    public void write(final DataOutputStream dos) throws IOException {
        dos.writeShort(startPcRenumbered);
        dos.writeShort(endPcRenumbered);
        dos.writeShort(handlerPcRenumbered);
        dos.writeShort(catchTypeIndex);
    }
}

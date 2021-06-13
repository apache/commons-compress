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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Attribute;

/**
 * Utility class to manage the various options available for pack200
 */
public class PackingOptions {

    public static final String STRIP = "strip";
    public static final String ERROR = "error";
    public static final String PASS = "pass";
    public static final String KEEP = "keep";

    // All options are initially set to their defaults
    private boolean gzip = true;
    private boolean stripDebug = false;
    private boolean keepFileOrder = true;
    private long segmentLimit = 1000000L;
    private int effort = 5;
    private String deflateHint = KEEP;
    private String modificationTime = KEEP;
    private List passFiles;
    private String unknownAttributeAction = PASS;
    private Map classAttributeActions;
    private Map fieldAttributeActions;
    private Map methodAttributeActions;
    private Map codeAttributeActions;
    private boolean verbose = false;
    private String logFile;

    private Attribute[] unknownAttributeTypes;

    public boolean isGzip() {
        return gzip;
    }

    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }

    public boolean isStripDebug() {
        return stripDebug;
    }

    /**
     * Set strip debug attributes. If true, all debug attributes (i.e.
     * LineNumberTable, SourceFile, LocalVariableTable and
     * LocalVariableTypeTable attributes) are stripped when reading the input
     * class files and not included in the output archive.
     *
     * @param stripDebug If true, all debug attributes.
     */
    public void setStripDebug(boolean stripDebug) {
        this.stripDebug = stripDebug;
    }

    public boolean isKeepFileOrder() {
        return keepFileOrder;
    }

    public void setKeepFileOrder(boolean keepFileOrder) {
        this.keepFileOrder = keepFileOrder;
    }

    public long getSegmentLimit() {
        return segmentLimit;
    }

    /**
     * Set the segment limit (equivalent to -S command line option)
     * @param segmentLimit - the limit in bytes
     */
    public void setSegmentLimit(long segmentLimit) {
        this.segmentLimit = segmentLimit;
    }

    public int getEffort() {
        return effort;
    }

    /**
     * Sets the compression effort level (0-9, equivalent to -E command line option)
     * @param effort the compression effort level, 0-9.
     */
    public void setEffort(int effort) {
        this.effort = effort;
    }

    public String getDeflateHint() {
        return deflateHint;
    }

    public boolean isKeepDeflateHint() {
        return KEEP.equals(deflateHint);
    }

    public void setDeflateHint(String deflateHint) {
        if (!KEEP.equals(deflateHint)
                && !"true".equals(deflateHint)
                && !"false".equals(deflateHint)) {
            throw new IllegalArgumentException(
                    "Bad argument: -H "
                            + deflateHint
                            + " ? deflate hint should be either true, false or keep (default)");
        }
        this.deflateHint = deflateHint;
    }

    public String getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(String modificationTime) {
        if (!KEEP.equals(modificationTime)
                && !"latest".equals(modificationTime)) {
            throw new IllegalArgumentException(
                    "Bad argument: -m "
                            + modificationTime
                            + " ? transmit modtimes should be either latest or keep (default)");
        }
        this.modificationTime = modificationTime;
    }

    public boolean isPassFile(String passFileName) {
        if (passFiles != null) {
            for (Iterator iterator = passFiles.iterator(); iterator.hasNext();) {
                String pass = (String) iterator.next();
                if (passFileName.equals(pass)) {
                    return true;
                } else if (!pass.endsWith(".class")) { // a whole directory is
                    // passed
                    if (!pass.endsWith("/")) {
                        // Make sure we don't get any false positives (e.g.
                        // exclude "org/apache/harmony/pack" should not match
                        // files under "org/apache/harmony/pack200/")
                        pass = pass + "/";
                    }
                    return passFileName.startsWith(pass);
                }
            }
        }
        return false;
    }

    /**
     * Tell the compressor to pass the file with the given name, or if the name
     * is a directory name all files under that directory will be passed.
     *
     * @param passFileName
     *            the file name
     */
    public void addPassFile(String passFileName) {
        if(passFiles == null) {
            passFiles = new ArrayList();
        }
        String fileSeparator = System.getProperty("file.separator");
        if(fileSeparator.equals("\\")) {
            // Need to escape backslashes for replaceAll(), which uses regex
            fileSeparator += "\\";
        }
        passFileName = passFileName.replaceAll(fileSeparator, "/");
        passFiles.add(passFileName);
    }

    public void removePassFile(String passFileName) {
        passFiles.remove(passFileName);
    }

    public String getUnknownAttributeAction() {
        return unknownAttributeAction;
    }

    /**
     * Tell the compressor what to do if an unknown attribute is encountered
     * @param unknownAttributeAction - the action to perform
     */
    public void setUnknownAttributeAction(String unknownAttributeAction) {
        this.unknownAttributeAction = unknownAttributeAction;
        if (!PASS.equals(unknownAttributeAction)
                && !ERROR.equals(unknownAttributeAction)
                && !STRIP.equals(unknownAttributeAction)) {
            throw new RuntimeException("Incorrect option for -U, "
                    + unknownAttributeAction);
        }
    }

    public void addClassAttributeAction(String attributeName, String action) {
        if(classAttributeActions == null) {
            classAttributeActions = new HashMap();
        }
        classAttributeActions.put(attributeName, action);
    }

    public void addFieldAttributeAction(String attributeName, String action) {
        if(fieldAttributeActions == null) {
            fieldAttributeActions = new HashMap();
        }
        fieldAttributeActions.put(attributeName, action);
    }

    public void addMethodAttributeAction(String attributeName, String action) {
        if(methodAttributeActions == null) {
            methodAttributeActions = new HashMap();
        }
        methodAttributeActions.put(attributeName, action);
    }

    public void addCodeAttributeAction(String attributeName, String action) {
        if(codeAttributeActions == null) {
            codeAttributeActions = new HashMap();
        }
        codeAttributeActions.put(attributeName, action);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setQuiet(boolean quiet) {
        this.verbose = !quiet;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    private void addOrUpdateAttributeActions(List prototypes, Map attributeActions,
            int tag) {
        if (attributeActions != null) {
            if (attributeActions.size() > 0) {
                String name, action;
                boolean prototypeExists;
                NewAttribute newAttribute;
                for (Iterator iteratorI = attributeActions.keySet().iterator(); iteratorI
                        .hasNext();) {
                    name = (String) iteratorI.next();
                    action = (String) attributeActions.get(name);
                    prototypeExists = false;
                    for (Iterator iteratorJ = prototypes.iterator(); iteratorJ
                            .hasNext();) {
                        newAttribute = (NewAttribute) iteratorJ.next();
                        if (newAttribute.type.equals(name)) {
                            // if the attribute exists, update its context
                            newAttribute.addContext(tag);
                            prototypeExists = true;
                            break;
                        }
                    }
                    // if no attribute is found, add a new attribute
                    if (!prototypeExists) {
                        if (ERROR.equals(action)) {
                            newAttribute = new NewAttribute.ErrorAttribute(name, tag);
                        } else if (STRIP.equals(action)) {
                            newAttribute = new NewAttribute.StripAttribute(name, tag);
                        } else if (PASS.equals(action)) {
                            newAttribute = new NewAttribute.PassAttribute(name, tag);
                        } else {
                            newAttribute = new NewAttribute(name, action, tag);
                        }
                        prototypes.add(newAttribute);
                    }
                }
            }
        }
    }

    public Attribute[] getUnknownAttributePrototypes() {
        if (unknownAttributeTypes == null) {
            List prototypes = new ArrayList();
            addOrUpdateAttributeActions(prototypes, classAttributeActions,
                    AttributeDefinitionBands.CONTEXT_CLASS);

            addOrUpdateAttributeActions(prototypes, methodAttributeActions,
                    AttributeDefinitionBands.CONTEXT_METHOD);

            addOrUpdateAttributeActions(prototypes, fieldAttributeActions,
                    AttributeDefinitionBands.CONTEXT_FIELD);

            addOrUpdateAttributeActions(prototypes, codeAttributeActions,
                    AttributeDefinitionBands.CONTEXT_CODE);

            unknownAttributeTypes = (Attribute[]) prototypes
                    .toArray(new Attribute[0]);
        }
        return unknownAttributeTypes;
    }

    public String getUnknownClassAttributeAction(String type) {
        if (classAttributeActions == null) {
            return unknownAttributeAction;
        }
        String action = (String) classAttributeActions.get(type);
        if(action == null) {
            action = unknownAttributeAction;
        }
        return action;
    }

    public String getUnknownMethodAttributeAction(String type) {
        if (methodAttributeActions == null) {
            return unknownAttributeAction;
        }
        String action = (String) methodAttributeActions.get(type);
        if(action == null) {
            action = unknownAttributeAction;
        }
        return action;
    }

    public String getUnknownFieldAttributeAction(String type) {
        if (fieldAttributeActions == null) {
            return unknownAttributeAction;
        }
        String action = (String) fieldAttributeActions.get(type);
        if(action == null) {
            action = unknownAttributeAction;
        }
        return action;
    }

    public String getUnknownCodeAttributeAction(String type) {
        if (codeAttributeActions == null) {
            return unknownAttributeAction;
        }
        String action = (String) codeAttributeActions.get(type);
        if(action == null) {
            action = unknownAttributeAction;
        }
        return action;
    }

}

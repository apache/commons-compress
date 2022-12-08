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
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Attribute;

/**
 * Utility class to manage the various options available for pack200.
 */
public class PackingOptions {

    private static final Attribute[] EMPTY_ATTRIBUTE_ARRAY = {};
    public static final long SEGMENT_LIMIT = 1_000_000L;
    public static final String STRIP = "strip";
    public static final String ERROR = "error";
    public static final String PASS = "pass";
    public static final String KEEP = "keep";

    // All options are initially set to their defaults
    private boolean gzip = true;
    private boolean stripDebug;
    private boolean keepFileOrder = true;
    private long segmentLimit = SEGMENT_LIMIT;
    private int effort = 5;
    private String deflateHint = KEEP;
    private String modificationTime = KEEP;
    private final List<String> passFiles = new ArrayList<>();
    private String unknownAttributeAction = PASS;
    private final Map<String, String> classAttributeActions = new HashMap<>();
    private final Map<String, String> fieldAttributeActions = new HashMap<>();
    private final Map<String, String> methodAttributeActions = new HashMap<>();
    private final Map<String, String> codeAttributeActions = new HashMap<>();
    private boolean verbose;
    private String logFile;

    private Attribute[] unknownAttributeTypes;

    public void addClassAttributeAction(final String attributeName, final String action) {
        classAttributeActions.put(attributeName, action);
    }

    public void addCodeAttributeAction(final String attributeName, final String action) {
        codeAttributeActions.put(attributeName, action);
    }

    public void addFieldAttributeAction(final String attributeName, final String action) {
        fieldAttributeActions.put(attributeName, action);
    }

    public void addMethodAttributeAction(final String attributeName, final String action) {
        methodAttributeActions.put(attributeName, action);
    }

    private void addOrUpdateAttributeActions(final List<Attribute> prototypes, final Map<String, String> attributeActions, final int tag) {
        if (attributeActions != null && attributeActions.size() > 0) {
            NewAttribute newAttribute;
            for (final String name : attributeActions.keySet()) {
                final String action = attributeActions.get(name);
                boolean prototypeExists = false;
                for (final Object prototype : prototypes) {
                    newAttribute = (NewAttribute) prototype;
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

    /**
     * Tell the compressor to pass the file with the given name, or if the name is a directory name all files under that
     * directory will be passed.
     *
     * @param passFileName the file name
     */
    public void addPassFile(String passFileName) {
        String fileSeparator = System.getProperty("file.separator");
        if (fileSeparator.equals("\\")) {
            // Need to escape backslashes for replaceAll(), which uses regex
            fileSeparator += "\\";
        }
        passFileName = passFileName.replaceAll(fileSeparator, "/");
        passFiles.add(passFileName);
    }

    public String getDeflateHint() {
        return deflateHint;
    }

    public int getEffort() {
        return effort;
    }

    public String getLogFile() {
        return logFile;
    }

    public String getModificationTime() {
        return modificationTime;
    }

    private String getOrDefault(final Map<String, String> map, final String type, final String defaultValue) {
        return map == null ? defaultValue : map.getOrDefault(type, defaultValue);
    }

    public long getSegmentLimit() {
        return segmentLimit;
    }

    public String getUnknownAttributeAction() {
        return unknownAttributeAction;
    }

    public Attribute[] getUnknownAttributePrototypes() {
        if (unknownAttributeTypes == null) {
            final List<Attribute> prototypes = new ArrayList<>();
            addOrUpdateAttributeActions(prototypes, classAttributeActions, AttributeDefinitionBands.CONTEXT_CLASS);
            addOrUpdateAttributeActions(prototypes, methodAttributeActions, AttributeDefinitionBands.CONTEXT_METHOD);
            addOrUpdateAttributeActions(prototypes, fieldAttributeActions, AttributeDefinitionBands.CONTEXT_FIELD);
            addOrUpdateAttributeActions(prototypes, codeAttributeActions, AttributeDefinitionBands.CONTEXT_CODE);
            unknownAttributeTypes = prototypes.toArray(EMPTY_ATTRIBUTE_ARRAY);
        }
        return unknownAttributeTypes;
    }

    public String getUnknownClassAttributeAction(final String type) {
        return getOrDefault(classAttributeActions, type, unknownAttributeAction);
    }

    public String getUnknownCodeAttributeAction(final String type) {
        return getOrDefault(codeAttributeActions, type, unknownAttributeAction);
    }

    public String getUnknownFieldAttributeAction(final String type) {
        return getOrDefault(fieldAttributeActions, type, unknownAttributeAction);
    }

    public String getUnknownMethodAttributeAction(final String type) {
        return getOrDefault(methodAttributeActions, type, unknownAttributeAction);
    }

    public boolean isGzip() {
        return gzip;
    }

    public boolean isKeepDeflateHint() {
        return KEEP.equals(deflateHint);
    }

    public boolean isKeepFileOrder() {
        return keepFileOrder;
    }

    public boolean isPassFile(final String passFileName) {
        for (String pass : passFiles) {
            if (passFileName.equals(pass)) {
                return true;
            }
            if (!pass.endsWith(".class")) { // a whole directory is
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
        return false;
    }

    public boolean isStripDebug() {
        return stripDebug;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void removePassFile(final String passFileName) {
        passFiles.remove(passFileName);
    }

    public void setDeflateHint(final String deflateHint) {
        if (!KEEP.equals(deflateHint) && !"true".equals(deflateHint) && !"false".equals(deflateHint)) {
            throw new IllegalArgumentException("Bad argument: -H " + deflateHint + " ? deflate hint should be either true, false or keep (default)");
        }
        this.deflateHint = deflateHint;
    }

    /**
     * Sets the compression effort level (0-9, equivalent to -E command line option)
     *
     * @param effort the compression effort level, 0-9.
     */
    public void setEffort(final int effort) {
        this.effort = effort;
    }

    public void setGzip(final boolean gzip) {
        this.gzip = gzip;
    }

    public void setKeepFileOrder(final boolean keepFileOrder) {
        this.keepFileOrder = keepFileOrder;
    }

    public void setLogFile(final String logFile) {
        this.logFile = logFile;
    }

    public void setModificationTime(final String modificationTime) {
        if (!KEEP.equals(modificationTime) && !"latest".equals(modificationTime)) {
            throw new IllegalArgumentException("Bad argument: -m " + modificationTime + " ? transmit modtimes should be either latest or keep (default)");
        }
        this.modificationTime = modificationTime;
    }

    public void setQuiet(final boolean quiet) {
        this.verbose = !quiet;
    }

    /**
     * Sets the segment limit (equivalent to -S command line option)
     *
     * @param segmentLimit - the limit in bytes
     */
    public void setSegmentLimit(final long segmentLimit) {
        this.segmentLimit = segmentLimit;
    }

    /**
     * Sets strip debug attributes. If true, all debug attributes (i.e. LineNumberTable, SourceFile, LocalVariableTable and
     * LocalVariableTypeTable attributes) are stripped when reading the input class files and not included in the output
     * archive.
     *
     * @param stripDebug If true, all debug attributes.
     */
    public void setStripDebug(final boolean stripDebug) {
        this.stripDebug = stripDebug;
    }

    /**
     * Sets the compressor behavior when an unknown attribute is encountered.
     *
     * @param unknownAttributeAction - the action to perform
     */
    public void setUnknownAttributeAction(final String unknownAttributeAction) {
        this.unknownAttributeAction = unknownAttributeAction;
        if (!PASS.equals(unknownAttributeAction) && !ERROR.equals(unknownAttributeAction) && !STRIP.equals(unknownAttributeAction)) {
            throw new IllegalArgumentException("Incorrect option for -U, " + unknownAttributeAction);
        }
    }

    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

}

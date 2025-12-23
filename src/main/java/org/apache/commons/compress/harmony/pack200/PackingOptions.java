/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.harmony.pack200;

import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Attribute;

/**
 * Manages the various options available for pack200.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class PackingOptions {

    private static final Attribute[] EMPTY_ATTRIBUTE_ARRAY = {};

    /**
     * Default segment limit.
     */
    public static final long SEGMENT_LIMIT = 1_000_000L;

    /**
     * Strip action constant.
     */
    public static final String STRIP = "strip";

    /**
     * Error action constant.
     */
    public static final String ERROR = "error";

    /**
     * Pass action constant.
     */
    public static final String PASS = "pass";

    /**
     * Keep action constant.
     */
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

    /**
     * Constructs a new PackingOptions.
     */
    public PackingOptions() {
    }

    /**
     * Adds a class attribute action.
     *
     * @param attributeName the attribute name.
     * @param action the action.
     */
    public void addClassAttributeAction(final String attributeName, final String action) {
        classAttributeActions.put(attributeName, action);
    }

    /**
     * Adds a code attribute action.
     *
     * @param attributeName the attribute name.
     * @param action the action.
     */
    public void addCodeAttributeAction(final String attributeName, final String action) {
        codeAttributeActions.put(attributeName, action);
    }

    /**
     * Adds a field attribute action.
     *
     * @param attributeName the attribute name.
     * @param action the action.
     */
    public void addFieldAttributeAction(final String attributeName, final String action) {
        fieldAttributeActions.put(attributeName, action);
    }

    /**
     * Adds a method attribute action.
     *
     * @param attributeName the attribute name.
     * @param action the action.
     */
    public void addMethodAttributeAction(final String attributeName, final String action) {
        methodAttributeActions.put(attributeName, action);
    }

    private void addOrUpdateAttributeActions(final List<Attribute> prototypes, final Map<String, String> attributeActions, final int tag) {
        if (attributeActions != null && attributeActions.size() > 0) {
            NewAttribute newAttribute;
            for (final Entry<String, String> entry : attributeActions.entrySet()) {
                final String name = entry.getKey();
                final String action = entry.getValue();
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
                    switch (action) {
                    case ERROR:
                        newAttribute = new NewAttribute.ErrorAttribute(name, tag);
                        break;
                    case STRIP:
                        newAttribute = new NewAttribute.StripAttribute(name, tag);
                        break;
                    case PASS:
                        newAttribute = new NewAttribute.PassAttribute(name, tag);
                        break;
                    default:
                        newAttribute = new NewAttribute(name, action, tag);
                        break;
                    }
                    prototypes.add(newAttribute);
                }
            }
        }
    }

    /**
     * Tell the compressor to pass the file with the given name, or if the name is a directory name all files under that directory will be passed.
     *
     * @param passFileName the file name
     */
    public void addPassFile(final String passFileName) {
        String fileSeparator = FileSystems.getDefault().getSeparator();
        if (fileSeparator.equals("\\")) {
            // Need to escape backslashes for replaceAll(), which uses regex
            fileSeparator += "\\";
        }
        passFiles.add(passFileName.replaceAll(fileSeparator, "/"));
    }

    /**
     * Gets the deflate hint.
     *
     * @return the deflate hint.
     */
    public String getDeflateHint() {
        return deflateHint;
    }

    /**
     * Gets the effort level.
     *
     * @return the effort level.
     */
    public int getEffort() {
        return effort;
    }

    /**
     * Gets the log file.
     *
     * @return the log file.
     */
    public String getLogFile() {
        return logFile;
    }

    /**
     * Gets the modification time.
     *
     * @return the modification time.
     */
    public String getModificationTime() {
        return modificationTime;
    }

    private String getOrDefault(final Map<String, String> map, final String type, final String defaultValue) {
        return map == null ? defaultValue : map.getOrDefault(type, defaultValue);
    }

    /**
     * Gets the segment limit.
     *
     * @return the segment limit.
     */
    public long getSegmentLimit() {
        return segmentLimit;
    }

    /**
     * Gets the unknown attribute action.
     *
     * @return the unknown attribute action.
     */
    public String getUnknownAttributeAction() {
        return unknownAttributeAction;
    }

    /**
     * Gets the unknown attribute prototypes.
     *
     * @return the attribute prototypes.
     */
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

    /**
     * Gets the unknown class attribute action.
     *
     * @param type the attribute type.
     * @return the action.
     */
    public String getUnknownClassAttributeAction(final String type) {
        return getOrDefault(classAttributeActions, type, unknownAttributeAction);
    }

    /**
     * Gets the unknown code attribute action.
     *
     * @param type the attribute type.
     * @return the action.
     */
    public String getUnknownCodeAttributeAction(final String type) {
        return getOrDefault(codeAttributeActions, type, unknownAttributeAction);
    }

    /**
     * Gets the unknown field attribute action.
     *
     * @param type the attribute type.
     * @return the action.
     */
    public String getUnknownFieldAttributeAction(final String type) {
        return getOrDefault(fieldAttributeActions, type, unknownAttributeAction);
    }

    /**
     * Gets the unknown method attribute action.
     *
     * @param type the attribute type.
     * @return the action.
     */
    public String getUnknownMethodAttributeAction(final String type) {
        return getOrDefault(methodAttributeActions, type, unknownAttributeAction);
    }

    /**
     * Tests whether gzip is enabled.
     *
     * @return true if gzip is enabled.
     */
    public boolean isGzip() {
        return gzip;
    }

    /**
     * Tests whether deflate hint should be kept.
     *
     * @return true if deflate hint should be kept.
     */
    public boolean isKeepDeflateHint() {
        return KEEP.equals(deflateHint);
    }

    /**
     * Tests whether file order should be kept.
     *
     * @return true if file order should be kept.
     */
    public boolean isKeepFileOrder() {
        return keepFileOrder;
    }

    /**
     * Tests whether the file should be passed.
     *
     * @param passFileName the file name.
     * @return true if the file should be passed.
     */
    public boolean isPassFile(final String passFileName) {
        for (String pass : passFiles) {
            if (passFileName.equals(pass)) {
                return true;
            }
            if (!pass.endsWith(".class")) { // a whole directory is
                // passed
                if (!pass.endsWith("/")) {
                    // Make sure we don't get any false positives (for example
                    // exclude "org/apache/harmony/pack" should not match
                    // files under "org/apache/harmony/pack200/")
                    pass += "/";
                }
                return passFileName.startsWith(pass);
            }
        }
        return false;
    }

    /**
     * Tests whether debug info should be stripped.
     *
     * @return true if debug info should be stripped.
     */
    public boolean isStripDebug() {
        return stripDebug;
    }

    /**
     * Tests whether verbose mode is enabled.
     *
     * @return true if verbose mode is enabled.
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Removes a pass file.
     *
     * @param passFileName the file name to remove.
     */
    public void removePassFile(final String passFileName) {
        passFiles.remove(passFileName);
    }

    /**
     * Sets the deflate hint.
     *
     * @param deflateHint the deflate hint.
     */
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

    /**
     * Sets whether to use gzip.
     *
     * @param gzip true to enable gzip.
     */
    public void setGzip(final boolean gzip) {
        this.gzip = gzip;
    }

    /**
     * Sets whether to keep file order.
     *
     * @param keepFileOrder true to keep file order.
     */
    public void setKeepFileOrder(final boolean keepFileOrder) {
        this.keepFileOrder = keepFileOrder;
    }

    /**
     * Sets the log file.
     *
     * @param logFile the log file path.
     */
    public void setLogFile(final String logFile) {
        this.logFile = logFile;
    }

    /**
     * Sets the modification time.
     *
     * @param modificationTime the modification time.
     */
    public void setModificationTime(final String modificationTime) {
        if (!KEEP.equals(modificationTime) && !"latest".equals(modificationTime)) {
            throw new IllegalArgumentException("Bad argument: -m " + modificationTime + " ? transmit modtimes should be either latest or keep (default)");
        }
        this.modificationTime = modificationTime;
    }

    /**
     * Sets quiet mode.
     *
     * @param quiet true for quiet mode.
     */
    public void setQuiet(final boolean quiet) {
        this.verbose = !quiet;
    }

    /**
     * Sets the segment limit (equivalent to -S command line option)
     *
     * @param segmentLimit the limit in bytes.
     */
    public void setSegmentLimit(final long segmentLimit) {
        this.segmentLimit = segmentLimit;
    }

    /**
     * Sets strip debug attributes. If true, all debug attributes (i.e. LineNumberTable, SourceFile, LocalVariableTable and LocalVariableTypeTable attributes)
     * are stripped when reading the input class files and not included in the output archive.
     *
     * @param stripDebug If true, all debug attributes.
     */
    public void setStripDebug(final boolean stripDebug) {
        this.stripDebug = stripDebug;
    }

    /**
     * Sets the compressor behavior when an unknown attribute is encountered.
     *
     * @param unknownAttributeAction   the action to perform
     */
    public void setUnknownAttributeAction(final String unknownAttributeAction) {
        this.unknownAttributeAction = unknownAttributeAction;
        if (!PASS.equals(unknownAttributeAction) && !ERROR.equals(unknownAttributeAction) && !STRIP.equals(unknownAttributeAction)) {
            throw new IllegalArgumentException("Incorrect option for -U, " + unknownAttributeAction);
        }
    }

    /**
     * Sets verbose mode.
     *
     * @param verbose true for verbose mode.
     */
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

}

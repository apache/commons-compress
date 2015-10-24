/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress2.archivers;

/**
 * Encapsulates owner information of an archive entry.
 *
 * <p>Fields that are not supported by the archive format may be null.</p>
 * @Immutable
 */
public class OwnerInformation {

    private final String userName, groupName;
    private final int userId, groupId;

    /**
     * Creates an OwnerInformation without names.
     * @param userId numerical id of the owner
     * @param groupId numerical id of the group owning the entry
     */
    public OwnerInformation(int userId, int groupId) {
        this(null, null, userId, groupId);
    }

    /**
     * Creates an OwnerInformation.
     * @param userName the name of the owner
     * @param groupName the name of the group owning the entry
     * @param userId numerical id of the owner
     * @param groupId numerical id of the group owning the entry
     */
    public OwnerInformation(String userName, String groupName, int userId, int groupId) {
        this.userName = userName;
        this.groupName = groupName;
        this.userId = userId;
        this.groupId = groupId;
    }

    /**
     * Gets the name of the owner.
     * @return the name of the owner, may be null
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Gets the name of the group owning the entry.
     * @return the name of the group owning the entry, may be null
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Gets numerical id of the owner.
     * @return numerical id of the owner
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets numerical id of the group owning the entry.
     * @return numerical id of the group owning the entry
     */
    public int getGroupId() {
        return groupId;
    }

    @Override
    public int hashCode() {
        return 17 * groupId + userId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OwnerInformation other = (OwnerInformation) obj;
        return userId == other.userId
            && groupId == other.groupId
            && equals(userName, other.userName)
            && equals(groupName, other.groupName);
    }

    private static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }
}

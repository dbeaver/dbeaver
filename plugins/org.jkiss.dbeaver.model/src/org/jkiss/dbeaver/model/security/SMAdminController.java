/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.security;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.security.user.SMObjectPermissions;
import org.jkiss.dbeaver.model.security.user.SMRole;
import org.jkiss.dbeaver.model.security.user.SMUser;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin interface
 */
public interface SMAdminController extends SMController {

    ///////////////////////////////////////////
    // Users

    void createUser(String userId, Map<String, String> metaParameters) throws DBException;

    void deleteUser(String userId) throws DBException;

    void setUserRoles(String userId, String[] roleIds, String grantorId) throws DBException;

    SMUser getUserById(String userId) throws DBException;

    @NotNull
    SMUser[] findUsers(String userNameMask) throws DBException;

    void setUserMeta(String userId, Map<String, Object> metaParameters) throws DBException;

    void enableUser(String userId, boolean enabled) throws DBException;

    ///////////////////////////////////////////
    // Roles

    @NotNull
    SMRole[] readAllRoles() throws DBException;

    SMRole findRole(String roleId) throws DBException;

    @NotNull
    String[] getRoleSubjects(String roleId) throws DBException;

    void createRole(String roleId, String name, String description, String grantor) throws DBException;

    void updateRole(String roleId, String name, String description) throws DBException;

    void deleteRole(String roleId) throws DBException;

    ///////////////////////////////////////////
    // Permissions
    void setSubjectPermissions(String subjectId, List<String> permissionIds, String grantorId) throws DBException;

    void setSubjectConnectionAccess(@NotNull String subjectId, @NotNull List<String> connectionIds, String grantor) throws DBException;

    void setObjectPermissions(
        @NotNull String objectId,
        @NotNull SMObjectType objectType,
        @NotNull Set<String> subjectIds,
        @NotNull Set<String> permissions,
        @NotNull String grantor
    ) throws DBException;


    @NotNull
    SMObjectPermissions getObjectPermissions(
        @NotNull String subjectId,
        @NotNull String objectId,
        @NotNull SMObjectType objectType
    ) throws DBException;

    @NotNull
    List<SMObjectPermissionsGrant> getObjectPermissionGrants(
        @NotNull String objectId,
        @NotNull SMObjectType smObjectType
    ) throws DBException;
}

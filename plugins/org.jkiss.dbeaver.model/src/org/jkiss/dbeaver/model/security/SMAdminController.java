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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.security.user.SMRole;
import org.jkiss.dbeaver.model.security.user.SMUser;

import java.util.List;
import java.util.Map;

/**
 * Admin interface
 */
public interface SMAdminController<USER extends SMUser, ROLE extends SMRole> extends SMController<USER, ROLE> {

    ///////////////////////////////////////////
    // Users

    void createUser(USER user) throws DBCException;

    void deleteUser(String userId) throws DBCException;

    void setUserRoles(String userId, String[] roleIds, String grantorId) throws DBCException;

    USER getUserById(String userId) throws DBCException;

    @NotNull
    USER[] findUsers(String userNameMask) throws DBCException;

    void setUserMeta(String userId, Map<String, Object> metaParameters) throws DBCException;

    ///////////////////////////////////////////
    // Roles

    @NotNull
    ROLE[] readAllRoles() throws DBCException;

    ROLE findRole(String roleId) throws DBCException;

    @NotNull
    String[] getRoleSubjects(String roleId) throws DBCException;

    void createRole(ROLE role, String grantor) throws DBCException;

    void updateRole(ROLE role) throws DBCException;

    void deleteRole(String roleId) throws DBCException;

    ///////////////////////////////////////////
    // Permissions
    void setSubjectPermissions(String subjectId, List<String> permissionIds, String grantorId) throws DBCException;

    void setSubjectConnectionAccess(@NotNull String subjectId, @NotNull List<String> connectionIds, String grantor) throws DBCException;
}

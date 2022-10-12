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
import org.jkiss.dbeaver.model.security.user.SMTeam;
import org.jkiss.dbeaver.model.security.user.SMUser;

import java.util.List;
import java.util.Map;

/**
 * Admin interface
 */
public interface SMAdminController extends SMController {

    ///////////////////////////////////////////
    // Users

    void createUser(String userId, Map<String, String> metaParameters, boolean enabled) throws DBException;

    void deleteUser(String userId) throws DBException;

    void setUserTeams(String userId, String[] teamIds, String grantorId) throws DBException;

    SMUser getUserById(String userId) throws DBException;

    @NotNull
    SMUser[] findUsers(String userNameMask) throws DBException;

    void setUserMeta(String userId, Map<String, Object> metaParameters) throws DBException;

    void enableUser(String userId, boolean enabled) throws DBException;

    ///////////////////////////////////////////
    // Teams

    @NotNull
    SMTeam[] readAllTeams() throws DBException;

    SMTeam findTeam(String teamId) throws DBException;

    @NotNull
    String[] getTeamMembers(String teamId) throws DBException;

    void createTeam(String teamId, String name, String description, String grantor) throws DBException;

    void updateTeam(String teamId, String name, String description) throws DBException;

    void deleteTeam(String teamId) throws DBException;

    ///////////////////////////////////////////
    // Permissions
    void setSubjectPermissions(String subjectId, List<String> permissionIds, String grantorId) throws DBException;

    /**
     * Delete all assigned object permissions for subject
     */
    void deleteAllSubjectObjectPermissions(
        @NotNull String subjectId,
        @NotNull SMObjectType objectType
    ) throws DBException;

    List<SMObjectPermissionsGrant> getSubjectObjectPermissionGrants(
        @NotNull String subjectId,
        @NotNull SMObjectType smObjectType
    ) throws DBException;
}

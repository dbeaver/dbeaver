/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.security.user.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin interface
 */
public interface SMAdminController extends SMController {

    ///////////////////////////////////////////
    // Users

    /**
     * Gets user teams.
     *
     * @param userId the user id
     * @return the user team [ ]
     * @throws DBException the db exception
     */
    @NotNull
    SMUserTeam[] getUserTeams(String userId) throws DBException;

    /**
     * Create user.
     *
     * @param userId          the user id
     * @param metaParameters  the meta parameters
     * @param enabled         the enabled
     * @param defaultAuthRole the default auth role
     * @throws DBException the db exception
     */
    void createUser(
        @NotNull String userId,
        @NotNull Map<String, String> metaParameters,
        boolean enabled,
        @Nullable String defaultAuthRole
    ) throws DBException;

    void importUsers(@NotNull SMUserImportList userImportList) throws DBException;

    void deleteUser(String userId) throws DBException;

    void invalidateAllTokens() throws DBException;

    void setUserTeams(String userId, String[] teamIds, String grantorId) throws DBException;

    void addUserTeams(@NotNull String userId, @NotNull String[] teamIds, @NotNull String grantorId) throws DBException;

    void deleteUserTeams(@NotNull String userId, @NotNull String[] teamIds) throws DBException;

    void setUserTeamRole(@NotNull String userId, @NotNull String teamId, @Nullable String teamRole) throws DBException;


    /**
     * Gets user by id.
     *
     * @param userId the user id
     * @return the user by id
     * @throws DBException the db exception
     */
    SMUser getUserById(String userId) throws DBException;

    @NotNull
    SMUser[] findUsers(String userNameMask) throws DBException;

    @NotNull
    SMUser[] findUsers(@NotNull SMUserFilter filter) throws DBException;

    int countUsers(@NotNull SMUserFilter filter) throws DBException;

    void enableUser(String userId, boolean enabled) throws DBException;

    void setUserAuthRole(@NotNull String userId, @Nullable String authRole) throws DBException;

    ///////////////////////////////////////////
    // Teams

    @NotNull
    SMTeam[] readAllTeams() throws DBException;

    SMTeam findTeam(String teamId) throws DBException;

    /**
     * Creates a new team with specified team id that will be in lower-case.
     */
    SMTeam createTeam(
        @NotNull String teamId,
        @Nullable String name,
        @Nullable String description,
        @NotNull String grantor
    ) throws DBException;

    void updateTeam(String teamId, String name, String description) throws DBException;

    void deleteTeam(String teamId, boolean force) throws DBException;

    ///////////////////////////////////////////
    // Credentials

    /**
     * Sets user credentials for specified provider.
     *
     * @param userId         the user id
     * @param authProviderId the auth provider id
     * @param credentials    the credentials
     * @throws DBException the db exception
     */
    void setUserCredentials(
        @NotNull String userId,
        @NotNull String authProviderId,
        @NotNull Map<String, Object> credentials
    ) throws DBException;

    /**
     * Delete user credentials for specified provider.
     *
     * @param userId         the user id
     * @param authProviderId the auth provider id
     * @throws DBException the db exception
     */
    void deleteUserCredentials(
        @NotNull String userId,
        @NotNull String authProviderId
    ) throws DBException;

    /**
     * Returns list of auth provider IDs associated with this user
     *
     * @param userId the user id
     * @return the string [ ]
     * @throws DBException the db exception
     */
    String[] getUserLinkedProviders(@NotNull String userId) throws DBException;

    ///////////////////////////////////////////
    // General

    @NotNull
    SMPropertyDescriptor[] getMetaParametersBySubjectType(SMSubjectType subjectType) throws DBException;

    void setSubjectMetas(@NotNull String subjectId, @NotNull Map<String, String> metaParameters) throws DBException;

    // Permissions

    /**
     * Gets subject permissions.
     *
     * @param subjectId the subject id
     * @return the subject permissions
     * @throws DBException the db exception
     */
    @NotNull
    Set<String> getSubjectPermissions(String subjectId) throws DBException;

    /**
     * Sets subject permissions.
     *
     * @param subjectId     the subject id
     * @param permissionIds the permission ids
     * @param grantorId     the grantor id
     * @throws DBException the db exception
     */
    void setSubjectPermissions(String subjectId, List<String> permissionIds, String grantorId) throws DBException;

    /**
     * Delete all assigned object permissions for subject
     */
    void deleteAllSubjectObjectPermissions(
        @NotNull String subjectId,
        @NotNull SMObjectType objectType
    ) throws DBException;

    /**
     * Gets subject object permission grants.
     *
     * @param subjectId    the subject id
     * @param smObjectType the sm object type
     * @return the subject object permission grants
     * @throws DBException the db exception
     */
    List<SMObjectPermissionsGrant> getSubjectObjectPermissionGrants(
        @NotNull String subjectId,
        @NotNull SMObjectType smObjectType
    ) throws DBException;


    void addObjectPermissions(
        @NotNull Set<String> objectIds,
        @NotNull SMObjectType objectType,
        @NotNull Set<String> subjectIds,
        @NotNull Set<String> permissions,
        @NotNull String grantor
    ) throws DBException;

    void deleteObjectPermissions(
        @NotNull Set<String> objectIds,
        @NotNull SMObjectType objectType,
        @NotNull Set<String> subjectIds,
        @NotNull Set<String> permissions
    ) throws DBException;

    @NotNull
    List<SMTeamMemberInfo> getTeamMembersInfo(@NotNull String teamId) throws DBException;

}

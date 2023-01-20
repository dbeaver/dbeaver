/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPObjectController;
import org.jkiss.dbeaver.model.auth.SMAuthCredentialsManager;
import org.jkiss.dbeaver.model.security.user.SMAuthPermissions;
import org.jkiss.dbeaver.model.security.user.SMObjectPermissions;
import org.jkiss.dbeaver.model.security.user.SMTeam;
import org.jkiss.dbeaver.model.security.user.SMUser;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin interface
 */
public interface SMController extends DBPObjectController,
    SMAuthCredentialsManager, SMAuthController {


    ///////////////////////////////////////////
    // Users

    /**
     * Gets user teams.
     *
     * @return the user team [ ]
     * @throws DBException the db exception
     */
    @NotNull
    SMTeam[] getCurrentUserTeams() throws DBException;

    /**
     * Gets current active user.
     *
     * @return the user
     * @throws DBException the db exception
     */
    @NotNull
    SMUser getCurrentUser() throws DBException;

    /**
     * Gets user parameters.
     *
     * @return the user parameters
     * @throws DBException the db exception
     */
    Map<String, Object> getCurrentUserParameters() throws DBException;

    /**
     * Sets user parameter.
     *
     * @param name  the name
     * @param value the value
     * @throws DBException the db exception
     */
    void setCurrentUserParameter(String name, Object value) throws DBException;

    ///////////////////////////////////////////
    // Credentials

    /**
     * Gets user credentials for specified provider
     *
     * @param authProviderId the auth provider id
     * @return the user credentials
     * @throws DBException the db exception
     */
    @NotNull
    Map<String, Object> getCurrentUserCredentials(@NotNull String authProviderId) throws DBException;

    /**
     * Sets user credentials for specified provider.
     *
     * @param authProviderId the auth provider id
     * @param credentials    the credentials
     * @throws DBException the db exception
     */
    void setCurrentUserCredentials(
        @NotNull String authProviderId,
        @NotNull Map<String, Object> credentials
    ) throws DBException;

    /**
     * Returns list of auth provider IDs associated with this user
     *
     * @return the string [ ]
     * @throws DBException the db exception
     */
    String[] getCurrentUserLinkedProviders() throws DBException;

    ///////////////////////////////////////////
    // Permissions

    /**
     * Gets user permissions.
     *
     * @param userId the user id
     * @return the user permissions
     * @throws DBException the db exception
     */
    @NotNull
    Set<String> getUserPermissions(String userId) throws DBException;

    ///////////////////////////////////////////
    // Sessions

    boolean isSessionPersisted(String id) throws DBException;


    /**
     * Invalidate current sm session and tokens
     *
     * @throws DBException if the current session is not found or something went wrong
     */
    void logout() throws DBException;

    /**
     * Refresh current sm session and generate new token
     *
     * @throws DBException if the current refresh token invalid
     */
    SMTokens refreshSession(@NotNull String refreshToken) throws DBException;

    /**
     * Updates session.
     *
     * @param sessionId  the session id
     * @param parameters the parameters
     * @throws DBException the db exception
     */
    void updateSession(@NotNull String sessionId, Map<String, Object> parameters) throws DBException;

    ///////////////////////////////////////////
    // Permissions

    /**
     * Gets token permissions.
     *
     * @return the token permissions
     * @throws DBException the db exception
     */
    SMAuthPermissions getTokenPermissions() throws DBException;

    ///////////////////////////////////////////
    // Auth providers

    SMAuthProviderDescriptor[] getAvailableAuthProviders() throws DBException;

    /**
     * Gets all available objects permissions.
     *
     * @param objectType the object type
     * @return the all available objects permissions
     * @throws DBException the db exception
     */
    @NotNull
    List<SMObjectPermissions> getAllAvailableObjectsPermissions(@NotNull SMObjectType objectType) throws DBException;

    void setObjectPermissions(
        @NotNull Set<String> objectIds,
        @NotNull SMObjectType objectType,
        @NotNull Set<String> subjectIds,
        @NotNull Set<String> permissions,
        @NotNull String grantor
    ) throws DBException;

    @NotNull
    List<SMObjectPermissionsGrant> getObjectPermissionGrants(
        @NotNull String objectId,
        @NotNull SMObjectType smObjectType
    ) throws DBException;

    /**
     * Gets object permissions.
     *
     * @param subjectId  the subject id
     * @param objectId   the object id
     * @param objectType the object type
     * @return the object permissions
     * @throws DBException the db exception
     */
    @NotNull
    SMObjectPermissions getObjectPermissions(
        @NotNull String subjectId,
        @NotNull String objectId,
        @NotNull SMObjectType objectType
    ) throws DBException;

    void deleteAllObjectPermissions(
        @NotNull String objectId,
        @NotNull SMObjectType objectType
    ) throws DBException;
}

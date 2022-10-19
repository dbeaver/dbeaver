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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObjectController;
import org.jkiss.dbeaver.model.auth.SMAuthCredentialsManager;
import org.jkiss.dbeaver.model.auth.SMAuthInfo;
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
public interface SMController extends DBPObjectController, SMAuthCredentialsManager {

    ///////////////////////////////////////////
    // Users
    @NotNull
    SMTeam[] getUserTeams(String userId) throws DBException;

    SMUser getUserById(String userId) throws DBException;

    Map<String, Object> getUserParameters(String userId) throws DBException;

    void setUserParameter(String userId, String name, Object value) throws DBException;

    ///////////////////////////////////////////
    // Credentials

    /**
     * Sets user credentials for specified provider
     */
    void setUserCredentials(@NotNull String userId, @NotNull String authProviderId, @NotNull Map<String, Object> credentials) throws DBException;

    /**
     * Returns list of auth provider IDs associated with this user
     */
    String[] getUserLinkedProviders(@NotNull String userId) throws DBException;

    ///////////////////////////////////////////
    // Permissions

    @NotNull
    Set<String> getSubjectPermissions(String subjectId) throws DBException;

    @NotNull
    Set<String> getUserPermissions(String userId) throws DBException;

    ///////////////////////////////////////////
    // Sessions

    boolean isSessionPersisted(String id) throws DBException;

    SMAuthInfo authenticateAnonymousUser(
        @NotNull String appSessionId,
        @NotNull Map<String, Object> sessionParameters,
        @NotNull SMSessionType sessionType
    ) throws DBException;

    SMAuthInfo authenticate(
        @NotNull String appSessionId,
        @Nullable String previousSmSessionId,
        @NotNull Map<String, Object> sessionParameters,
        @NotNull SMSessionType sessionType,
        @NotNull String authProviderId,
        @Nullable String authProviderConfigurationId,
        @NotNull Map<String, Object> userCredentials
    ) throws DBException;

    SMAuthInfo getAuthStatus(@NotNull String authId) throws DBException;

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

    void updateSession(
        @NotNull String sessionId,
        @Nullable String userId,
        Map<String, Object> parameters) throws DBException;

    ///////////////////////////////////////////
    // Permissions

    SMAuthPermissions getTokenPermissions(String token) throws DBException;

    ///////////////////////////////////////////
    // Auth providers

    SMAuthProviderDescriptor[] getAvailableAuthProviders() throws DBException;

    @NotNull
    List<SMObjectPermissions> getAllAvailableObjectsPermissions(
        @NotNull String subjectId,
        @NotNull SMObjectType objectType
    ) throws DBException;

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

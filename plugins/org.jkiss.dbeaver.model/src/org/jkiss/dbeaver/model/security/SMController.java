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
import org.jkiss.dbeaver.model.auth.SMAuthCredentialsManager;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.security.user.SMRole;
import org.jkiss.dbeaver.model.security.user.SMUser;

import java.util.Map;
import java.util.Set;

/**
 * Admin interface
 */
public interface SMController<USER extends SMUser, ROLE extends SMRole> extends SMAuthCredentialsManager {

    ///////////////////////////////////////////
    // Users
    @NotNull
    ROLE[] getUserRoles(String userId) throws DBCException;

    USER getUserById(String userId) throws DBCException;

    Map<String, Object> getUserParameters(String userId) throws DBCException;

    void setUserParameter(String userId, String name, Object value) throws DBCException;

    ///////////////////////////////////////////
    // Credentials

    /**
     * Sets user credentials for specified provider
     */
    void setUserCredentials(String userId, String authProviderId, Map<String, Object> credentials) throws DBCException;

    /**
     * Returns list of auth provider IDs associated with this user
     */
    String[] getUserLinkedProviders(String userId) throws DBCException;

    ///////////////////////////////////////////
    // Permissions

    @NotNull
    Set<String> getSubjectPermissions(String subjectId) throws DBCException;

    @NotNull
    Set<String> getUserPermissions(String userId) throws DBCException;

    ///////////////////////////////////////////
    // Sessions

    boolean isSessionPersisted(String id) throws DBCException;

    void createSession(@NotNull String appSessionId, @Nullable String userId, @NotNull Map<String, Object> parameters, @NotNull SMSessionType sessionType) throws DBCException;

    void updateSession(@NotNull String sessionId, @Nullable String userId, Map<String, Object> parameters) throws DBCException;

    ///////////////////////////////////////////
    // Permissions

    @NotNull
    SMDataSourceGrant[] getSubjectConnectionAccess(@NotNull String[] subjectId) throws DBCException;

    @NotNull
    SMDataSourceGrant[] getConnectionSubjectAccess(String connectionId) throws DBCException;

    void setConnectionSubjectAccess(@NotNull String connectionId, @Nullable String[] subjects, @Nullable String grantorId) throws DBCException;
}

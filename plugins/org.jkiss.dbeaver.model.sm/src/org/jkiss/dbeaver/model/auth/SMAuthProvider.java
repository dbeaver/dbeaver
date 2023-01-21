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
package org.jkiss.dbeaver.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.security.SMController;

import java.util.Map;

/**
 * Auth provider
 */
public interface SMAuthProvider<AUTH_SESSION extends SMSession> {
    /**
     * Validates that user may be associated with local user
     *
     * @param userCredentials credentials from authExternalUser
     * @param activeUserId
     * @return new user ID. If activeUserId is not null then it must be the same.
     */
    @NotNull
    String validateLocalAuth(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SMController securityController,
        @NotNull Map<String, Object> providerConfig,
        @NotNull Map<String, Object> userCredentials,
        @Nullable String activeUserId) throws DBException;

    AUTH_SESSION openSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SMSession mainSession,
        @NotNull Map<String, Object> providerConfig, // Auth provider configuration (e.g. 3rd party auth server address)
        @NotNull Map<String, Object> userCredentials // Saved user credentials (e.g. associated 3rd party provider user name or realm)
    ) throws DBException;

    void closeSession(
        @NotNull SMSession mainSession,
        AUTH_SESSION session) throws DBException;

    void refreshSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SMSession mainSession,
        AUTH_SESSION session) throws DBException;

}

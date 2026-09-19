/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model.auth;

import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNativeCredentials;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * Credentials of the token based ClickHouse auth models. User name and password are not used:
 * the identity comes from the JWT claims.
 */
public class ClickhouseJWTCredentials extends AuthModelDatabaseNativeCredentials {

    private String idpAccessToken;
    private String idpRefreshToken;

    /**
     * Identity provider access token, persisted in the secure storage to avoid an interactive login
     * on every application start.
     */
    public String getIdpAccessToken() {
        return idpAccessToken;
    }

    public void setIdpAccessToken(String idpAccessToken) {
        this.idpAccessToken = idpAccessToken;
    }

    public String getIdpRefreshToken() {
        return idpRefreshToken;
    }

    public void setIdpRefreshToken(String idpRefreshToken) {
        this.idpRefreshToken = idpRefreshToken;
    }

    @Override
    @Property(hidden = true)
    public String getUserName() {
        return super.getUserName();
    }

    @Override
    @Property(hidden = true)
    public String getUserPassword() {
        return super.getUserPassword();
    }
}

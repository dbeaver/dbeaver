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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.utils.CommonUtils;

/**
 * Settings of the external OpenID Connect provider.
 *
 * @param issuer       issuer or discovery url. May be empty when it can be resolved from the email
 * @param clientId     application (client) id registered in the identity provider
 * @param clientSecret optional, only for confidential clients
 * @param audience     audience the token must be issued for, as configured in ClickHouse
 * @param scopes       requested scopes
 * @param email        user email. Used as a login hint and to resolve the Microsoft Entra ID tenant
 * @param callbackPort local port the browser is redirected to
 * @param useDeviceCode use the device authorization grant instead of a loopback redirect
 */
public record ClickhouseOIDCSettings(
    @Nullable String issuer,
    @NotNull String clientId,
    @Nullable String clientSecret,
    @Nullable String audience,
    @Nullable String scopes,
    @Nullable String email,
    int callbackPort,
    boolean useDeviceCode
) {
    /**
     * Microsoft Entra ID resolves a tenant by any of its verified domains, so the issuer of a
     * corporate account can be discovered from the email address alone.
     */
    private static final String ENTRA_ID_ISSUER_FORMAT = "https://login.microsoftonline.com/%s/v2.0";

    /**
     * Returns the configured issuer, falling back to the Microsoft Entra ID tenant of the email domain.
     */
    @NotNull
    public String resolveIssuer() throws DBException {
        if (!CommonUtils.isEmpty(issuer)) {
            return issuer;
        }
        String domain = getEmailDomain(email);
        if (domain == null) {
            throw new DBException("Specify either the identity provider URL or an email address");
        }
        return ENTRA_ID_ISSUER_FORMAT.formatted(domain);
    }

    /**
     * Whether {@link #resolveIssuer()} can determine an issuer from these values.
     * Used to validate the connection settings before they are saved.
     */
    public static boolean canResolveIssuer(@Nullable String issuer, @Nullable String email) {
        return !CommonUtils.isEmpty(issuer) || getEmailDomain(email) != null;
    }

    @Nullable
    private static String getEmailDomain(@Nullable String email) {
        if (CommonUtils.isEmpty(email)) {
            return null;
        }
        int at = email.lastIndexOf('@');
        return at > 0 && at < email.length() - 1 ? email.substring(at + 1).trim() : null;
    }
}

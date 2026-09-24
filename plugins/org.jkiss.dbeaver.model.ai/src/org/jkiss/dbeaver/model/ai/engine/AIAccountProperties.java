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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.openai.AIAccountAuthenticator;

import java.util.Set;

/**
 * Account authentication capability exposed by AI engine properties.
 */
public interface AIAccountProperties extends AIEngineProperties {
    String ACCOUNT_ACCESS_TOKEN_PROPERTY = "accessToken";
    String ACCOUNT_REFRESH_TOKEN_PROPERTY = "refreshToken";
    String ACCOUNT_ID_PROPERTY = "accountId";
    String ACCOUNT_EMAIL_PROPERTY = "accountEmail";
    String ACCOUNT_EXPIRES_AT_PROPERTY = "expiresAt";

    Set<String> ACCOUNT_CREDENTIAL_PROPERTY_IDS = Set.of(
        ACCOUNT_ACCESS_TOKEN_PROPERTY,
        ACCOUNT_REFRESH_TOKEN_PROPERTY,
        ACCOUNT_ID_PROPERTY,
        ACCOUNT_EMAIL_PROPERTY,
        ACCOUNT_EXPIRES_AT_PROPERTY
    );

    boolean isAccountAuthentication();

    boolean isAccountConnected();

    @NotNull
    String getAccountAuthenticationProviderName();

    @Nullable
    String getAccessToken();

    void setAccessToken(@Nullable String accessToken);

    @Nullable
    String getRefreshToken();

    void setRefreshToken(@Nullable String refreshToken);

    @Nullable
    String getAccountId();

    void setStoredAccountId(@Nullable String accountId);

    @Nullable
    String getAccountEmail();

    void setStoredAccountEmail(@Nullable String accountEmail);

    long getStoredExpiresAt();

    void setStoredExpiresAt(long expiresAt);

    void setAccountTokens(@NotNull AIAccountAuthenticator.Tokens tokens);

    void clearAccountTokens();

    void useAccountCredentialsFrom(@NotNull AIAccountProperties source);

    void setAccountTokenPersistence(@Nullable AccountTokenPersistence accountTokenPersistence);

    void setAccountTokenValidator(@Nullable AccountTokenValidator accountTokenValidator);

    void setAccountTokenRefreshHandler(@Nullable AccountTokenRefreshHandler accountTokenRefreshHandler);

    @NotNull
    String getValidAccessToken(@NotNull AIAccountAuthenticator authenticator) throws DBException;

    void saveAccountTokens() throws DBException;

    /**
     * Creates the authenticator for this provider and the selected account authentication mode.
     * Implementations inheriting shared account state must override this method when they use a different protocol.
     */
    @NotNull
    AIAccountAuthenticator createAccountAuthenticator() throws DBException;

    @FunctionalInterface
    interface AccountTokenPersistence {
        void save(@Nullable String previousRefreshToken, @NotNull AIAccountAuthenticator.Tokens tokens) throws DBException;
    }

    @FunctionalInterface
    interface AccountTokenValidator {
        void validate(@Nullable String refreshToken) throws DBException;
    }

    @FunctionalInterface
    interface AccountTokenRefreshHandler {
        @NotNull
        AIAccountAuthenticator.Tokens refresh(
            @NotNull AIAccountAuthenticator authenticator,
            @NotNull String refreshToken
        ) throws DBException;
    }
}

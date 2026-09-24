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
package org.jkiss.dbeaver.model.ai.engine.openai;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.BaseAIEngineProperties;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.auth.AuthProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.SecureProperty;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class OpenAIProperties extends BaseAIEngineProperties implements OpenAIBaseProperties {
    protected static final String GPT_BASE_URL = "gpt.base_url";
    protected static final String GPT_TOKEN = "gpt.token";
    protected static final String GPT_MODEL = "gpt.model";
    protected static final String GPT_CONTEXT_WINDOW_SIZE = "gpt.contextWindowSize";
    public static final String AUTHENTICATION_API_TOKEN = "apiToken";
    public static final String AUTHENTICATION_CHATGPT_ACCOUNT = "chatgptAccount";
    public static final String ACCOUNT_ACCESS_TOKEN_PROPERTY = "accessToken";
    public static final String ACCOUNT_REFRESH_TOKEN_PROPERTY = "refreshToken";
    public static final String ACCOUNT_ID_PROPERTY = "accountId";
    public static final String ACCOUNT_EMAIL_PROPERTY = "accountEmail";
    public static final String ACCOUNT_EXPIRES_AT_PROPERTY = "expiresAt";
    public static final int DEFAULT_ACCOUNT_CONTEXT_WINDOW_SIZE = 272_000;
    private static final String ACCESS_TOKEN = "openai.account.accessToken";
    private static final String REFRESH_TOKEN = "openai.account.refreshToken";


    @Nullable
    @SerializedName(GPT_BASE_URL)
    private String baseUrl;

    @Nullable
    @SecureProperty
    @SerializedName(GPT_TOKEN)
    private String token;

    @Nullable
    @SerializedName(GPT_MODEL)
    protected String model;

    @Nullable
    @SerializedName(GPT_CONTEXT_WINDOW_SIZE)
    protected Integer contextWindowSize;

    @SerializedName("openai.authentication")
    private String authentication = AUTHENTICATION_API_TOKEN;
    @SecureProperty
    @SerializedName(ACCESS_TOKEN)
    private String accessToken;
    @SecureProperty
    @SerializedName(REFRESH_TOKEN)
    private String refreshToken;
    @SerializedName("openai.account.expiresAt")
    private long expiresAt;
    @SerializedName("openai.account.accountId")
    private String accountId;
    @SerializedName("openai.account.email")
    private String accountEmail;
    private transient AIConfigurationProfile profile;
    private transient volatile OpenAIProperties accountCredentialsSource;
    private transient AccountTokenPersistence accountTokenPersistence;
    private transient AccountTokenValidator accountTokenValidator;
    private transient AccountTokenRefreshHandler accountTokenRefreshHandler;

    public OpenAIProperties() {
    }

    @NotNull
    @Override
    @Property(order = 2, required = true)
    public String getBaseUrl() {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return OpenAIClientResponses.OPENAI_ENDPOINT;
        }
        return baseUrl;
    }

    public void setBaseUrl(@Nullable String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Nullable
    @Override
    @Property(order = 1, password = true, hideExpr = AIConstants.AI_NON_GLOBAL_CREDENTIALS_HIDE_EXPRESSION)
    public String getToken() {
        return token;
    }

    public void setToken(@Nullable String token) {
        this.token = token;
    }

    @Nullable
    @Override
    @Property(order = 3)
    public String getModel() {
        if (model != null) {
            return OpenAIModels.getEffectiveModelName(model);
        }

        String modelName = DBWorkbench.getPlatform()
            .getPreferenceStore()
            .getString(OpenAIConstants.GPT_MODEL);
        return OpenAIModels.getEffectiveModelName(modelName);
    }

    public void setModel(@Nullable String model) {
        this.model = model;
    }

    @Override
    public void selectModel(@NotNull AIModel model) {
        setModel(model.name());
        setContextWindowSize(model.contextWindowSize() != null ? model.contextWindowSize() :
            OpenAIModels.getModelByName(model.name()).map(AIModel::contextWindowSize).orElse(AIConstants.DEFAULT_CONTEXT_WINDOW_SIZE));
    }

    @Override
    @Property(order = 4)
    public double getTemperature() {
        if (Double.isFinite(temperature) && temperature != AIUtils.DEFAULT_TEMPERATURE) {
            return temperature;
        }

        return DBWorkbench.getPlatform()
            .getPreferenceStore()
            .getDouble(OpenAIConstants.AI_TEMPERATURE);
    }

    @Override
    @Property(order = 1000)
    public boolean isLoggingEnabled() {
        if (loggingEnabled != null) {
            return loggingEnabled;
        }

        return DBWorkbench.getPlatform()
            .getPreferenceStore()
            .getBoolean(OpenAIConstants.AI_LOG_QUERY);
    }

    @Nullable
    @Override
    @Property(order = 6, min = 1)
    public Integer getContextWindowSize() {
        if (contextWindowSize != null) {
            return contextWindowSize;
        }

        return OpenAIModels.getModelByName(getModel())
            .map(AIModel::contextWindowSize)
            .orElse(null);
    }

    public void setContextWindowSize(@Nullable Integer contextWindowSize) {
        this.contextWindowSize = contextWindowSize;
    }

    @NotNull
    @Property(order = 1)
    public String getAuthentication() {
        return AUTHENTICATION_CHATGPT_ACCOUNT.equals(authentication)
            ? AUTHENTICATION_CHATGPT_ACCOUNT
            : AUTHENTICATION_API_TOKEN;
    }

    public boolean isChatGptAccountAuthentication() {
        return AUTHENTICATION_CHATGPT_ACCOUNT.equals(getAuthentication());
    }

    public boolean isAccountAuthentication() {
        return isChatGptAccountAuthentication();
    }

    public void setAuthentication(@Nullable String authentication) {
        this.authentication = AUTHENTICATION_CHATGPT_ACCOUNT.equals(authentication)
            ? AUTHENTICATION_CHATGPT_ACCOUNT
            : AUTHENTICATION_API_TOKEN;
    }

    @Nullable
    @Property(id = ACCOUNT_ACCESS_TOKEN_PROPERTY, hidden = true, password = true)
    @AuthProperty
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(@Nullable String accessToken) {
        this.accessToken = accessToken;
        accountId = OpenAIAccountAuthenticator.extractAccountId(accessToken);
        accountEmail = OpenAIAccountAuthenticator.extractEmail(accessToken);
        expiresAt = OpenAIAccountAuthenticator.extractExpiresAt(accessToken);
    }

    @Nullable
    @Property(id = ACCOUNT_REFRESH_TOKEN_PROPERTY, hidden = true, password = true)
    @AuthProperty
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(@Nullable String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Nullable
    @Property(id = ACCOUNT_ID_PROPERTY, hidden = true)
    @AuthProperty
    public String getStoredAccountId() {
        return accountId;
    }

    public void setStoredAccountId(@Nullable String accountId) {
        this.accountId = accountId;
    }

    @Nullable
    @Property(id = ACCOUNT_EMAIL_PROPERTY, hidden = true)
    @AuthProperty
    public String getStoredAccountEmail() {
        return accountEmail;
    }

    public void setStoredAccountEmail(@Nullable String accountEmail) {
        this.accountEmail = accountEmail;
    }

    @Property(id = ACCOUNT_EXPIRES_AT_PROPERTY, hidden = true)
    @AuthProperty
    public long getStoredExpiresAt() {
        return expiresAt;
    }

    public void setStoredExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isChatGptAccountConnected() {
        return isAccountConnected();
    }

    public boolean isAccountConnected() {
        OpenAIProperties credentials = getAccountCredentialsOwner();
        synchronized (credentials) {
            return !CommonUtils.isEmpty(credentials.refreshToken);
        }
    }

    @Override
    public boolean isValidConfiguration() {
        return isChatGptAccountAuthentication()
            ? isChatGptAccountConnected()
            : !CommonUtils.isEmpty(token);
    }

    @Nullable
    public String getAccountId() {
        OpenAIProperties credentials = getAccountCredentialsOwner();
        synchronized (credentials) {
            return credentials.accountId;
        }
    }

    @Nullable
    public String getAccountEmail() {
        OpenAIProperties credentials = getAccountCredentialsOwner();
        synchronized (credentials) {
            return credentials.accountEmail;
        }
    }

    public synchronized void setAccountTokens(@NotNull AIAccountAuthenticator.Tokens tokens) {
        accessToken = tokens.accessToken();
        refreshToken = tokens.refreshToken();
        if (tokens.accountId() != null) {
            accountId = tokens.accountId();
        }
        if (tokens.email() != null) {
            accountEmail = tokens.email();
        }
        expiresAt = System.currentTimeMillis() + tokens.expiresInSeconds() * 1000;
    }

    public void clearAccountTokens() {
        OpenAIProperties credentials = getAccountCredentialsOwner();
        synchronized (credentials) {
            credentials.accessToken = null;
            credentials.refreshToken = null;
            credentials.expiresAt = 0;
            credentials.accountId = null;
            credentials.accountEmail = null;
        }
    }

    public synchronized void copyAccountTokensFrom(@NotNull OpenAIProperties source) {
        OpenAIProperties credentials = source.getAccountCredentialsOwner();
        synchronized (credentials) {
            accessToken = credentials.accessToken;
            refreshToken = credentials.refreshToken;
            expiresAt = credentials.expiresAt;
            accountId = credentials.accountId;
            accountEmail = credentials.accountEmail;
        }
    }

    public void useAccountCredentialsFrom(@NotNull OpenAIProperties source) {
        accountCredentialsSource = source.getAccountCredentialsOwner();
    }

    public void setAccountTokenPersistence(@Nullable AccountTokenPersistence accountTokenPersistence) {
        getAccountCredentialsOwner().accountTokenPersistence = accountTokenPersistence;
    }

    public void setAccountTokenValidator(@Nullable AccountTokenValidator accountTokenValidator) {
        getAccountCredentialsOwner().accountTokenValidator = accountTokenValidator;
    }

    public void setAccountTokenRefreshHandler(@Nullable AccountTokenRefreshHandler accountTokenRefreshHandler) {
        getAccountCredentialsOwner().accountTokenRefreshHandler = accountTokenRefreshHandler;
    }

    @NotNull
    public String getValidAccessToken(@NotNull AIAccountAuthenticator authenticator) throws DBException {
        OpenAIProperties credentials = getAccountCredentialsOwner();
        synchronized (credentials) {
            if (credentials.accountTokenValidator != null) {
                credentials.accountTokenValidator.validate(credentials.refreshToken);
            }
            if (!CommonUtils.isEmpty(credentials.accessToken)
                && credentials.expiresAt > System.currentTimeMillis() + 60_000
            ) {
                return credentials.accessToken;
            }
            if (CommonUtils.isEmpty(credentials.refreshToken)) {
                throw new DBException(getAccountProviderName() + " account is not connected");
            }
            String previousRefreshToken = credentials.refreshToken;
            AIAccountAuthenticator.Tokens tokens;
            if (credentials.accountTokenRefreshHandler != null) {
                tokens = credentials.accountTokenRefreshHandler.refresh(authenticator, previousRefreshToken);
            } else {
                tokens = authenticator.refresh(previousRefreshToken);
            }
            credentials.setAccountTokens(tokens);
            if (credentials.accountTokenRefreshHandler == null) {
                credentials.saveAccountTokensInternal(previousRefreshToken, new AIAccountAuthenticator.Tokens(
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    tokens.expiresInSeconds(),
                    credentials.accountId,
                    credentials.accountEmail
                ));
            }
            return credentials.accessToken;
        }
    }

    public void saveAccountTokens() throws DBException {
        OpenAIProperties credentials = getAccountCredentialsOwner();
        synchronized (credentials) {
            long expiresInSeconds = Math.max(0, (credentials.expiresAt - System.currentTimeMillis()) / 1000);
            credentials.saveAccountTokensInternal(credentials.refreshToken, new AIAccountAuthenticator.Tokens(
                credentials.accessToken,
                credentials.refreshToken,
                expiresInSeconds,
                credentials.accountId,
                credentials.accountEmail
            ));
        }
    }

    @Override
    public void resolveSecrets(@NotNull AIConfigurationProfile profile) throws DBException {
        if (token == null) {
            token = AIUtils.getSecretValueOrDefault(profile, OpenAIConstants.GPT_API_TOKEN, token);
        }
        this.profile = profile;
        if (accessToken == null) {
            accessToken = AIUtils.getSecretValueOrDefault(profile, getAccessTokenSecretId(), null);
        }
        if (refreshToken == null) {
            refreshToken = AIUtils.getSecretValueOrDefault(profile, getRefreshTokenSecretId(), null);
        }
        String resolvedAccountId = OpenAIAccountAuthenticator.extractAccountId(null, accessToken);
        if (resolvedAccountId != null) {
            accountId = resolvedAccountId;
        }
        String resolvedEmail = OpenAIAccountAuthenticator.extractEmail(null, accessToken);
        if (resolvedEmail != null) {
            accountEmail = resolvedEmail;
        }
        long resolvedExpiresAt = OpenAIAccountAuthenticator.extractExpiresAt(accessToken);
        if (resolvedExpiresAt > 0) {
            expiresAt = resolvedExpiresAt;
        }
    }

    @Override
    public void saveSecrets(@NotNull AIConfigurationProfile profile) throws DBException {
        AIUtils.setSecretValue(profile, OpenAIConstants.GPT_API_TOKEN, token);
        this.profile = profile;
        AIUtils.setSecretValue(profile, getRefreshTokenSecretId(), refreshToken);
        AIUtils.setSecretValue(profile, getAccessTokenSecretId(), accessToken);
    }

    @Override
    public void deleteSecrets(@NotNull AIConfigurationProfile profile) throws DBException {
        AIUtils.deleteSecretValue(profile, OpenAIConstants.GPT_API_TOKEN);
        AIUtils.deleteSecretValue(profile, getAccessTokenSecretId());
        AIUtils.deleteSecretValue(profile, getRefreshTokenSecretId());
    }

    @NotNull
    protected String getAccountProviderName() {
        return "ChatGPT";
    }

    @NotNull
    protected String getAccessTokenSecretId() {
        return ACCESS_TOKEN;
    }

    @NotNull
    protected String getRefreshTokenSecretId() {
        return REFRESH_TOKEN;
    }

    @NotNull
    private OpenAIProperties getAccountCredentialsOwner() {
        OpenAIProperties source = accountCredentialsSource;
        return source == null ? this : source.getAccountCredentialsOwner();
    }

    private void saveAccountTokensInternal(
        @Nullable String previousRefreshToken,
        @NotNull AIAccountAuthenticator.Tokens tokens
    ) throws DBException {
        if (profile != null) {
            saveSecrets(profile);
        }
        if (accountTokenPersistence != null && tokens.accessToken() != null && tokens.refreshToken() != null) {
            accountTokenPersistence.save(previousRefreshToken, tokens);
        }
    }

    @FunctionalInterface
    public interface AccountTokenPersistence {
        void save(@Nullable String previousRefreshToken, @NotNull AIAccountAuthenticator.Tokens tokens) throws DBException;
    }

    @FunctionalInterface
    public interface AccountTokenValidator {
        void validate(@Nullable String refreshToken) throws DBException;
    }

    @FunctionalInterface
    public interface AccountTokenRefreshHandler {
        @NotNull
        AIAccountAuthenticator.Tokens refresh(
            @NotNull AIAccountAuthenticator authenticator,
            @NotNull String refreshToken
        ) throws DBException;
    }
}

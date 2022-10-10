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
package org.jkiss.dbeaver.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.security.user.SMAuthPermissions;

import java.util.Map;

public class SMAuthInfo {
    @NotNull
    private final SMAuthStatus authStatus;
    @Nullable
    private final String error;
    @NotNull
    private final String authAttemptId;

    @NotNull
    private final Map<String, Object> authData;

    @Nullable
    private final String redirectUrl;

    @Nullable
    private final String smAuthToken;
    @Nullable
    private final String smRefreshToken;
    @Nullable
    private final String authRole;
    @Nullable
    private final SMAuthPermissions authPermissions;

    private SMAuthInfo(
        @NotNull SMAuthStatus authStatus,
        @Nullable String error,
        @NotNull String authAttemptId,
        @NotNull Map<String, Object> authData,
        @Nullable String redirectUrl,
        @Nullable String smAuthToken,
        @Nullable String smRefreshToken,
        @Nullable String authRole,
        @Nullable SMAuthPermissions authPermissions
    ) {
        this.authStatus = authStatus;
        this.error = error;
        this.authAttemptId = authAttemptId;
        this.authData = authData;
        this.redirectUrl = redirectUrl;
        this.smAuthToken = smAuthToken;
        this.smRefreshToken = smRefreshToken;
        this.authRole = authRole;
        this.authPermissions = authPermissions;
    }

    public static SMAuthInfo expired(@NotNull String authAttemptId) {
        return new Builder()
            .setAuthStatus(SMAuthStatus.EXPIRED)
            .setAuthAttemptId(authAttemptId)
            .setAuthData(Map.of())
            .build();
    }

    public static SMAuthInfo error(@NotNull String authAttemptId, @NotNull String error) {
        return new Builder()
            .setAuthStatus(SMAuthStatus.ERROR)
            .setAuthAttemptId(authAttemptId)
            .setError(error)
            .build();
    }

    public static SMAuthInfo inProgress(
        @NotNull String authAttemptId,
        @Nullable String redirectUrl,
        @NotNull Map<String, Object> authData
    ) {
        return new Builder()
            .setAuthStatus(SMAuthStatus.IN_PROGRESS)
            .setAuthAttemptId(authAttemptId)
            .setRedirectUrl(redirectUrl)
            .setAuthData(authData)
            .build();
    }

    public static SMAuthInfo success(
        @NotNull String authAttemptId,
        @NotNull String accessToken,
        @Nullable String refreshToken,
        @NotNull SMAuthPermissions smAuthPermissions,
        @NotNull Map<String, Object> authData
    ) {
        return new Builder()
            .setAuthStatus(SMAuthStatus.SUCCESS)
            .setAuthAttemptId(authAttemptId)
            .setSmAuthToken(accessToken)
            .setSmRefreshToken(refreshToken)
            .setAuthData(authData)
            .setAuthPermissions(smAuthPermissions)
            .build();
    }


    @Nullable
    public String getSmAuthToken() {
        return smAuthToken;
    }

    @Nullable
    public String getSmRefreshToken() {
        return smRefreshToken;
    }

    @Nullable
    public SMAuthPermissions getAuthPermissions() {
        return authPermissions;
    }

    @Nullable
    public String getAuthRole() {
        return authRole;
    }

    @NotNull
    public SMAuthStatus getAuthStatus() {
        return authStatus;
    }

    @NotNull
    public String getAuthAttemptId() {
        return authAttemptId;
    }

    @NotNull
    public Map<String, Object> getAuthData() {
        return authData;
    }

    @Nullable
    public String getRedirectUrl() {
        return redirectUrl;
    }

    @Nullable
    public String getError() {
        return error;
    }


    private static final class Builder {
        private SMAuthStatus authStatus;
        private String error;
        private String authAttemptId;
        private Map<String, Object> authData;
        private String redirectUrl;
        private String smAuthToken;
        private String smRefreshToken;
        private String authRole;
        private SMAuthPermissions authPermissions;

        private Builder() {
        }

        public Builder setAuthStatus(SMAuthStatus authStatus) {
            this.authStatus = authStatus;
            return this;
        }

        public Builder setError(String error) {
            this.error = error;
            return this;
        }

        public Builder setAuthAttemptId(String authAttemptId) {
            this.authAttemptId = authAttemptId;
            return this;
        }

        public Builder setAuthData(Map<String, Object> authData) {
            this.authData = authData;
            return this;
        }

        public Builder setRedirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
            return this;
        }

        public Builder setSmAuthToken(String smAuthToken) {
            this.smAuthToken = smAuthToken;
            return this;
        }

        public Builder setSmRefreshToken(String smRefreshToken) {
            this.smRefreshToken = smRefreshToken;
            return this;
        }

        public Builder setAuthRole(String authRole) {
            this.authRole = authRole;
            return this;
        }

        public Builder setAuthPermissions(SMAuthPermissions authPermissions) {
            this.authPermissions = authPermissions;
            return this;
        }

        public SMAuthInfo build() {
            return new SMAuthInfo(
                authStatus, error, authAttemptId, authData, redirectUrl, smAuthToken, smRefreshToken, authRole, authPermissions);
        }
    }
}

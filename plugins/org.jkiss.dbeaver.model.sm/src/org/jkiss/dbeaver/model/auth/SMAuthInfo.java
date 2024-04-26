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
    private final Map<SMAuthConfigurationReference, Object> authData;

    @Nullable
    private final String redirectUrl;
    @Nullable
    private final String signOutLink;

    @Nullable
    private final String smAccessToken;

    /**
     * Deprecated, use smAccessToken instead
     */
    @Deprecated
    @Nullable //Backward compatibility
    private String smAuthToken;

    @Nullable
    private final String smRefreshToken;
    @Nullable
    private final String authRole;
    @Nullable
    private final SMAuthPermissions authPermissions;

    private final boolean mainAuth;
    private final boolean forceSessionsLogout;

    @Nullable
    private final String errorCode;

    private SMAuthInfo(
        @NotNull SMAuthStatus authStatus,
        @Nullable String error,
        @NotNull String authAttemptId,
        @NotNull Map<SMAuthConfigurationReference, Object> authData,
        @Nullable String smSignInLink,
        @Nullable String smSignOutLink,
        @Nullable String smAccessToken,
        @Nullable String smRefreshToken,
        @Nullable String authRole,
        @Nullable SMAuthPermissions authPermissions,
        boolean mainAuth,
        boolean forceSessionsLogout,
        @Nullable String errorCode
    ) {
        this.authStatus = authStatus;
        this.error = error;
        this.authAttemptId = authAttemptId;
        this.authData = authData;
        this.redirectUrl = smSignInLink;
        this.signOutLink = smSignOutLink;
        this.smAccessToken = smAccessToken;
        this.smRefreshToken = smRefreshToken;
        this.authRole = authRole;
        this.authPermissions = authPermissions;
        this.mainAuth = mainAuth;
        this.forceSessionsLogout = forceSessionsLogout;
        this.errorCode = errorCode;
    }

    public static SMAuthInfo expired(
        @NotNull String authAttemptId,
        @NotNull Map<SMAuthConfigurationReference, Object> authData,
        boolean mainAuth
    ) {
        return new Builder()
            .setAuthStatus(SMAuthStatus.EXPIRED)
            .setAuthAttemptId(authAttemptId)
            .setAuthData(authData)
            .setMainAuth(mainAuth)
            .build();
    }

    public static SMAuthInfo error(
        @NotNull String authAttemptId,
        @NotNull String error,
        boolean mainAuth,
        @Nullable String errorCode
    ) {
        return new Builder()
            .setAuthStatus(SMAuthStatus.ERROR)
            .setAuthAttemptId(authAttemptId)
            .setError(error)
            .setMainAuth(mainAuth)
            .setErrorCode(errorCode)
            .build();
    }

    public static SMAuthInfo inProgress(
        @NotNull String authAttemptId,
        @Nullable String signInLink,
        @Nullable String signOutLink,
        @NotNull Map<SMAuthConfigurationReference, Object> authData,
        boolean mainAuth,
        boolean forceSessionsLogout
    ) {
        return new Builder()
            .setAuthStatus(SMAuthStatus.IN_PROGRESS)
            .setAuthAttemptId(authAttemptId)
            .setSignInLink(signInLink)
            .setSignOutLink(signOutLink)
            .setAuthData(authData)
            .setMainAuth(mainAuth)
            .setForceSessionsLogout(forceSessionsLogout)
            .build();
    }

    public static SMAuthInfo successMainSession(
        @NotNull String authAttemptId,
        @NotNull String accessToken,
        @Nullable String refreshToken,
        @NotNull SMAuthPermissions smAuthPermissions,
        @NotNull Map<SMAuthConfigurationReference, Object> authData,
        @Nullable String authRole
    ) {
        return new Builder().setAuthStatus(SMAuthStatus.SUCCESS)
            .setAuthAttemptId(authAttemptId)
            .setSmAccessToken(accessToken)
            .setSmRefreshToken(refreshToken)
            .setAuthData(authData)
            .setAuthPermissions(smAuthPermissions)
            .setAuthRole(authRole)
            .setMainAuth(true)
            .build();
    }

    public static SMAuthInfo successChildSession(
        @NotNull String authAttemptId,
        SMAuthPermissions permissions,
        @NotNull Map<SMAuthConfigurationReference, Object> authData
    ) {
        return new Builder().setAuthStatus(SMAuthStatus.SUCCESS)
            .setAuthAttemptId(authAttemptId)
            .setAuthPermissions(permissions)
            .setAuthData(authData)
            .setMainAuth(false)
            .build();
    }


    @Nullable
    public String getSmAccessToken() {
        if (smAuthToken != null) {
            return smAuthToken;
        }
        return smAccessToken;
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
    public Map<SMAuthConfigurationReference, Object> getAuthData() {
        return authData;
    }

    @Nullable
    public String getRedirectUrl() {
        return redirectUrl;
    }

    @Nullable
    public String getSignInLink() {
        return redirectUrl;
    }

    @Nullable
    public String getSignOutLink() {
        return signOutLink;
    }

    @Nullable
    public String getError() {
        return error;
    }

    public boolean isMainAuth() {
        return mainAuth;
    }

    public boolean isForceSessionsLogout() {
        return forceSessionsLogout;
    }

    @Nullable
    public String getErrorCode() {
        return errorCode;
    }

    private static final class Builder {
        private SMAuthStatus authStatus;
        private String error;
        private String authAttemptId;
        private Map<SMAuthConfigurationReference, Object> authData;
        private String signInLink;
        private String signOutLink;
        private String smAccessToken;
        private String smRefreshToken;
        private String authRole;
        private SMAuthPermissions authPermissions;
        private boolean mainAuth;
        private boolean forceSessionsLogout;
        private String errorCode;

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

        public Builder setAuthData(Map<SMAuthConfigurationReference, Object> authData) {
            this.authData = authData;
            return this;
        }

        public Builder setSignInLink(String signInLink) {
            this.signInLink = signInLink;
            return this;
        }

        public Builder setSignOutLink(String signOutLink) {
            this.signOutLink = signOutLink;
            return this;
        }

        public Builder setSmAccessToken(String smAccessToken) {
            this.smAccessToken = smAccessToken;
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

        public Builder setMainAuth(boolean mainAuth) {
            this.mainAuth = mainAuth;
            return this;
        }

        public Builder setForceSessionsLogout(boolean forceSessionsLogout) {
            this.forceSessionsLogout = forceSessionsLogout;
            return this;
        }

        public Builder setErrorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public SMAuthInfo build() {
            return new SMAuthInfo(
                authStatus,
                error,
                authAttemptId,
                authData,
                signInLink,
                signOutLink,
                smAccessToken,
                smRefreshToken,
                authRole,
                authPermissions,
                mainAuth,
                forceSessionsLogout,
                errorCode
            );
        }
    }
}

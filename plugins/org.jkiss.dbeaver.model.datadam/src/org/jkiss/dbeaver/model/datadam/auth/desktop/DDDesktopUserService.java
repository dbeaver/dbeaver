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
package org.jkiss.dbeaver.model.datadam.auth.desktop;

import com.google.gson.JsonObject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;

import java.util.UUID;

class DDDesktopUserService {
    private static final Log log = Log.getLog(DDDesktopUserService.class);
    private final DDDesktopTokenManager tokenManager;
    private final DDAccountClient accountClient;

    DDDesktopUserService(@NotNull DDDesktopTokenManager tokenManager, @NotNull DDAccountClient accountClient) {
        this.tokenManager = tokenManager;
        this.accountClient = accountClient;
    }

    @NotNull
    DDLoggedUserView getLoggedUserView(@Nullable UUID accountId) {
        JsonObject claims = tokenManager.getUnverifiedTokenClaims();
        String userName = getStringClaim(claims, "email");
        String subject = getStringClaim(claims, "sub");
        UUID userId = null;
        if (subject != null) {
            try {
                userId = UUID.fromString(subject);
            } catch (IllegalArgumentException e) {
                log.debug("Invalid DataDam user ID in token", e);
            }
        }
        String accountName = null;
        if (accountId != null && tokenManager.getStatus() == DDDesktopTokenManager.Status.SIGNED_IN) {
            try {
                accountName = accountClient.getAccountName();
            } catch (Exception e) {
                log.debug("Could not read DataDam account name", e);
            }
        }
        return new DDLoggedUserView(userId, userName, accountId, accountName);
    }

    @Nullable
    private String getStringClaim(@Nullable JsonObject claims, @NotNull String name) {
        var claim = claims == null ? null : claims.get(name);
        return claim != null && claim.isJsonPrimitive() && claim.getAsJsonPrimitive().isString()
            ? claim.getAsString() : null;
    }
}

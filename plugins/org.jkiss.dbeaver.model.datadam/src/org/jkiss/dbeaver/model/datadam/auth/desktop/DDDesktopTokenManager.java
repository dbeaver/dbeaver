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

import com.dbeaver.datadam.sso.api.DDSsoConstants;
import com.dbeaver.datadam.sso.api.DDSsoScopes;
import com.google.gson.JsonObject;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoClient;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoLogin;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

/**
 * Internal owner of desktop tokens. Valid-token reads are not synchronized; expired-token refreshes are.
 * Reading status never starts a browser flow.
 */
class DDDesktopTokenManager {
    public enum Status { SIGNED_OUT, SIGNED_IN, INVALID_TOKEN }

    private static final Log log = Log.getLog(DDDesktopTokenManager.class);
    private static final String PREF_LOGGED_IN = "datadam.desktop.sso.logged-in";
    private static final DDDesktopTokenManager INSTANCE = new DDDesktopTokenManager(
        DDDesktopTokenStore.INSTANCE, new DDDesktopTokenValidator(), new DDDesktopSsoLogin());

    private final DDDesktopTokenStore store;
    private final DDDesktopTokenValidator validator;
    private final DDDesktopSsoLogin login;
    @Nullable
    private volatile DDDesktopUserToken token;

    DDDesktopTokenManager(
        @NotNull DDDesktopTokenStore store, @NotNull DDDesktopTokenValidator validator,
        @NotNull DDDesktopSsoLogin login
    ) {
        this.store = store;
        this.validator = validator;
        this.login = login;
    }

    @NotNull
    static DDDesktopTokenManager getInstance() {
        return INSTANCE;
    }

    @NotNull
    Status getStatus() {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_LOGGED_IN)) {
            return Status.SIGNED_OUT;
        }
        var current = token;
        return current == null || (validator.isExpired(current) && current.refreshToken() == null)
            ? Status.INVALID_TOKEN : Status.SIGNED_IN;
    }

    @Nullable
    Instant getExpiresAt() {
        DDDesktopUserToken current = token;
        return current == null ? null : validator.expiresAt(current);
    }

    /** Returns readable claims even for an expired token; these are not verified credentials. */
    @Nullable
    JsonObject getUnverifiedTokenClaims() {
        var current = token;
        return current == null ? null : JSONUtils.parseJwtClaims(current.accessToken());
    }

    synchronized void load(@NotNull URI resource) throws DBException {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_LOGGED_IN)) {
            return;
        }
        log.debug("Loading DataDam desktop token");
        DDDesktopUserToken saved = store.load();
        if (saved != null && (!validator.matchesResource(saved, resource) || !validator.matchesIssuer(saved))) {
            log.debug("Saved DataDam desktop token does not match the configured endpoints");
            saved = null;
        }
        if (saved == null) {
            signOut();
            return;
        }
        token = saved;
        log.debug("Loaded DataDam desktop token");
        refreshIfExpired();
    }

    @NotNull
    String getAccessTokenForResource(@NotNull URI resource) throws DBException {
        DDDesktopUserToken current = requireSignedInToken();
        if (!validator.matchesResource(current, resource)) {
            throw new DBException("DataDam token is not valid for this endpoint. Relogin to DataDam");
        }
        if (validator.isExpired(current)) {
            current = refreshIfExpired();
        }
        return current.accessToken();
    }

    @NotNull
    private synchronized DDDesktopUserToken refreshIfExpired() throws DBException {
        DDDesktopUserToken current = requireSignedInToken();
        return validator.isExpired(current) ? refresh(current) : current;
    }

    @NotNull
    private DDDesktopUserToken requireSignedInToken() throws DBException {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_LOGGED_IN)) {
            throw new DBException("Sign in to DataDam first");
        }
        DDDesktopUserToken current = token;
        if (current == null) {
            throw new DBException("DataDam authorization is invalid. Sign in again");
        }
        return current;
    }

    @NotNull
    private DDDesktopUserToken refresh(@NotNull DDDesktopUserToken current) throws DBException {
        String refreshToken = current.refreshToken();
        if (refreshToken == null) {
            throw new DBException("DataDam authorization cannot be refreshed. Sign in again");
        }
        log.debug("Refreshing DataDam desktop token");
        try (DDDesktopSsoClient client = new DDDesktopSsoClient(URI.create(DDEndpoints.getSsoBaseUrl()))) {
            var response = client.refresh(refreshToken, DDSsoScopes.format(current.scopes()));
            DDDesktopUserToken rotated = new DDDesktopUserToken(
                response.accessToken(), validator.getIssuer(response.accessToken()), current.endpoints(), current.scopes(), response.refreshToken());
            if (!validator.matchesIssuer(rotated)) {
                throw new DBException("Refreshed DataDam token has an unexpected issuer");
            }
            if (validator.isExpired(rotated)) {
                throw new DBException("Refreshed DataDam token has expired");
            }
            saveToken(rotated);
            return rotated;
        } catch (Exception e) {
            DDDesktopUserToken spent = new DDDesktopUserToken(
                current.accessToken(), current.issuer(), current.endpoints(), current.scopes());
            try {
                saveToken(spent);
            } catch (DBException saveError) {
                token = null;
                log.warn("Could not save DataDam refresh failure");
            }
            throw new DBException("DataDam token refresh failed. Sign in again");
        }
    }

    void signIn(@NotNull URI accountUrl, @NotNull URI storageUrl, @NotNull DBRProgressMonitor monitor)
    throws DBException, InterruptedException {
        signIn(accountUrl, storageUrl, monitor, DDDesktopSsoLogin::openBrowser);
    }

    private void signIn(
        @NotNull URI accountUrl, @NotNull URI storageUrl, @NotNull DBRProgressMonitor monitor,
        @NotNull DDDesktopSsoLogin.BrowserOpener browser
    ) throws DBException, InterruptedException {
        log.debug("Signing in to DataDam desktop access");
        var result = login.login(URI.create(DDEndpoints.getSsoBaseUrl()), null,
            Set.of(DDSsoConstants.SCOPE_ACCOUNT_FULL_ACCESS, DDSsoConstants.SCOPE_DBEAVER_STORAGE_FULL_ACCESS,
                DDSsoConstants.SCOPE_MCP_CONNECT), monitor, browser);
        DDDesktopUserToken saved = new DDDesktopUserToken(
            result.accessToken(), validator.getIssuer(result.accessToken()), Set.of(accountUrl.toString(), storageUrl.toString()),
            DDSsoScopes.parse(result.scope()), result.refreshToken()
        );
        if (!validator.matchesIssuer(saved)) {
            throw new DBException("DataDam desktop token has an unexpected issuer");
        }
        if (validator.isExpired(saved)) {
            throw new DBException("DataDam desktop token has expired");
        }
        saveToken(saved);
    }

    private synchronized void saveToken(@NotNull DDDesktopUserToken saved) throws DBException {
        store.save(saved);
        token = saved;
        log.debug("Stored DataDam desktop token");
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_LOGGED_IN, true);
    }

    synchronized void signOut() throws DBException {
        log.debug("Signing out of DataDam desktop access");
        DDDesktopUserToken previous = token;
        try {
            DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_LOGGED_IN, false);
            token = null;
            store.clear();
            log.debug("Cleared DataDam desktop token");
        } finally {
            revoke(previous);
        }
    }

    private void revoke(@Nullable DDDesktopUserToken previous) {
        try {
            if (previous == null) {
                return;
            }
            String refreshToken = previous.refreshToken();
            if (refreshToken == null) {
                return;
            }
            if (!DDEndpoints.getSsoBaseUrl().equals(previous.issuer())) {
                return;
            }
            Job.createSystem(
                "Revoke DataDam desktop token", monitor -> {
                    try (DDDesktopSsoClient client = new DDDesktopSsoClient(URI.create(DDEndpoints.getSsoBaseUrl()))) {
                        client.revoke(refreshToken);
                    } catch (Exception e) {
                        log.warn("DataDam credentials cleared locally, but server revocation could not be confirmed", e);
                    }
                }
            ).schedule();
        } catch (Exception e) {
            log.warn("Could not revoke DataDam desktop token", e);
        }
    }
}

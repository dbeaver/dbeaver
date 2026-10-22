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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoClient;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoLogin;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.net.URI;

/**
 * Owns the desktop access token. Reading status or attaching a token never starts a browser flow.
 */
class DDDesktopTokenManager {
    public enum Status { SIGNED_OUT, UNLOADED, SIGNED_IN, EXPIRED }

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
    public static DDDesktopTokenManager getInstance() {
        return INSTANCE;
    }

    @NotNull
    public Status getStatus() {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_LOGGED_IN)) {
            return Status.SIGNED_OUT;
        }
        DDDesktopUserToken current = token;
        if (current == null) {
            return Status.UNLOADED;
        }
        return validator.isExpired(current) ? Status.EXPIRED : Status.SIGNED_IN;
    }

    /** Returns readable claims even for an expired token; these are not verified credentials. */
    @Nullable
    JsonObject getUnverifiedTokenClaims() {
        DDDesktopUserToken current = token;
        return current == null ? null : JSONUtils.parseJwtClaims(current.accessToken());
    }

    public void load(@NotNull URI resource) throws DBException {
        if (getStatus() != Status.UNLOADED) {
            return;
        }
        DDDesktopUserToken saved = store.load();
        if (saved != null && !validator.matchesResource(saved, resource)) {
            log.debug("Saved DataDam desktop token does not match the requested resource");
            saved = null;
        }
        synchronized (this) {
            if (getStatus() == Status.UNLOADED) {
                token = saved;
            }
        }
        if (getStatus() == Status.UNLOADED) {
            signOut();
        } else {
            log.debug("Loaded DataDam desktop token");
        }
    }

    @NotNull
    public String getAccessTokenForResource(@NotNull URI resource) throws DBException {
        DDDesktopUserToken current = token;
        if (current == null || validator.isExpired(current)
            || !DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_LOGGED_IN)) {
            throw new DBException("Sign in to DataDam first");
        }
        if (!validator.matchesResource(current, resource)) {
            throw new DBException("DataDam token is not valid for this endpoint. Relogin to DataDam");
        }
        return current.accessToken();
    }

    public void signIn(@NotNull URI accountUrl, @NotNull URI storageUrl, @NotNull DBRProgressMonitor monitor)
    throws DBException, InterruptedException {
        signIn(accountUrl, storageUrl, monitor, DDDesktopSsoLogin::openBrowser);
    }

    private void signIn(
        @NotNull URI accountUrl, @NotNull URI storageUrl, @NotNull DBRProgressMonitor monitor,
        @NotNull DDDesktopSsoLogin.BrowserOpener browser
    ) throws DBException, InterruptedException {
        URI issuer = discoverIssuer(accountUrl);
        var result = login.login(issuer, monitor, browser);
        DDDesktopUserToken saved = new DDDesktopUserToken(
            result.accessToken(), issuer.toString(), accountUrl.toString(), storageUrl.toString());
        if (validator.isExpired(saved)) {
            throw new DBException("DataDam desktop token has expired");
        }
        synchronized (this) {
            store.save(saved);
            token = saved;
            DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_LOGGED_IN, true);
        }
        log.debug("Stored DataDam desktop token");
    }

    public void signOut() throws DBException {
        synchronized (this) {
            token = null;
            store.clear();
            DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_LOGGED_IN, false);
        }
        log.debug("Cleared DataDam desktop token");
    }

    @NotNull
    private URI discoverIssuer(@NotNull URI accountUrl) throws DBException {
        try (DDDesktopSsoClient client = new DDDesktopSsoClient(accountUrl)) {
            return client.discover();
        }
    }
}

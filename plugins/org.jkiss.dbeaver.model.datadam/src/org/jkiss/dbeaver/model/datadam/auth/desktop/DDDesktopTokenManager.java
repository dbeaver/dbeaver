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
import com.dbeaver.datadam.sso.api.model.DDSsoConfigResponse;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Owns resource-specific desktop access tokens. Reading status never starts a browser flow.
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
    private volatile Map<String, DDDesktopUserToken> tokens;
    @Nullable
    private volatile String primaryTokenKey;

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
        var current = tokens;
        if (current == null) {
            return Status.UNLOADED;
        }
        String key = primaryTokenKey;
        DDDesktopUserToken primary = key == null ? null : current.get(key);
        return primary == null ? Status.SIGNED_OUT : validator.isExpired(primary) ? Status.EXPIRED : Status.SIGNED_IN;
    }

    /** Returns readable claims even for an expired token; these are not verified credentials. */
    @Nullable
    JsonObject getUnverifiedTokenClaims() {
        var current = tokens;
        String key = primaryTokenKey;
        if (current == null || key == null) {
            return null;
        }
        DDDesktopUserToken primary = current.get(key);
        return primary == null ? null : JSONUtils.parseJwtClaims(primary.accessToken());
    }

    public void load(@NotNull URI resource) throws DBException {
        if (getStatus() != Status.UNLOADED) {
            return;
        }
        var saved = store.load().stream().collect(Collectors.toMap(DDDesktopUserToken::key, Function.identity()));
        synchronized (this) {
            if (getStatus() == Status.UNLOADED) {
                primaryTokenKey = saved.values().stream().filter(token -> validator.matchesResource(token, resource))
                    .findFirst().map(DDDesktopUserToken::key).orElse(null);
                tokens = Map.copyOf(saved);
            }
        }
        if (saved.isEmpty()) {
            signOut();
        } else {
            log.debug("Loaded DataDam desktop token");
        }
    }

    @NotNull
    public String getAccessTokenForResource(@NotNull URI resource) throws DBException {
        var current = tokens;
        if (current == null
            || !DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_LOGGED_IN)) {
            throw new DBException("Sign in to DataDam first");
        }
        var candidates = current.values().stream()
            .filter(token -> !validator.isExpired(token) && validator.matchesResource(token, resource)).toList();
        if (candidates.size() != 1) {
            throw new DBException("DataDam token is not valid for this endpoint. Relogin to DataDam");
        }
        return candidates.getFirst().accessToken();
    }

    public void signIn(@NotNull URI accountUrl, @NotNull URI storageUrl, @NotNull DBRProgressMonitor monitor)
    throws DBException, InterruptedException {
        signIn(accountUrl, storageUrl, monitor, DDDesktopSsoLogin::openBrowser);
    }

    private void signIn(
        @NotNull URI accountUrl, @NotNull URI storageUrl, @NotNull DBRProgressMonitor monitor,
        @NotNull DDDesktopSsoLogin.BrowserOpener browser
    ) throws DBException, InterruptedException {
        var config = discover(accountUrl);
        var saved = signInForResource(URI.create(config.issuer()), null,
            Set.of(DDSsoConstants.SCOPE_ACCOUNT_ACCESS, DDSsoConstants.SCOPE_DBEAVER_STORAGE_ACCESS),
            Set.of(accountUrl.toString(), storageUrl.toString()), monitor, browser);
        primaryTokenKey = saved.key();
    }

    @NotNull
    public DDDesktopUserToken signInForResource(
        @NotNull URI issuer, @Nullable URI resource, @NotNull Set<String> scopes, @NotNull Set<String> endpoints,
        @NotNull DBRProgressMonitor monitor, @NotNull DDDesktopSsoLogin.BrowserOpener browser
    ) throws DBException, InterruptedException {
        var result = login.login(issuer, resource, scopes, monitor, browser);
        DDDesktopUserToken saved = new DDDesktopUserToken(
            result.accessToken(), issuer.toString(), resource == null ? null : resource.toString(), endpoints, DDSsoScopes.parse(result.scope()));
        if (validator.isExpired(saved)) {
            throw new DBException("DataDam desktop token has expired");
        }
        synchronized (this) {
            Map<String, DDDesktopUserToken> updated = tokens == null
                ? store.load().stream().collect(Collectors.toMap(DDDesktopUserToken::key, Function.identity(), (a, b) -> b, HashMap::new))
                : new HashMap<>(tokens);
            updated.put(saved.key(), saved);
            store.save(updated.values());
            tokens = Map.copyOf(updated);
            DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_LOGGED_IN, true);
        }
        log.debug("Stored DataDam desktop token");
        return saved;
    }

    public void signOut() throws DBException {
        synchronized (this) {
            tokens = null;
            primaryTokenKey = null;
            store.clear();
            DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_LOGGED_IN, false);
        }
        log.debug("Cleared DataDam desktop token");
    }

    @NotNull
    private DDSsoConfigResponse discover(@NotNull URI accountUrl) throws DBException {
        try (DDDesktopSsoClient client = new DDDesktopSsoClient(accountUrl)) {
            return client.discover();
        }
    }
}

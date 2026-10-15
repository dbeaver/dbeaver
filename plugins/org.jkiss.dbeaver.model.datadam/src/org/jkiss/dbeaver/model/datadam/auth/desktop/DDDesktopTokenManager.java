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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoClient;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoLogin;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.URI;

/**
 * Owns the desktop access token. Reading status or attaching a token never starts a browser flow.
 */
public class DDDesktopTokenManager {
    public enum Status { SIGNED_OUT, SIGNED_IN, EXPIRED }

    private static final DDDesktopTokenManager INSTANCE = new DDDesktopTokenManager(
        DDDesktopTokenStore.INSTANCE, new DDDesktopTokenValidator(), new DDDesktopSsoLogin());

    private final DDDesktopTokenStore store;
    private final DDDesktopTokenValidator validator;
    private final DDDesktopSsoLogin login;
    @Nullable
    private DDDesktopUserToken token;

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
        if (token == null) {
            return Status.SIGNED_OUT;
        }
        return validator.isExpired(token) ? Status.EXPIRED : Status.SIGNED_IN;
    }

    public void restore(@NotNull URI accountUrl, @NotNull URI storageUrl) throws DBException {
        DDDesktopUserToken saved = store.load();
        token = saved != null && accountUrl.toString().equals(saved.accountOrigin())
            && storageUrl.toString().equals(saved.storageOrigin()) ? saved : null;
    }

    @NotNull
    public String accessFor(@NotNull URI resource) throws DBException {
        DDDesktopUserToken current = token;
        if (current == null || validator.isExpired(current)) {
            throw new DBException("Sign in to DataDam first");
        }
        if (!validator.matchesResource(current, resource)) {
            throw new DBException("DataDam token is not valid for this endpoint");
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
        store.save(saved);
        token = saved;
    }

    public void signOut() throws DBException {
        token = null;
        store.clear();
    }

    @NotNull
    private URI discoverIssuer(@NotNull URI accountUrl) throws DBException {
        try (DDDesktopSsoClient client = new DDDesktopSsoClient(accountUrl)) {
            return client.discover();
        }
    }
}

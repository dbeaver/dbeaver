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
package org.jkiss.dbeaver.model.datadam.auth;

import com.dbeaver.datadam.sso.api.model.DDSsoDesktopTokenResponse;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

/** A desktop access token is kept only until the application exits or the user signs out. */
public final class DDDesktopSsoSession {
    @Nullable
    private static Session session;
    private static long loginAttempt;

    private DDDesktopSsoSession() {
    }

    public static void login(@NotNull URI accountUrl, @NotNull URI storageUrl,
        @NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        login(accountUrl, storageUrl, monitor, DDDesktopSsoLogin::openBrowser);
    }

    public static void login(
        @NotNull URI accountUrl,
        @NotNull URI storageUrl,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DDDesktopSsoLogin.BrowserOpener browser
    ) throws DBException, InterruptedException {
        DDDesktopSsoClient.checkOrigin(accountUrl);
        URI accountOrigin = origin(accountUrl);
        URI storageOrigin = origin(storageUrl);
        long attempt;
        synchronized (DDDesktopSsoSession.class) {
            attempt = ++loginAttempt;
        }
        URI issuer = DDDesktopSsoClient.discover(accountUrl);
        DDSsoDesktopTokenResponse tokens = new DDDesktopSsoLogin().login(issuer, monitor, browser);
        synchronized (DDDesktopSsoSession.class) {
            // A browser callback must not restore a session after logout or supersede a newer login.
            if (attempt != loginAttempt || monitor.isCanceled()) {
                throw new InterruptedException("DataDam sign-in was canceled");
            }
            session = new Session(accountOrigin, storageOrigin, tokens.accessToken(), Instant.now().plusSeconds(tokens.expiresIn()));
        }
    }

    public static synchronized boolean hasLogin() {
        return session != null;
    }

    public static synchronized boolean needsLogin() {
        return session == null || !session.expiresAt().isAfter(Instant.now().plusSeconds(30));
    }

    @NotNull
    public static synchronized String accessFor(@NotNull URI resource) throws DBException {
        if (session == null) {
            throw new DBException("Sign in to DataDam first");
        }
        URI resourceOrigin = origin(resource);
        if (!session.accountOrigin().equals(resourceOrigin) && !session.storageOrigin().equals(resourceOrigin)) {
            throw new DBException("DataDam token is not valid for this endpoint");
        }
        if (!session.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
            throw new DBException("DataDam session has expired. Sign in again");
        }
        return session.accessToken();
    }

    public static synchronized void logout() {
        loginAttempt++;
        session = null;
    }

    @NotNull
    private static URI origin(@NotNull URI uri) throws DBException {
        try {
            int port = uri.getPort();
            if (port == 443 && "https".equals(uri.getScheme()) || port == 80 && "http".equals(uri.getScheme())) {
                port = -1;
            }
            URI origin = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), port, null, null, null);
            DDDesktopSsoClient.checkOrigin(origin);
            return origin;
        } catch (URISyntaxException e) {
            throw new DBException("Invalid DataDam URL", e);
        }
    }

    private record Session(
        @NotNull URI accountOrigin,
        @NotNull URI storageOrigin,
        @NotNull String accessToken,
        @NotNull Instant expiresAt
    ) {
        @Override
        @NotNull
        public String toString() {
            return "DesktopSsoSession[storageOrigin=" + storageOrigin + "]";
        }
    }
}

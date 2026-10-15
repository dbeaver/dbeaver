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
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.URI;

/** Coordinates the desktop login and the locally imported project keys. */
public class DDDesktopAccessService {
    public enum Status { SIGNED_OUT, TOKEN_ONLY, PROJECT_READY }

    private static final DDDesktopAccessService INSTANCE = new DDDesktopAccessService(DDDesktopTokenManager.getInstance());

    private final DDDesktopTokenManager tokenManager;
    private final URI accountUrl = URI.create(DDEndpoints.getAccountBaseUrl());
    private final URI storageUrl = URI.create(DDEndpoints.getStorageBaseUrl());
    @Nullable
    private volatile DDKeyBundle keyBundle;

    DDDesktopAccessService(@NotNull DDDesktopTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @NotNull
    public static DDDesktopAccessService getInstance() {
        return INSTANCE;
    }

    @NotNull
    public Status getStatus() {
        if (tokenManager.getStatus() != DDDesktopTokenManager.Status.SIGNED_IN) {
            return Status.SIGNED_OUT;
        }
        return keyBundle == null ? Status.TOKEN_ONLY : Status.PROJECT_READY;
    }

    @Nullable
    public DDKeyBundle getKeyBundle() {
        return getStatus() == Status.PROJECT_READY ? keyBundle : null;
    }

    public void load() throws DBException {
        tokenManager.load(storageUrl);
        keyBundle = tokenManager.getStatus() == DDDesktopTokenManager.Status.SIGNED_IN ? DDKeyStore.load() : null;
    }

    public void signIn(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        tokenManager.signIn(accountUrl, storageUrl, monitor);
        keyBundle = DDKeyStore.load();
    }

    public void importKeys(@NotNull String recoveryPhrase) throws DBException {
        if (tokenManager.getStatus() != DDDesktopTokenManager.Status.SIGNED_IN) {
            throw new DBException("Sign in to DataDam before importing keys");
        }
        var state = new DDAccountClient(accountUrl).getCryptoState();
        DDKeyBundle bundle = DDKeyStore.unpack(state, recoveryPhrase);
        DDKeyStore.save(bundle);
        keyBundle = bundle;
    }

    public void forgetKeys() throws DBException {
        DDKeyStore.clear();
        keyBundle = null;
    }

    public void signOut() throws DBException {
        DDKeyStore.clear();
        keyBundle = null;
        tokenManager.signOut();
    }
}

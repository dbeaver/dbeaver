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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Coordinates the desktop login and the locally imported project keys. */
public class DDDesktopAccessService {
    public enum Status { SIGNED_OUT, EXPIRED, TOKEN_ONLY, PROJECT_SYNC }

    private static final DDDesktopAccessService INSTANCE = new DDDesktopAccessService(DDDesktopTokenManager.getInstance());

    private final DDDesktopTokenManager tokenManager;
    private final List<Runnable> keyBundleListeners = new CopyOnWriteArrayList<>();
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
        DDDesktopTokenManager.Status tokenStatus = tokenManager.getStatus();
        if (tokenStatus == DDDesktopTokenManager.Status.SIGNED_OUT) {
            return Status.SIGNED_OUT;
        }
        if (tokenStatus == DDDesktopTokenManager.Status.EXPIRED) {
            return Status.EXPIRED;
        }
        return keyBundle == null ? Status.TOKEN_ONLY : Status.PROJECT_SYNC;
    }

    @Nullable
    public DDKeyBundle getKeyBundle() {
        return getStatus() != Status.SIGNED_OUT ? keyBundle : null;
    }

    public void load() throws DBException {
        tokenManager.load(storageUrl);
        setKeyBundle(tokenManager.getStatus() == DDDesktopTokenManager.Status.SIGNED_IN ? DDKeyStore.load() : null);
    }

    public void signIn(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        tokenManager.signIn(accountUrl, storageUrl, monitor);
        setKeyBundle(DDKeyStore.load());
    }

    public void importKeys(@NotNull String recoveryPhrase) throws DBException {
        if (tokenManager.getStatus() != DDDesktopTokenManager.Status.SIGNED_IN) {
            throw new DBException("Sign in to DataDam before importing keys");
        }
        var state = new DDAccountClient(accountUrl).getCryptoState();
        DDKeyBundle bundle = DDKeyStore.unpack(state, recoveryPhrase);
        DDKeyStore.save(bundle);
        setKeyBundle(bundle);
    }

    public void forgetKeys() throws DBException {
        DDKeyStore.clear();
        setKeyBundle(null);
    }

    public void signOut() throws DBException {
        DDKeyStore.clear();
        setKeyBundle(null);
        tokenManager.signOut();
    }

    public void addKeyBundleListener(@NotNull Runnable listener) {
        keyBundleListeners.add(listener);
    }

    public void removeKeyBundleListener(@NotNull Runnable listener) {
        keyBundleListeners.remove(listener);
    }

    private void setKeyBundle(@Nullable DDKeyBundle bundle) {
        if (!Objects.equals(keyBundle, bundle)) {
            keyBundle = bundle;
            for (Runnable listener : keyBundleListeners) {
                listener.run();
            }
        }
    }
}

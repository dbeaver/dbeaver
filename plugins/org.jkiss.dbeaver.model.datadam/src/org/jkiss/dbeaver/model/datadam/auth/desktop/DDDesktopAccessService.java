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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/** Coordinates the desktop login and the locally imported project keys. */
public class DDDesktopAccessService {
    public enum Status { SIGNED_IN, SIGNED_OUT, EXPIRED, UNLOADED }

    private static final Log log = Log.getLog(DDDesktopAccessService.class);
    private static final DDDesktopAccessService INSTANCE = new DDDesktopAccessService(DDDesktopTokenManager.getInstance());

    private final DDDesktopTokenManager tokenManager;
    private final ReentrantLock operationLock = new ReentrantLock();
    private final List<Runnable> keyBundleListeners = new CopyOnWriteArrayList<>();
    private final URI accountUrl = URI.create(DDEndpoints.getAccountBaseUrl());
    private final URI storageUrl = URI.create(DDEndpoints.getStorageBaseUrl());
    private final DDAccountClient accountClient = new DDAccountClient(accountUrl);
    private final DDDesktopUserService userService;
    @Nullable
    private volatile DDKeyBundle keyBundle;

    DDDesktopAccessService(@NotNull DDDesktopTokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.userService = new DDDesktopUserService(tokenManager, accountClient);
    }

    @NotNull
    public static DDDesktopAccessService getInstance() {
        return INSTANCE;
    }

    @NotNull
    public Status getStatus() {
        return switch (tokenManager.getStatus()) {
            case SIGNED_IN -> Status.SIGNED_IN;
            case SIGNED_OUT -> Status.SIGNED_OUT;
            case EXPIRED -> Status.EXPIRED;
            case UNLOADED -> Status.UNLOADED;
        };
    }

    @Nullable
    public DDKeyBundle getKeyBundle() {
        if (!operationLock.tryLock()) {
            return null;
        }
        try {
            return getStatus() != Status.SIGNED_OUT ? keyBundle : null;
        } finally {
            operationLock.unlock();
        }
    }

    @NotNull
    public String getAccessTokenForResource(@NotNull URI resource) throws DBException {
        if (!operationLock.tryLock()) {
            throw new DBException("DataDam access operation is in progress");
        }
        try {
            return tokenManager.getAccessTokenForResource(resource);
        } finally {
            operationLock.unlock();
        }
    }

    /** Reads display information, including an Account request when keys and a valid token are available. */
    @NotNull
    public DDLoggedUserView getLoggedUserView() {
        DDKeyBundle bundle = getKeyBundle();
        return userService.getLoggedUserView(bundle == null ? null : bundle.accountId());
    }

    public void load() throws DBException {
        operationLock.lock();
        try {
            if (tokenManager.getStatus() != DDDesktopTokenManager.Status.UNLOADED) {
                return;
            }
            log.debug("Loading DataDam desktop access");
            tokenManager.load(storageUrl);
            updateKeyBundle(tokenManager.getStatus() == DDDesktopTokenManager.Status.SIGNED_IN ? DDKeyStore.load() : null);
        } finally {
            operationLock.unlock();
        }
    }

    public void signIn(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        operationLock.lock();
        try {
            tokenManager.signIn(accountUrl, storageUrl, monitor);
            setKeyBundle(null);
            updateKeyBundle(DDKeyStore.load());
            log.debug("Signed in to DataDam desktop access");
        } finally {
            operationLock.unlock();
        }
    }

    public void importKeys(@NotNull String recoveryPhrase) throws DBException {
        operationLock.lock();
        try {
            if (tokenManager.getStatus() != DDDesktopTokenManager.Status.SIGNED_IN) {
                throw new DBException("Sign in to DataDam before importing keys");
            }
            var state = accountClient.getCryptoState();
            DDKeyBundle bundle = DDKeyStore.unpack(state, recoveryPhrase);
            DDKeyStore.save(bundle);
            setKeyBundle(bundle);
            log.debug("Imported DataDam encryption keys");
        } finally {
            operationLock.unlock();
        }
    }

    public void forgetKeys() throws DBException {
        operationLock.lock();
        try {
            DDKeyStore.clear();
            setKeyBundle(null);
            log.debug("Forgot DataDam encryption keys");
        } finally {
            operationLock.unlock();
        }
    }

    public void signOut() throws DBException {
        operationLock.lock();
        try {
            tokenManager.signOut();
            setKeyBundle(null);
            DDKeyStore.clear();
            log.debug("Signed out of DataDam desktop access");
        } finally {
            operationLock.unlock();
        }
    }

    public void addKeyBundleListener(@NotNull Runnable listener) {
        keyBundleListeners.add(listener);
    }

    public void removeKeyBundleListener(@NotNull Runnable listener) {
        keyBundleListeners.remove(listener);
    }

    private void updateKeyBundle(@Nullable DDKeyBundle bundle) throws DBException {
        if (bundle != null && !bundle.accountId().equals(accountClient.getAccountId())) {
            log.debug("Stored DataDam encryption keys belong to another account");
            setKeyBundle(null);
        } else {
            setKeyBundle(bundle);
        }
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

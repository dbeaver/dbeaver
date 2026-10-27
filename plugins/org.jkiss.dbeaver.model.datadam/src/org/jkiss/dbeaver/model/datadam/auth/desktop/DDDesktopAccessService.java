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

import org.eclipse.core.runtime.jobs.Job;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/** Coordinates the desktop login and the locally imported project keys. */
public class DDDesktopAccessService {
    public enum Status { SIGNED_IN, SIGNED_OUT, EXPIRED, UNLOADED }

    private static final Log log = Log.getLog(DDDesktopAccessService.class);
    private static final DDDesktopAccessService INSTANCE = new DDDesktopAccessService(DDDesktopTokenManager.getInstance());

    private final DDDesktopTokenManager tokenManager;
    private final ReentrantReadWriteLock operationLock = new ReentrantReadWriteLock();
    private final List<Runnable> keyBundleListeners = new CopyOnWriteArrayList<>();
    private final List<DDDesktopAccessListener> accessListeners = new CopyOnWriteArrayList<>();
    private final URI accountUrl = URI.create(DDEndpoints.getAccountBaseUrl());
    private final URI storageUrl = URI.create(DDEndpoints.getStorageBaseUrl());
    private final DDAccountClient accountClient = new DDAccountClient(accountUrl);
    private final DDDesktopUserService userService;
    @Nullable
    private volatile DDKeyBundle keyBundle;
    @Nullable
    private Job expiryJob;
    private long expiryVersion;

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
        if (!operationLock.readLock().tryLock()) {
            return null;
        }
        try {
            return getStatus() != Status.SIGNED_OUT ? keyBundle : null;
        } finally {
            operationLock.readLock().unlock();
        }
    }

    @NotNull
    public String getAccessTokenForResource(@NotNull URI resource) throws DBException {
        if (!operationLock.readLock().tryLock()) {
            throw new DBException("DataDam access operation is in progress");
        }
        try {
            return tokenManager.getAccessTokenForResource(resource);
        } finally {
            operationLock.readLock().unlock();
        }
    }

    /** Reads display information, including an Account request when keys and a valid token are available. */
    @NotNull
    public DDLoggedUserView getLoggedUserView() {
        DDKeyBundle bundle = getKeyBundle();
        return userService.getLoggedUserView(bundle == null ? null : bundle.accountId());
    }

    public void load() throws DBException {
        operationLock.writeLock().lock();
        try {
            if (tokenManager.getStatus() != DDDesktopTokenManager.Status.UNLOADED) {
                return;
            }
            log.debug("Loading DataDam desktop access");
            notifyAccessListeners(DDDesktopAccessListener::accessChanging);
            tokenManager.load(storageUrl);
            updateKeyBundle(tokenManager.getStatus() == DDDesktopTokenManager.Status.SIGNED_IN ? DDKeyStore.load() : null);
        } finally {
            finishAccessChange();
        }
    }

    public void signIn(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        operationLock.writeLock().lock();
        try {
            notifyAccessListeners(DDDesktopAccessListener::accessChanging);
            tokenManager.signIn(accountUrl, storageUrl, monitor);
            setKeyBundle(null);
            updateKeyBundle(DDKeyStore.load());
            log.debug("Signed in to DataDam desktop access");
        } finally {
            finishAccessChange();
        }
    }

    public void importKeys(@NotNull String recoveryPhrase) throws DBException {
        operationLock.writeLock().lock();
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
            operationLock.writeLock().unlock();
        }
    }

    public void forgetKeys() throws DBException {
        operationLock.writeLock().lock();
        try {
            DDKeyStore.clear();
            setKeyBundle(null);
            log.debug("Forgot DataDam encryption keys");
        } finally {
            operationLock.writeLock().unlock();
        }
    }

    public void signOut() throws DBException {
        operationLock.writeLock().lock();
        try {
            notifyAccessListeners(DDDesktopAccessListener::accessChanging);
            tokenManager.signOut();
            setKeyBundle(null);
            DDKeyStore.clear();
            log.debug("Signed out of DataDam desktop access");
        } finally {
            finishAccessChange();
        }
    }

    public void addAccessListener(@NotNull DDDesktopAccessListener listener) {
        accessListeners.add(listener);
    }

    public void removeAccessListener(@NotNull DDDesktopAccessListener listener) {
        accessListeners.remove(listener);
    }

    public void addKeyBundleListener(@NotNull Runnable listener) {
        keyBundleListeners.add(listener);
    }

    public void removeKeyBundleListener(@NotNull Runnable listener) {
        keyBundleListeners.remove(listener);
    }

    private void finishAccessChange() {
        try {
            scheduleExpiry();
        } finally {
            try {
                notifyAccessListeners(DDDesktopAccessListener::accessChanged);
            } finally {
                operationLock.writeLock().unlock();
            }
        }
    }

    private void scheduleExpiry() {
        long version = ++expiryVersion;
        if (expiryJob != null) {
            expiryJob.cancel();
            expiryJob = null;
        }
        if (tokenManager.getStatus() != DDDesktopTokenManager.Status.SIGNED_IN) {
            return;
        }
        Instant expiresAt = tokenManager.getExpiresAt();
        if (expiresAt != null) {
            Job job = Job.createSystem("Expire DataDam desktop access", monitor -> {
                expireAccess(version);
            });
            expiryJob = job;
            job.schedule(Math.max(1, Duration.between(Instant.now(), expiresAt).toMillis()));
        }
    }

    private void expireAccess(long version) {
        operationLock.writeLock().lock();
        try {
            if (version != expiryVersion) {
                return;
            }
            expiryJob = null;
            if (tokenManager.getStatus() == DDDesktopTokenManager.Status.EXPIRED) {
                notifyAccessListeners(DDDesktopAccessListener::accessChanged);
            } else {
                scheduleExpiry();
            }
        } finally {
            operationLock.writeLock().unlock();
        }
    }

    private void notifyAccessListeners(@NotNull Consumer<DDDesktopAccessListener> event) {
        for (DDDesktopAccessListener listener : accessListeners) {
            try {
                event.accept(listener);
            } catch (RuntimeException e) {
                log.warn("DataDam desktop access listener failed", e);
            }
        }
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

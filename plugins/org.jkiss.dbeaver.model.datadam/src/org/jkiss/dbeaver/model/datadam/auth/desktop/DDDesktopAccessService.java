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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/** Coordinates the desktop login and the locally imported project keys. */
public class DDDesktopAccessService {
    public enum Status { SIGNED_IN, SIGNED_OUT, INVALID_TOKEN, UNLOADED }

    private static final Log log = Log.getLog(DDDesktopAccessService.class);
    private static final long MIN_RETRY_MILLIS = 1_000;
    private static final long MAX_RETRY_MILLIS = 60_000;
    private static final DDDesktopAccessService INSTANCE = new DDDesktopAccessService(DDDesktopTokenManager.getInstance());

    private final DDDesktopTokenManager tokenManager;
    private final ReentrantReadWriteLock operationLock = new ReentrantReadWriteLock();
    private final List<Runnable> keyBundleListeners = new CopyOnWriteArrayList<>();
    private final List<DDDesktopAccessListener> accessListeners = new CopyOnWriteArrayList<>();
    private final URI accountUrl = URI.create(DDEndpoints.getAccountBaseUrl());
    private final URI storageUrl = URI.create(DDEndpoints.getStorageBaseUrl());
    private final DDAccountClient accountClient = new DDAccountClient(accountUrl, this);
    private final DDDesktopUserService userService;
    private volatile boolean loaded;
    private boolean initializationComplete;
    private Instant initializationRetryAt = Instant.EPOCH;
    private long retryMillis = MIN_RETRY_MILLIS;
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
        if (!loaded) {
            return Status.UNLOADED;
        }
        return switch (tokenManager.getStatus()) {
            case SIGNED_IN -> Status.SIGNED_IN;
            case SIGNED_OUT -> Status.SIGNED_OUT;
            case INVALID_TOKEN -> Status.INVALID_TOKEN;
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

    @Nullable
    public UUID getAccountId() {
        DDKeyBundle bundle = getKeyBundle();
        return bundle == null ? null : bundle.accountId();
    }

    @NotNull
    public String getAccessTokenForResource(@NotNull URI resource) throws DBException {
        try {
            operationLock.readLock().lock();
            try {
                return tokenManager.getAccessTokenForResource(resource);
            } finally {
                operationLock.readLock().unlock();
            }
        } catch (DBException e) {
            if (tokenManager.getStatus() == DDDesktopTokenManager.Status.INVALID_TOKEN) {
                operationLock.writeLock().lock();
                try {
                    if (tokenManager.getStatus() == DDDesktopTokenManager.Status.INVALID_TOKEN) {
                        setKeyBundle(null);
                    }
                } finally {
                    operationLock.writeLock().unlock();
                }
            }
            throw e;
        }
    }

    /** Reads display information, including an Account request when keys and a valid token are available. */
    @NotNull
    public DDLoggedUserView getLoggedUserView() {
        return userService.getLoggedUserView(getAccountId());
    }

    /** Reads display-only user identity from the local token without making an Account request. */
    @NotNull
    public DDLoggedUserView getLoggedUserIdentity() {
        return userService.getLoggedUserView(null);
    }


    //can call some UI functionality. so must be kept separately from instance creation
    public void load() throws DBException {
        operationLock.writeLock().lock();
        if ((loaded && initializationComplete) || Instant.now().isBefore(initializationRetryAt)) {
            operationLock.writeLock().unlock();
            return;
        }
        try {
            log.debug("Loading DataDam desktop access");
            notifyAccessListeners(DDDesktopAccessListener::accessChanging);
            initializeAccess();
        } catch (DBException e) {
            retryInitialization();
            throw e;
        } finally {
            finishAccessChange();
        }
    }

    public void signIn(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        operationLock.writeLock().lock();
        try {
            notifyAccessListeners(DDDesktopAccessListener::accessChanging);
            tokenManager.signIn(accountUrl, storageUrl, monitor);
            loaded = true;
            initializationComplete = false;
            setKeyBundle(null);
            initializeAccess();
            log.debug("Signed in to DataDam desktop access");
        } catch (DBException e) {
            retryInitialization();
            throw e;
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
            loaded = true;
            initializationComplete = true;
            initializationRetryAt = Instant.EPOCH;
            retryMillis = MIN_RETRY_MILLIS;
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
        Instant expiresAt = tokenManager.getExpiresAt();
        Instant nextAttempt;
        if (!loaded || !initializationComplete) {
            nextAttempt = initializationRetryAt;
        } else if (tokenManager.getStatus() == DDDesktopTokenManager.Status.SIGNED_IN) {
            nextAttempt = tokenManager.getRefreshAt();
        } else {
            return;
        }
        if (nextAttempt != null) {
            Job job = Job.createSystem("Expire DataDam desktop access", monitor -> {
                expireAccess(version, expiresAt);
            });
            expiryJob = job;
            job.schedule(Math.max(1, Duration.between(Instant.now(), nextAttempt).toMillis()));
        }
    }

    private void expireAccess(long version, @Nullable Instant expiresAt) {
        boolean recovering;
        operationLock.writeLock().lock();
        try {
            if (version != expiryVersion) {
                return;
            }
            expiryJob = null;
            recovering = !loaded || !initializationComplete;
            if (recovering) {
                try {
                    initializeAccess();
                } catch (DBException e) {
                    retryInitialization();
                    log.debug("DataDam desktop access could not be initialized");
                }
            }
        } finally {
            operationLock.writeLock().unlock();
        }
        // A slow refresh must not hide already validated keys from concurrent readers.
        if (!recovering) {
            try {
                getAccessTokenForResource(storageUrl);
            } catch (DBException e) {
                log.debug("DataDam desktop access could not be renewed");
            }
        }
        operationLock.writeLock().lock();
        try {
            if (version != expiryVersion) {
                return;
            }
            // An early wake keeps the deadline; a rotated token needs a new WebSocket handshake.
            boolean refreshed = !Objects.equals(expiresAt, tokenManager.getExpiresAt());
            boolean recovered = recovering && initializationComplete;
            if (refreshed || recovered) {
                notifyAccessListeners(DDDesktopAccessListener::accessChanging);
            }
            scheduleExpiry();
            if (refreshed || recovered || tokenManager.getStatus() != DDDesktopTokenManager.Status.SIGNED_IN) {
                notifyAccessListeners(DDDesktopAccessListener::accessChanged);
            }
        } finally {
            operationLock.writeLock().unlock();
        }
    }

    private void initializeAccess() throws DBException {
        if (!loaded) {
            tokenManager.load(storageUrl);
            loaded = true;
        }
        if (tokenManager.getStatus() == DDDesktopTokenManager.Status.SIGNED_IN) {
            tokenManager.getAccessTokenForResource(storageUrl);
            updateKeyBundle(DDKeyStore.load());
        } else {
            setKeyBundle(null);
        }
        initializationComplete = true;
        initializationRetryAt = Instant.EPOCH;
        retryMillis = MIN_RETRY_MILLIS;
    }

    private void retryInitialization() {
        initializationRetryAt = Instant.now().plusMillis(retryMillis);
        retryMillis = Math.min(MAX_RETRY_MILLIS, retryMillis * 2);
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

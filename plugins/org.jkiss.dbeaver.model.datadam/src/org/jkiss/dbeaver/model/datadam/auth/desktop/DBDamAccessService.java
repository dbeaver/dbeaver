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
public class DBDamAccessService {
    public enum Status { SIGNED_IN, SIGNED_OUT, INVALID_TOKEN, UNLOADED }

    private static final Log log = Log.getLog(DBDamAccessService.class);
    private static final Duration REFRESH_RETRY_DELAY = Duration.ofSeconds(30);
    private static final DBDamAccessService INSTANCE = new DBDamAccessService(DBDamTokenManager.getInstance());

    private final DBDamTokenManager tokenManager;
    private final ReentrantReadWriteLock operationLock = new ReentrantReadWriteLock();
    private final List<Runnable> keyBundleListeners = new CopyOnWriteArrayList<>();
    private final List<DBDamAccessListener> accessListeners = new CopyOnWriteArrayList<>();
    private final URI accountUrl = URI.create(DDEndpoints.getAccountBaseUrl());
    private final URI storageUrl = URI.create(DDEndpoints.getStorageBaseUrl());
    private final DDAccountClient accountClient = new DDAccountClient(accountUrl, this);
    private final DBDamUserService userService;
    private volatile boolean loaded;
    private boolean keyBundleLoadPending;
    @Nullable
    private volatile DDKeyBundle keyBundle;
    @Nullable
    private Job expiryJob;
    private long expiryVersion;

    DBDamAccessService(@NotNull DBDamTokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.userService = new DBDamUserService(tokenManager, accountClient);
    }

    @NotNull
    public static DBDamAccessService getInstance() {
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

    public boolean hasKeyBundle() {
        return getStatus() == Status.SIGNED_IN && keyBundle != null;
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
            if (tokenManager.getStatus() == DBDamTokenManager.Status.INVALID_TOKEN) {
                operationLock.writeLock().lock();
                try {
                    if (tokenManager.getStatus() == DBDamTokenManager.Status.INVALID_TOKEN) {
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
        if (loaded) {
            operationLock.writeLock().unlock();
            return;
        }
        try {
            log.debug("Loading DataDam desktop access");
            notifyAccessListeners(DBDamAccessListener::accessChanging);
            keyBundleLoadPending = true;
            tokenManager.load(storageUrl);
            updateKeyBundle(tokenManager.getStatus() == DBDamTokenManager.Status.SIGNED_IN ? DDKeyStore.load() : null);
            keyBundleLoadPending = false;
        } finally {
            loaded = true;
            finishAccessChange();
        }
    }

    public void signIn(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        operationLock.writeLock().lock();
        try {
            notifyAccessListeners(DBDamAccessListener::accessChanging);
            tokenManager.signIn(accountUrl, storageUrl, monitor);
            loaded = true;
            setKeyBundle(null);
            keyBundleLoadPending = true;
            updateKeyBundle(DDKeyStore.load());
            keyBundleLoadPending = false;
            log.debug("Signed in to DataDam desktop access");
        } finally {
            finishAccessChange();
        }
    }

    public void importKeys(@NotNull String recoveryPhrase) throws DBException {
        operationLock.writeLock().lock();
        try {
            saveImportedKeys(prepareKeyImport(recoveryPhrase));
        } finally {
            operationLock.writeLock().unlock();
        }
    }

    /** Validates a recovery phrase without replacing the saved keys. */
    @NotNull
    public DDKeyBundle prepareKeyImport(@NotNull String recoveryPhrase) throws DBException {
        operationLock.writeLock().lock();
        try {
            if (tokenManager.getStatus() != DBDamTokenManager.Status.SIGNED_IN) {
                throw new DBException("Sign in to DataDam before importing keys");
            }
            return DDKeyStore.unpack(accountClient.getCryptoState(), recoveryPhrase);
        } finally {
            operationLock.writeLock().unlock();
        }
    }

    /** Commits a prepared replacement only if access has not changed during project cleanup. */
    public void completeKeyImport(@NotNull DDKeyBundle expectedKeys, @NotNull DDKeyBundle replacement) throws DBException {
        operationLock.writeLock().lock();
        try {
            requireRecoveryKeys(expectedKeys, replacement);
            saveImportedKeys(replacement);
        } finally {
            operationLock.writeLock().unlock();
        }
    }

    @FunctionalInterface
    public interface ProjectCleanup {
        void run() throws DBException;
    }

    /** Prevents account and key changes from interleaving with destructive local recovery. */
    public void recoverProjectKeys(
        @NotNull DDKeyBundle expectedKeys, @Nullable DDKeyBundle replacement, @NotNull ProjectCleanup cleanup
    ) throws DBException {
        operationLock.writeLock().lock();
        try {
            requireRecoveryKeys(expectedKeys, replacement);
            cleanup.run();
            requireRecoveryKeys(expectedKeys, replacement);
            if (replacement == null) {
                forgetKeys();
            } else {
                saveImportedKeys(replacement);
            }
        } finally {
            operationLock.writeLock().unlock();
        }
    }

    private void requireRecoveryKeys(@NotNull DDKeyBundle expectedKeys, @Nullable DDKeyBundle replacement) throws DBException {
        if (getStatus() != Status.SIGNED_IN || !expectedKeys.equals(keyBundle)
            || replacement != null && !expectedKeys.accountId().equals(replacement.accountId())) {
            throw new DBException("DataDam account or encryption key changed during import");
        }
    }

    private void saveImportedKeys(@NotNull DDKeyBundle bundle) throws DBException {
        DDKeyStore.save(bundle);
        keyBundleLoadPending = false;
        setKeyBundle(bundle);
        log.debug("Imported DataDam encryption keys");
    }

    public void forgetKeys() throws DBException {
        operationLock.writeLock().lock();
        try {
            DDKeyStore.clear();
            keyBundleLoadPending = false;
            setKeyBundle(null);
            log.debug("Forgot DataDam encryption keys");
        } finally {
            operationLock.writeLock().unlock();
        }
    }

    /** Checks the key identity, which changes when account encryption is reset. */
    public boolean validateProjectKeys() throws DBException {
        DDKeyBundle bundle = getKeyBundle();
        if (getStatus() != Status.SIGNED_IN || bundle == null) {
            return false;
        }
        // Do not hold the operation lock over an Account call: token refresh may need to change access.
        var state = accountClient.getCryptoState();
        return bundle.equals(getKeyBundle()) && state.cryptoConfigured() && bundle.accountId().equals(state.accountId())
            && Objects.equals(bundle.apiKeyId(), state.apiKeyId());
    }

    public void signOut() throws DBException {
        operationLock.writeLock().lock();
        try {
            notifyAccessListeners(DBDamAccessListener::accessChanging);
            tokenManager.signOut();
            loaded = true;
            keyBundleLoadPending = false;
            setKeyBundle(null);
            DDKeyStore.clear();
            log.debug("Signed out of DataDam desktop access");
        } finally {
            finishAccessChange();
        }
    }

    public void addAccessListener(@NotNull DBDamAccessListener listener) {
        accessListeners.add(listener);
    }

    public void removeAccessListener(@NotNull DBDamAccessListener listener) {
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
                notifyAccessListeners(DBDamAccessListener::accessChanged);
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
        if (tokenManager.getStatus() != DBDamTokenManager.Status.SIGNED_IN) {
            return;
        }
        Instant expiresAt = tokenManager.getExpiresAt();
        if (expiresAt != null) {
            Job job = Job.createSystem("Expire DataDam desktop access", monitor -> {
                expireAccess(version, expiresAt);
            });
            expiryJob = job;
            long delay = Duration.between(Instant.now(), expiresAt).toMillis();
            if (keyBundleLoadPending) {
                delay = Math.min(delay, REFRESH_RETRY_DELAY.toMillis());
            }
            // An expired token may still be refreshable after a temporary network failure.
            job.schedule(delay > 0 ? delay : REFRESH_RETRY_DELAY.toMillis());
        }
    }

    private void expireAccess(long version, @NotNull Instant expiresAt) {
        operationLock.writeLock().lock();
        try {
            if (version != expiryVersion) {
                return;
            }
            expiryJob = null;
        } finally {
            operationLock.writeLock().unlock();
        }
        boolean tokenAvailable = false;
        try {
            getAccessTokenForResource(storageUrl);
            tokenAvailable = true;
        } catch (DBException e) {
            log.debug(tokenManager.getStatus() == DBDamTokenManager.Status.INVALID_TOKEN
                ? "DataDam authorization requires sign-in" : "DataDam token refresh will be retried", e);
        }
        operationLock.writeLock().lock();
        try {
            if (version != expiryVersion) {
                return;
            }
            // Credential rotation does not change desktop access or the running session.
            boolean refreshed = !Objects.equals(expiresAt, tokenManager.getExpiresAt());
            if (tokenAvailable && keyBundleLoadPending) {
                try {
                    updateKeyBundle(DDKeyStore.load());
                    keyBundleLoadPending = false;
                } catch (DBException e) {
                    log.debug("DataDam encryption key loading will be retried", e);
                }
            }
            scheduleExpiry();
            if (tokenManager.getStatus() != DBDamTokenManager.Status.SIGNED_IN) {
                notifyAccessListeners(DBDamAccessListener::accessChanged);
            } else if (refreshed) {
                notifyAccessListeners(DBDamAccessListener::accessTokenRefreshed);
            }
        } finally {
            operationLock.writeLock().unlock();
        }
    }

    private void notifyAccessListeners(@NotNull Consumer<DBDamAccessListener> event) {
        for (DBDamAccessListener listener : accessListeners) {
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

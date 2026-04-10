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
package org.jkiss.dbeaver.model.fs.lock.local;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.fs.lock.Lock;
import org.jkiss.dbeaver.model.fs.lock.LockException;
import org.jkiss.dbeaver.model.fs.lock.LockManager;
import org.jkiss.dbeaver.model.fs.lock.LockOptions;
import org.jkiss.dbeaver.model.fs.lock.LockTarget;
import org.jkiss.dbeaver.utils.FileMutex;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class LocalFileLockManager implements LockManager {

    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofMinutes(1);
    private static final String LOCK_FOLDER_NAME = ".local-locks";
    private static final String LOCK_FILE_EXTENSION = ".lock";

    private final Path metadataFolder;
    private final Duration lockTimeout;

    public LocalFileLockManager() {
        this(GeneralUtils.getMetadataFolder());
    }

    public LocalFileLockManager(@NotNull Path metadataFolder) {
        this(metadataFolder, DEFAULT_LOCK_TIMEOUT);
    }

    public LocalFileLockManager(@NotNull Path metadataFolder, @NotNull Duration lockTimeout) {
        this.metadataFolder = metadataFolder;
        this.lockTimeout = lockTimeout;
    }

    @NotNull
    @Override
    public Lock lock(@NotNull LockTarget target, @Nullable LockOptions options) throws LockException {
        Path lockFilePath = getLockFilePath(target);
        try {
            createLockFolderIfNeeded();
            return new LocalFileLock(lockFilePath, FileMutex.tryLock(lockFilePath, getTimeout(options)));
        } catch (IOException e) {
            throw new LockException("Failed to lock target: " + target.value(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockException("Interrupted while locking target: " + target.value(), e);
        }
    }

    @Nullable
    @Override
    public Lock tryLock(@NotNull LockTarget target, @Nullable LockOptions options) throws LockException {
        Path lockFilePath = getLockFilePath(target);
        if (isLocked(target)) {
            return null;
        }
        try {
            createLockFolderIfNeeded();
            return new LocalFileLock(lockFilePath, FileMutex.tryLock(lockFilePath));
        } catch (IOException e) {
            if (Files.exists(lockFilePath) && isLocked(target)) {
                return null;
            }
            throw new LockException("Failed to lock target: " + target.value(), e);
        }
    }

    @Override
    public boolean isLocked(@NotNull LockTarget target) throws LockException {
        Path lockFilePath = getLockFilePath(target);
        if (Files.notExists(lockFilePath)) {
            return false;
        }
        try {
            return FileMutex.isLocked(lockFilePath);
        } catch (IOException e) {
            throw new LockException("Failed to check lock state for target: " + target.value(), e);
        }
    }

    @NotNull
    private Duration getTimeout(@Nullable LockOptions options) {
        if (options == null || options.timeout() == null) {
            return lockTimeout;
        }
        return options.timeout();
    }

    @NotNull
    private Path getLockFilePath(@NotNull LockTarget target) {
        return metadataFolder.resolve(LOCK_FOLDER_NAME).resolve(target.value() + LOCK_FILE_EXTENSION);
    }

    private void createLockFolderIfNeeded() throws IOException {
        Files.createDirectories(metadataFolder.resolve(LOCK_FOLDER_NAME));
    }
}

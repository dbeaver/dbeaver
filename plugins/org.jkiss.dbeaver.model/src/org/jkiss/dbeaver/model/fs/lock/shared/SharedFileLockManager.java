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
package org.jkiss.dbeaver.model.fs.lock.shared;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.fs.lock.FileLockController;
import org.jkiss.dbeaver.model.fs.lock.Lock;
import org.jkiss.dbeaver.model.fs.lock.LockException;
import org.jkiss.dbeaver.model.fs.lock.LockManager;
import org.jkiss.dbeaver.model.fs.lock.LockOptions;
import org.jkiss.dbeaver.model.fs.lock.LockTarget;

import java.nio.file.Path;

public class SharedFileLockManager implements LockManager {

    private final FileLockController fileLockController;

    public SharedFileLockManager(@NotNull String applicationId) throws LockException {
        this.fileLockController = createController(applicationId);
    }

    public SharedFileLockManager(@NotNull String applicationId, @NotNull Path metadataFolder) throws LockException {
        this.fileLockController = createController(applicationId, metadataFolder);
    }

    @NotNull
    @Override
    public Lock lock(@NotNull LockTarget target, @Nullable LockOptions options) throws LockException {
        try {
            return new SharedFileLock(fileLockController.lock(target.value(), getOperationName(options)));
        } catch (DBException e) {
            throw new LockException("Failed to lock target: " + target.value(), e);
        }
    }

    @Nullable
    @Override
    public Lock tryLock(@NotNull LockTarget target, @Nullable LockOptions options) throws LockException {
        try {
            var fileLock = fileLockController.lockIfNotLocked(target.value(), getOperationName(options));
            if (fileLock == null) {
                return null;
            }
            return new SharedFileLock(fileLock);
        } catch (DBException e) {
            throw new LockException("Failed to lock target: " + target.value(), e);
        }
    }

    @Override
    public boolean isLocked(@NotNull LockTarget target) {
        return fileLockController.isFileLocked(target.value());
    }

    @NotNull
    private static FileLockController createController(@NotNull String applicationId) throws LockException {
        try {
            return new FileLockController(applicationId);
        } catch (DBException e) {
            throw new LockException("Failed to initialize shared file lock manager", e);
        }
    }

    @NotNull
    private static FileLockController createController(@NotNull String applicationId, @NotNull Path metadataFolder) throws LockException {
        try {
            return new FileLockController(applicationId, metadataFolder);
        } catch (DBException e) {
            throw new LockException("Failed to initialize shared file lock manager", e);
        }
    }

    @NotNull
    private static String getOperationName(@Nullable LockOptions options) {
        if (options == null || options.operationName() == null) {
            return "";
        }
        return options.operationName();
    }
}

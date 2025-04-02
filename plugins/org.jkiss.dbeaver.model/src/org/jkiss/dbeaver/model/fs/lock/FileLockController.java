/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.fs.lock;

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * File based resource locks
 */
public class FileLockController {
    private static final Log log = Log.getLog(FileLockController.class);
    private static final long DEFAULT_MAX_LOCK_TIME = Duration.ofMinutes(1).toMillis(); // 1 min
    private static final int CHECK_PERIOD = 10;

    private static final String LOCK_META_FOLDER = ".locks";
    private static final String LOCK_FILE_EXTENSION = ".lock";

    private final Gson gson = new Gson();

    private final Path lockFolderPath;
    private final String applicationId;
    private final long maxLockTime;

    public FileLockController(@NotNull String applicationId) throws DBException {
        this(applicationId, DEFAULT_MAX_LOCK_TIME);
    }

    // for tests
    public FileLockController(@NotNull String applicationId, long maxLockTime) throws DBException {
        this.lockFolderPath =  GeneralUtils.getMetadataFolder()
            .resolve(LOCK_META_FOLDER);
        this.applicationId = applicationId;
        this.maxLockTime = maxLockTime;
    }

    /**
     * Lock the project for the duration of any operation.
     * Other threads/processes will also see this lock, and will wait for it to end
     * or force intercept lock, if the operation will take too long and
     * exceeds the maximum available locking time {@link #maxLockTime} or the lock is invalid {@link #awaitUnlock)}.
     *
     * @param lockFileName - file to be locked
     * @param operationName - executed operation name
     * @return - lock
     */
    @NotNull
    public FileLock lock(@NotNull String lockFileName, @NotNull String operationName) throws DBException {
        synchronized (FileLockController.class) {
            try {
                FileLockInfo lockInfo = new FileLockInfo.Builder(UUID.randomUUID().toString())
                    .setApplicationId(applicationId)
                    .setOperationName(operationName)
                    .setOperationStartTime(System.currentTimeMillis())
                    .build();
                Path lockFilePath = getLockFilePath(lockFileName);

                if (!IOUtils.isFileFromDefaultFS(lockFolderPath)) {
                    // fake lock for external file system?
                    return new FileLock(lockFilePath);
                }
                createLockFolderIfNeeded();
                createLockFile(lockFilePath, lockInfo);
                return new FileLock(lockFilePath);
            } catch (Exception e) {
                throw new DBException("Failed to lock file: " + lockFileName, e);
            }
        }
    }

    /**
     * if the project is already locked, the operation will be executed as a child of the first lock,
     * otherwise it creates its own lock.
     *
     * @param lockFileName  - file to be locked
     * @param operationName - executed operation name
     * @return - lock
     */
    @Nullable
    public FileLock lockIfNotLocked(@NotNull String lockFileName, @NotNull String operationName) throws DBException {
        synchronized (FileLockController.class) {
            if (isLocked(getLockFilePath(lockFileName))) {
                return null;
            }
            return lock(lockFileName, operationName);
        }
    }

    protected boolean isLocked(@NotNull Path lockFilePath) {
        return Files.exists(lockFilePath);
    }

    /**
     * Check that file locked
     */
    public boolean isFileLocked(@NotNull String lockFileName) {
        Path projectLockFilePath = getLockFilePath(lockFileName);
        return isLocked(projectLockFilePath);
    }

    private void createLockFile(@NotNull Path lockFile, @NotNull FileLockInfo lockInfo) throws DBException, InterruptedException {
        boolean lockFileCreated = false;
        while (!lockFileCreated) {
            if (Files.exists(lockFile)) {
                awaitUnlock(lockFile);
            }
            try {
                Files.createFile(lockFile);
                lockFileCreated = true;
            } catch (IOException e) {
                if (Files.exists(lockFile)) {
                    log.info("Looks like file was locked by another rm instance at the same time");
                    continue;
                } else {
                    throw new DBException("Failed to create lock file: " + lockFile, e);
                }
            }

            try {
                Files.write(lockFile, gson.toJson(lockInfo).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("Failed to write lock info, unlock: " + lockFile);
                try {
                    Files.deleteIfExists(lockFile);
                } catch (IOException ex) {
                    throw new DBException("Failed to remove invalid lock file: " + lockFile, ex);
                }
                throw new DBException("Failed to lock: " + lockFile.getFileName(), e);
            }
        }

    }

    protected void awaitUnlock(@NotNull Path lockFile) throws InterruptedException, DBException {
        if (!isLocked(lockFile)) {
            return;
        }
        awaitingUnlock(lockFile);
    }

    protected void awaitingUnlock(@NotNull Path lockFile) throws DBException, InterruptedException {
        log.info("Waiting for a file to be unlocked: " + lockFile);
        FileLockInfo originalLockInfo = readLockInfo(lockFile);
        boolean fileUnlocked = originalLockInfo == null; //lock can be removed at the moment when we try to read lock file info
        long maxIterations = maxLockTime / CHECK_PERIOD;
        int currentCheckCount = 0;

        while (!fileUnlocked) {
            fileUnlocked = !isLocked(lockFile);
            if (currentCheckCount >= maxIterations || fileUnlocked) {
                break;
            }
            if (originalLockInfo != null && originalLockInfo.isBlank()) {
                // possible in situation where the project has just been locked
                // and the lock information has not yet been written
                originalLockInfo = readLockInfo(lockFile);
            }
            currentCheckCount++;
            Thread.sleep(CHECK_PERIOD);
        }

        if (fileUnlocked) {
            return;
        }

        FileLockInfo currentLockInfo = readLockInfo(lockFile);
        if (currentLockInfo == null) {
            // file unlocked now
            return;
        }

        //checking that this is not a new lock from another operation
        if (originalLockInfo != null && originalLockInfo.getOperationId().equals(currentLockInfo.getOperationId())) {
            forceUnlock(lockFile);
        } else {
            awaitUnlock(lockFile);
        }
    }

    protected void forceUnlock(Path projectLockFile) {
        // something went wrong and lock is invalid
        log.warn("File has not been unlocked within the expected period, force unlock");
        try {
            Files.deleteIfExists(projectLockFile);
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Nullable
    /**
     @return
     - null if lock not exist;
     - empty lock info if the lock has just been created and the information has not yet been written;
     - lock info
     */
    private FileLockInfo readLockInfo(@NotNull Path lockFile) {
        if (Files.notExists(lockFile)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(lockFile, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, FileLockInfo.class);
        } catch (IOException e) {
            if (!isLocked(lockFile)) {
                return null;
            }
            log.warn("Failed to read lock file info, but lock file still exist: " + lockFile);
            return FileLockInfo.emptyLock();
        }
    }

    @NotNull
    private Path getLockFilePath(@NotNull String lockFileName) {
        return lockFolderPath.resolve(lockFileName + LOCK_FILE_EXTENSION);
    }

    private void createLockFolderIfNeeded() throws IOException {
        synchronized (FileLockController.class) {
            if (Files.notExists(lockFolderPath)) {
                Files.createDirectories(lockFolderPath);
            }
        }
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.rm.lock;

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPWorkspace;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * File based resource locks
 */
public class RMFileLockController {
    private static final Log log = Log.getLog(RMFileLockController.class);
    private static final int DEFAULT_MAX_LOCK_TIME = 1 * 60 * 1000; // 1 min
    private static final int CHECK_PERIOD = 10;

    private static final String LOCK_META_FOLDER = ".locks";
    private static final String LOCK_FILE_EXTENSION = ".lock";

    private final Gson gson = new Gson();

    private final Path lockFolderPath;
    private final String applicationId;
    private final int maxLockTime;


    public RMFileLockController(DBPApplication application) {
        this(application, DEFAULT_MAX_LOCK_TIME);
    }

    // for tests
    public RMFileLockController(DBPApplication application, int maxLockTime) {
        this.lockFolderPath = application.getWorkspaceDirectory()
            .resolve(DBPWorkspace.METADATA_FOLDER)
            .resolve(LOCK_META_FOLDER);
        this.applicationId = application.getApplicationId();
        this.maxLockTime = maxLockTime;
    }

    /**
     * Lock the project for the duration of any operation.
     * Other threads/processes will also see this lock, and will wait for it to end
     * or force intercept lock, if the operation will take too long and
     * exceeds the maximum available locking time {@link #maxLockTime} or the lock is invalid {@link #awaitUnlock)}.
     *
     * @param projectId     - project to be locked
     * @param operationName - executed operation name
     * @return - lock
     */
    @NotNull
    public RMLock lockProject(@NotNull String projectId,@NotNull  String operationName) throws DBException {
        synchronized (RMFileLockController.class) {
            try {
                createLockFolderIfNeeded();
                createProjectFolder(projectId);
                Path projectLockFile = getProjectLockFilePath(projectId);

                RMLockInfo lockInfo = new RMLockInfo.Builder(projectId, UUID.randomUUID().toString())
                    .setApplicationId(applicationId)
                    .setOperationName(operationName)
                    .setOperationStartTime(System.currentTimeMillis())
                    .build();
                createLockFile(projectLockFile, lockInfo);
                return new RMLock(projectLockFile);
            } catch (Exception e) {
                throw new DBException("Failed to lock project: " + projectId, e);
            }
        }
    }

    /**
     * if the project is already locked, the operation will be executed as a child of the first lock,
     * otherwise it creates its own lock.
     *
     * @param projectId     - project to be locked
     * @param operationName - executed operation name
     * @return - lock
     */
    @Nullable
    public RMLock lockIfNotLocked(@NotNull String projectId, @NotNull String operationName) throws DBException {
        synchronized (RMFileLockController.class) {
            if (isProjectLocked(projectId)) {
                return null;
            }
            return lockProject(projectId, operationName);
        }
    }

    /**
     * Check that project locked
     */
    public boolean isProjectLocked(String projectId) {
        Path projectLockFilePath = getProjectLockFilePath(projectId);
        return isLocked(projectLockFilePath);
    }

    protected boolean isLocked(Path lockFilePath) {
        return Files.exists(lockFilePath);
    }

    private void createLockFile(Path projectLockFile, RMLockInfo lockInfo) throws DBException, InterruptedException {
        boolean lockFileCreated = false;
        while (!lockFileCreated) {
            if (Files.exists(projectLockFile)) {
                awaitUnlock(lockInfo.getProjectId(), projectLockFile);
            }
            try {
                Files.createFile(projectLockFile);
                lockFileCreated = true;
            } catch (IOException e) {
                if (Files.exists(projectLockFile)) {
                    log.info("Looks like file was locked by another rm instance at the same time");
                    continue;
                } else {
                    throw new DBException("Failed to create lock file: " + projectLockFile, e);
                }
            }

            try {
                Files.write(projectLockFile, gson.toJson(lockInfo).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("Failed to write lock info, unlock project: " + lockInfo.getProjectId());
                try {
                    Files.deleteIfExists(lockFolderPath);
                } catch (IOException ex) {
                    throw new DBException("Failed to remove invalid lock file: " + projectLockFile, ex);
                }
                throw new DBException("Failed to lock project: " + lockInfo.getProjectId(), e);
            }
        }

    }

    private void createProjectFolder(String projectId) throws DBException {
        Path projectLocksFolder = lockFolderPath.resolve(projectId);
        if (Files.exists(projectLocksFolder)) {
            return;
        }
        try {
            Files.createDirectories(projectLocksFolder);
        } catch (Exception e) {
            if (Files.exists(projectLocksFolder)) {
                // ignore, because file can be created by another server
            } else {
                throw new DBException("Failed to create project lock folder: " + projectId, e);
            }
        }
    }

    protected void awaitUnlock(String projectId, Path projectLockFile) throws InterruptedException, DBException {
        if (!isLocked(projectLockFile)) {
            return;
        }
        awaitingUnlock(projectId, projectLockFile);
    }

    protected void awaitingUnlock(String projectId, Path projectLockFile) throws DBException, InterruptedException {
        log.info("Waiting for a file to be unlocked: " + projectLockFile);
        RMLockInfo originalLockInfo = readLockInfo(projectId, projectLockFile);
        boolean fileUnlocked = originalLockInfo == null; //lock can be removed at the moment when we try to read lock file info
        int maxIterations = maxLockTime / CHECK_PERIOD;
        int currentCheckCount = 0;

        while (!fileUnlocked) {
            fileUnlocked = !isLocked(projectLockFile);
            if (currentCheckCount >= maxIterations || fileUnlocked) {
                break;
            }
            if (originalLockInfo != null & originalLockInfo.isBlank()) {
                // possible in situation where the project has just been locked
                // and the lock information has not yet been written
                originalLockInfo = readLockInfo(projectId, projectLockFile);
            }
            currentCheckCount++;
            Thread.sleep(CHECK_PERIOD);
        }
        if (fileUnlocked) {
            return;
        }

        RMLockInfo currentLockInfo = readLockInfo(projectId, projectLockFile);
        if (currentLockInfo == null) {
            // file unlocked now
            return;
        }

        //checking that this is not a new lock from another operation
        if (originalLockInfo.getOperationId().equals(currentLockInfo.getOperationId())) {
            forceUnlock(projectLockFile);
        } else {
            awaitUnlock(projectId, lockFolderPath);
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
    private RMLockInfo readLockInfo(String projectId, Path projectLockFile) throws DBException {
        if (Files.notExists(projectLockFile)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(projectLockFile, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, RMLockInfo.class);
        } catch (IOException e) {
            if (!isLocked(projectLockFile)) {
                return null;
            }
            log.warn("Failed to read lock file info, but lock file still exist: " + projectLockFile);
            return RMLockInfo.emptyLock(projectId);
        }
    }

    private Path getProjectLockFilePath(String projectId) {
        return lockFolderPath.resolve(projectId).resolve(projectId + LOCK_FILE_EXTENSION);
    }

    private void createLockFolderIfNeeded() throws IOException {
        synchronized (RMFileLockController.class) {
            if (Files.notExists(lockFolderPath)) {
                Files.createDirectories(lockFolderPath);
            }
        }
    }
}

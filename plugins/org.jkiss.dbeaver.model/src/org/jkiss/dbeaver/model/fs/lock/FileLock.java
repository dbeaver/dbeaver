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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resource Manager resource lock
 */
public class FileLock implements AutoCloseable {
    private static final Log log = Log.getLog(FileLock.class);

    private final Path lockFilePath;

    public FileLock(@NotNull Path lockFilePath) {
        this.lockFilePath = lockFilePath;
    }

    /**
     * Unlock resource and remove .lock file
     */
    public void unlock() {
        try {
            Files.deleteIfExists(lockFilePath);
        } catch (IOException e) {
            log.error("Failed to unlock file: " + lockFilePath, e);
            if (Files.exists(lockFilePath)) {
                // file still locket, try to unlock again
                unlock();
            }
        }
    }

    /**
     * @return path to the lock file
     */
    protected Path getLockFilePath() {
        return lockFilePath;
    }

    @Override
    public void close() {
        unlock();
    }
}

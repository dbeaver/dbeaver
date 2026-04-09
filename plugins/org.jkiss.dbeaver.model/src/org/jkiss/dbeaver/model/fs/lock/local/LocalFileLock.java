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
import org.jkiss.dbeaver.model.fs.lock.Lock;
import org.jkiss.dbeaver.model.fs.lock.LockException;
import org.jkiss.dbeaver.utils.FileMutex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalFileLock implements Lock {

    private final Path lockFilePath;
    private final FileMutex fileMutex;

    public LocalFileLock(@NotNull Path lockFilePath, @NotNull FileMutex fileMutex) {
        this.lockFilePath = lockFilePath;
        this.fileMutex = fileMutex;
    }

    @Override
    public void close() throws LockException {
        try {
            fileMutex.close();
        } catch (IOException e) {
            throw new LockException("Failed to unlock file: " + lockFilePath, e);
        }
        try {
            Files.deleteIfExists(lockFilePath);
        } catch (IOException e) {
            throw new LockException("Failed to remove lock file: " + lockFilePath, e);
        }
    }
}

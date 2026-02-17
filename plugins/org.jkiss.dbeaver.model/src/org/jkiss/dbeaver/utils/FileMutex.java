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
package org.jkiss.dbeaver.utils;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileMutex implements AutoCloseable {

    private final FileChannel channel;
    private final FileLock lock;

    private FileMutex(@NotNull FileChannel channel, @NotNull FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    @NotNull
    public static FileMutex tryLock(@NotNull Path path) throws IOException {
        LockAttempt attempt = attemptLock(path);

        if (attempt.lock == null) {
            attempt.channel.close();
            throw new IllegalStateException("Already locked: " + path);
        }

        return new FileMutex(attempt.channel, attempt.lock);
    }

    public static boolean isLocked(@NotNull Path path) {
        try {
            LockAttempt attempt = attemptLock(path);

            if (attempt.lock == null) {
                return true;
            }

            attempt.lock.release();
            attempt.channel.close();
            return false;

        } catch (IOException e) {
            return true;
        }
    }

    @NotNull
    private static LockAttempt attemptLock(@NotNull Path path) throws IOException {
        FileChannel channel = FileChannel.open(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        );

        FileLock lock = channel.tryLock();
        return new LockAttempt(channel, lock);
    }

    private record LockAttempt(@NotNull FileChannel channel, @Nullable FileLock lock) {}

    @Override
    public void close() throws IOException {
        lock.release();
        channel.close();
    }
}
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
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class FileMutex implements AutoCloseable {

    private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofMillis(50);

    private final FileChannel channel;
    private final FileLock lock;

    private FileMutex(@NotNull FileChannel channel, @NotNull FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    @NotNull
    public static FileMutex tryLock(@NotNull Path path) throws IOException {
        try {
            return tryLock(path, Duration.ZERO);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while acquiring file lock: " + path, e);
        }
    }

    @NotNull
    public static FileMutex tryLock(
        @NotNull Path path,
        @NotNull Duration timeout
    ) throws IOException, InterruptedException {
        return tryLock0(path, timeout);
    }

    @NotNull
    private static FileMutex tryLock0(
        @NotNull Path path,
        @NotNull Duration timeout
    ) throws IOException, InterruptedException {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout must be >= 0");
        }

        if (timeout.isZero()) {
            LockAttempt attempt = attemptLock(path);
            if (attempt.lock != null) {
                return new FileMutex(attempt.channel, attempt.lock);
            }
            attempt.channel.close();
            throw new IOException("File is locked: " + path);
        }

        long timeoutNanos = timeout.toNanos();
        long retryNanos = DEFAULT_RETRY_INTERVAL.toNanos();
        long startedAt = System.nanoTime();

        while (true) {
            LockAttempt attempt = attemptLock(path);
            if (attempt.lock != null) {
                return new FileMutex(attempt.channel, attempt.lock);
            }
            attempt.channel.close();

            long elapsed = System.nanoTime() - startedAt;
            if (elapsed >= timeoutNanos) {
                throw new IOException("Timeout waiting for file lock: " + path);
            }

            long remaining = timeoutNanos - elapsed;
            long sleepNanos = Math.min(retryNanos, remaining);
            TimeUnit.NANOSECONDS.sleep(sleepNanos);
        }
    }



    public static boolean isLocked(@NotNull Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )) {
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException e) {
                return true;
            }

            if (lock == null) {
                return true;
            }

            lock.release();
            return false;
        }
    }

    @NotNull
    private static LockAttempt attemptLock(@NotNull Path path) throws IOException {
        FileChannel channel = FileChannel.open(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        );

        FileLock lock;
        try {
            lock = channel.tryLock();
        } catch (OverlappingFileLockException e) {
            lock = null;
        }

        return new LockAttempt(channel, lock);
    }

    private record LockAttempt(@NotNull FileChannel channel, @Nullable FileLock lock) { }

    @Override
    public void close() throws IOException {
        IOException error = null;

        try {
            if (lock.isValid()) {
                lock.release();
            }
        } catch (IOException e) {
            error = e;
        }

        try {
            if (channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            if (error == null) {
                error = e;
            } else {
                error.addSuppressed(e);
            }
        }

        if (error != null) {
            throw error;
        }
    }
}
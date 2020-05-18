/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPExclusiveResource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple exclusive lock
 */
public class SimpleExclusiveLock implements DBPExclusiveResource {

    private static final String TASK_GLOBAL = "#global";

    private static class Lock {
        private volatile Thread lockThread;
        private int lockCount = 0;
    }
    private final Map<String, Lock> locks = new HashMap<>();

    @Override
    public Object acquireExclusiveLock() {
        return acquireTaskLock(TASK_GLOBAL, false);
    }

    @Override
    public Object acquireTaskLock(@NotNull String taskName, boolean checkDup) {
        Thread curThread = Thread.currentThread();
        Lock lock;
        synchronized (this) {
            lock = locks.get(taskName);
            if (lock == null) {
                lock = new Lock();
                locks.put(taskName, lock);
            }
        }

        boolean taskRunning = false;
        for (;;) {
            synchronized (this) {
                if (lock.lockThread == curThread || lock.lockThread == null) {
                    if (checkDup && taskRunning) {
                        return TASK_PROCESED;
                    }
                    lock.lockThread = curThread;
                    lock.lockCount++;
                    return curThread;
                }
            }
            taskRunning = true;
            // Wait for a while
            DBWorkbench.getPlatformUI().readAndDispatchEvents();
            RuntimeUtils.pause(50);
        }
    }

    @Override
    public void releaseExclusiveLock(@NotNull Object lock) {
        releaseTaskLock(TASK_GLOBAL, lock);
    }

    @Override
    public void releaseTaskLock(@NotNull String taskName, @NotNull Object lockObj) {
        synchronized (this) {
            Lock lock = locks.get(taskName);
            if (lock == null) {
                throw new IllegalArgumentException("Wrong task name: " + taskName);
            }

            if (lock.lockThread != lockObj) {
                throw new IllegalArgumentException("Wrong exclusive lock passed");
            }
            lock.lockCount--;
            if (lock.lockCount == 0) {
                lock.lockThread = null;
            } else if (lock.lockCount < 0) {
                throw new IllegalStateException("Internal error: negative lock count. Restart application.");
            }
        }
    }

}

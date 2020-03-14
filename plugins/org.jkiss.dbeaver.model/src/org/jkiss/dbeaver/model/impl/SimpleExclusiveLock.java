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

import org.jkiss.dbeaver.model.DBPExclusiveResource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Simple exclusive lock
 */
public class SimpleExclusiveLock implements DBPExclusiveResource {

    private volatile Thread lockThread;
    private int lockCount = 0;

    @Override
    public Object acquireExclusiveLock() {
        Thread curThread = Thread.currentThread();
        for (;;) {
            synchronized (this) {
                if (lockThread == curThread || lockThread == null) {
                    lockThread = curThread;
                    lockCount++;
                    return curThread;
                }
            }
            // Wait for a while
            DBWorkbench.getPlatformUI().readAndDispatchEvents();
            RuntimeUtils.pause(50);
        }
    }

    @Override
    public void releaseExclusiveLock(Object lock) {
        synchronized (this) {
            if (lockThread != lock) {
                throw new IllegalArgumentException("Wrong exclusive lock passed");
            }
            lockCount--;
            if (lockCount == 0) {
                lockThread = null;
            } else if (lockCount < 0) {
                throw new IllegalStateException("Internal error: negative lock count. Restart application.");
            }
        }
    }

}

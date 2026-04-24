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
package org.jkiss.dbeaver.ui;

import org.eclipse.ui.internal.Workbench;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.List;

/**
 * Similar to simple Display.asyncExec but puts all jobs in queue.
 * Next job can be run only after previous job is finished.
 * It is needed to avoid simultaneous UI jobs start (e.g. in case when one job opens a dialog and other jobs will run in this dialog idle exec)
 */
public class UIExecutionQueue {

    private static final List<Runnable> execQueue = new ArrayList<>();
    private static int runCount = 0;
    private static volatile Runnable nextJob;

    public static void queueExec(@NotNull Runnable runnable) {
        synchronized (execQueue) {
            execQueue.add(runnable);
        }
        UIUtils.asyncExec(UIExecutionQueue::executeInUI);
    }

    public static void blockQueue() {
        synchronized (execQueue) {
            runCount++;
        }
    }

    public static void unblockQueue() {
        synchronized (execQueue) {
            if (runCount <= 0) {
                throw new IllegalStateException("Queue is unblocked");
            }
            runCount--;
        }
    }

    private static void executeInUI() {
        synchronized (execQueue) {
            boolean workbenchStarted = DBWorkbench.getPlatform() instanceof DBPPlatformDesktop pd && pd.isWorkbenchStarted();
            if (runCount > 0 || !workbenchStarted) {
                // If workbench wasn't fully started or
                // job is running or
                // some Eclipse job is active in UI thread then retry later
                if (!DBWorkbench.getPlatform().isShuttingDown()) {
                    UIUtils.asyncExec(UIExecutionQueue::executeInUI);
                }
                return;
            }
            if (execQueue.isEmpty()) {
                return;
            }
            runCount++;
            nextJob = execQueue.removeFirst();
        }
        try {
            if (!Workbench.getInstance().isClosing()) {
                nextJob.run();
            }
        } finally {
            synchronized (execQueue) {
                nextJob = null;
                runCount--;
            }
        }
        UIUtils.asyncExec(UIExecutionQueue::executeInUI);
    }

}
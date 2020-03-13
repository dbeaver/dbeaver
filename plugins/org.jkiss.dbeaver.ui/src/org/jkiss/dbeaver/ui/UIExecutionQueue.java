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
package org.jkiss.dbeaver.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Similar to simple Display.asyncExec but puts all jobs in queue.
 * Next job can be run only after previous job is finished.
 * It is needed to avoid simultaneous UI jobs start (e.g. in case when one job opens a dialog and other jobs will run in this dialog idle exec)
 */
public class UIExecutionQueue {

    private static final List<Runnable> execQueue = new ArrayList<>();
    private static boolean isRunning = false;

    public static void queueExec(Runnable runnable) {
        synchronized (execQueue) {
            execQueue.add(runnable);
        }
        UIUtils.asyncExec(UIExecutionQueue::executeInUI);
    }

    private static void executeInUI() {
        Runnable nextJob;
        synchronized (execQueue) {
            if (isRunning) {
                return;
            }
            if (execQueue.isEmpty()) {
                return;
            }
            isRunning = true;
            nextJob = execQueue.remove(0);
        }
        try {
            nextJob.run();
        } finally {
            synchronized (execQueue) {
                isRunning = false;
            }
        }
        UIUtils.asyncExec(UIExecutionQueue::executeInUI);
    }

}
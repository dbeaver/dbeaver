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
package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;

public class TaskProcessorUI implements DBRRunnableContext, DBTTaskExecutionListener {
    private static final Log log = Log.getLog(TaskProcessorUI.class);

    @NotNull
    private DBTTask task;
    @NotNull
    private DBRRunnableContext staticContext;
    private long startTime;
    private boolean started;

    public TaskProcessorUI(@NotNull DBRRunnableContext staticContext, @NotNull DBTTask task) {
        this.staticContext = staticContext;
        this.task = task;
    }

    protected void runTask() throws DBException {
        throw new DBException("Empty task execute implementation");
    }

    protected boolean isShowFinalMessage() {
        return true;
    }

    @NotNull
    public DBTTask getTask() {
        return task;
    }

    public void executeTask() throws DBException {
        runTask();
    }

    @Override
    public void taskStarted(@NotNull Object task) {
        this.started = true;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void taskFinished(@NotNull Object task, @Nullable Throwable error) {
        this.started = false;

        long elapsedTime = System.currentTimeMillis() - startTime;

        UIUtils.asyncExec(() -> {
            // Make a sound
            Display.getCurrent().beep();
            // Notify agent
            boolean hasErrors = error != null;
            DBPPlatformUI platformUI = DBWorkbench.getPlatformUI();
            String completeMessage = this.task.getType().getName() + " task completed (" + RuntimeUtils.formatExecutionTime(elapsedTime) + ")";
            if (elapsedTime > platformUI.getLongOperationTimeout() * 1000) {
                platformUI.notifyAgent(
                    completeMessage, !hasErrors ? IStatus.INFO : IStatus.ERROR);
            }
            if (isShowFinalMessage() && !hasErrors) {
                // Show message box
                DBeaverNotifications.showNotification(
                    "task",
                    this.task.getName(),
                    completeMessage,
                    DBPMessageType.INFORMATION,
                    null);
            } else if (error != null) {
                DBWorkbench.getPlatformUI().showError("Task error", "Task execution failed", error);
            }
        });

    }

    @Override
    public void subTaskFinished(@Nullable Throwable error) {

    }

    @Override
    public void run(boolean fork, boolean cancelable, DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        staticContext.run(fork, cancelable, runnable);
    }

}
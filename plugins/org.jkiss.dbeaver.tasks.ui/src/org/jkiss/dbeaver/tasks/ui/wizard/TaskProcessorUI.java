/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.runtime.ui.UIServiceSystemAgent;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolSettings;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class TaskProcessorUI implements DBRRunnableContext, DBTTaskExecutionListener {
    private static final Log log = Log.getLog(TaskProcessorUI.class);

    @NotNull
    private DBTTask task;
    @NotNull
    private DBRRunnableContext staticContext;
    private long startTime;
    private boolean started;
    private long timeSincePreviousTask;

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
    public void taskStarted(@Nullable DBTTask task) {
        this.started = true;
        this.startTime = System.currentTimeMillis();
        this.timeSincePreviousTask = startTime;
    }

    @Override
    public void taskFinished(@Nullable DBTTask task, @Nullable Object result, @Nullable Throwable error, @Nullable Object settings) {
        this.started = false;

        long elapsedTime = System.currentTimeMillis() - startTime;

        sendNotification(task, error, elapsedTime, settings);

    }

    private void sendNotification(@Nullable DBTTask task, @Nullable Throwable error, long elapsedTime, @Nullable Object settings) {
        UIUtils.asyncExec(() -> {
            boolean hasErrors = error != null;

            StringBuilder completeMessage = new StringBuilder();
            completeMessage.append(task == null ? this.task.getType().getName() : task.getType().getName()).append(" ").append(TaskUIMessages.task_processor_ui_message_task_completed).append(" (").append(RuntimeUtils.formatExecutionTime(elapsedTime)).append(")");
            List<String> objects = new ArrayList<>();
            if (settings instanceof AbstractNativeToolSettings) {
                for (DBSObject databaseObject : ((AbstractNativeToolSettings<?>) settings).getDatabaseObjects()) {
                    objects.add(databaseObject.getName());
                }
                completeMessage.append("\nObject(s) processed: ").append(String.join(",", objects));
            }
            UIServiceSystemAgent serviceSystemAgent = DBWorkbench.getService(UIServiceSystemAgent.class);
            if (serviceSystemAgent != null && elapsedTime > serviceSystemAgent.getLongOperationTimeout() * 1000) {
                serviceSystemAgent.notifyAgent(
                    completeMessage.toString(), !hasErrors ? IStatus.INFO : IStatus.ERROR);
            }

            if (isShowFinalMessage() && !hasErrors) {
                DBeaverNotifications.showNotification(
                    "task.execute.success",
                    task == null ? this.task.getName() : task.getName(),
                    completeMessage.toString(),
                    DBPMessageType.INFORMATION,
                    null);
            } else if (error != null && !(error instanceof InterruptedException)) { ;
                DBeaverNotifications.showNotification(
                    "task.execute.failure",
                    task == null ? this.task.getName() : task.getName(),
                    error.getMessage(),
                    DBPMessageType.ERROR,
                    () -> DBWorkbench.getPlatformUI().showError("Task error", "Task execution failed", error)
                );
            }
        });
    }


    @Override
    public void subTaskFinished(@Nullable DBTTask task, @Nullable Throwable error, @Nullable Object settings) {
        long elapsedTime = System.currentTimeMillis() - timeSincePreviousTask;
        timeSincePreviousTask = System.currentTimeMillis();
        sendNotification(task, error, elapsedTime, settings);

    }

    @Override
    public void run(boolean fork, boolean cancelable, DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        staticContext.run(fork, cancelable, runnable);
    }

}
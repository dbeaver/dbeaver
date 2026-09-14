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
package org.jkiss.dbeaver.model.qm;

import org.eclipse.core.runtime.OperationCanceledException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.qm.meta.QMMConnectionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMProjectInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTaskInfo;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.DBTTask;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.function.Consumer;

/** Records one execution, independently of task notification callbacks. */
public final class QMTaskExecution implements AutoCloseable {
    private static final Log log = Log.getLog(QMTaskExecution.class);

    private final QMMTaskInfo started;
    private final Consumer<QMMTaskInfo> publisher;
    private boolean failed;
    private boolean canceled;
    private boolean finished;

    public QMTaskExecution(@NotNull DBTTask task) {
        this(createSnapshot(task), event -> {
            if (event.isClosed()) {
                QMUtils.getDefaultHandler().handleTaskEnd(task.getProject(), event);
            } else {
                QMUtils.getDefaultHandler().handleTaskBegin(task.getProject(), event);
            }
        });
    }

    QMTaskExecution(@NotNull QMMTaskInfo started, @NotNull Consumer<QMMTaskInfo> publisher) {
        this.started = started;
        this.publisher = publisher;
        publish(started);
    }

    @NotNull
    private static QMMTaskInfo createSnapshot(@NotNull DBTTask task) {
        long now = System.currentTimeMillis();
        QMMProjectInfo project = new QMMProjectInfo(task.getProject());
        // Task lifetime is independent of any physical database connection.
        QMMConnectionInfo connection = QMMConnectionInfo.builder()
            .setProjectInfo(project)
            .setContainerId(QMMTaskInfo.TASK_DATASOURCE_PREFIX + project.getUuid())
            .setContainerName("Task execution")
            .setDriverId("")
            .setInstanceId("")
            .setContextName("tasks")
            .setOpenTime(now)
            .build();
        return new QMMTaskInfo(
            UUID.randomUUID().toString(), task.getName(), task.getType().getId(), task.getType().getName(),
            connection, now, 0, QMMTaskInfo.Status.STARTED);
    }

    public synchronized void recordError(@Nullable Throwable error) {
        failed |= error != null;
    }

    public synchronized void recordCancellation(boolean canceled) {
        this.canceled |= canceled;
    }

    /** Observes errors and cancellation before handlers can catch or discard them. */
    public void run(
        @NotNull DBRRunnableContext context,
        boolean fork,
        boolean cancelable,
        @NotNull DBRRunnableWithProgress runnable
    ) throws InvocationTargetException, InterruptedException {
        try {
            context.run(fork, cancelable, monitor -> {
                try {
                    runnable.run(monitor);
                } finally {
                    recordCancellation(monitor.isCanceled());
                }
            });
        } catch (InterruptedException | OperationCanceledException e) {
            recordCancellation(true);
            throw e;
        } catch (InvocationTargetException e) {
            recordError(e.getTargetException());
            throw e;
        } catch (RuntimeException | Error e) {
            recordError(e);
            throw e;
        }
    }

    @Override
    public synchronized void close() {
        if (!finished) {
            finished = true;
            publish(started.finished(System.currentTimeMillis(), canceled ? QMMTaskInfo.Status.CANCELED :
                failed ? QMMTaskInfo.Status.FAILED : QMMTaskInfo.Status.SUCCESS));
        }
    }

    private void publish(@NotNull QMMTaskInfo event) {
        try {
            publisher.accept(event);
        } catch (Exception e) {
            log.debug("Error recording task execution", e);
        }
    }
}

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
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskRun;
import org.jkiss.dbeaver.model.task.DBTTaskRunRecord;
import org.jkiss.dbeaver.model.task.DBTTaskRunStorage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.function.Consumer;

/** Records one execution, independently of task notification callbacks. */
public final class QMTaskExecution implements AutoCloseable {
    private static final Log log = Log.getLog(QMTaskExecution.class);

    private final DBTTaskRunRecord started;
    private final Consumer<DBTTaskRunRecord> publisher;
    private boolean failed;
    private boolean canceled;
    private boolean finished;

    public QMTaskExecution(@NotNull DBTTask task) {
        this(task, null);
    }

    /** Reuses a managed run's identity so history and its file-based log remain associated. */
    public QMTaskExecution(@NotNull DBTTask task, @Nullable DBTTaskRun run) {
        this(createSnapshot(task, run), createPublisher());
    }

    private QMTaskExecution(@NotNull DBTTaskRunRecord started, @NotNull Consumer<DBTTaskRunRecord> publisher) {
        this.started = started;
        this.publisher = publisher;
        publish(started);
    }

    @NotNull
    private static DBTTaskRunRecord createSnapshot(@NotNull DBTTask task, @Nullable DBTTaskRun run) {
        String id = run == null ? UUID.randomUUID().toString() : run.getId();
        return new DBTTaskRunRecord(
            id, task.getId(), task.getName(), task.getType().getId(), task.getType().getName(),
            task.getProject().getId(), task.getProject().getName(),
            run == null ? System.currentTimeMillis() : run.getStartTime().getTime(), 0,
            run == null ? System.getProperty(StandardConstants.ENV_USER_NAME, "") : run.getStartUser(),
            run == null ? GeneralUtils.getProductTitle() : run.getStartedBy(), DBTTaskRunRecord.Status.RUNNING, run != null);
    }

    @NotNull
    private static Consumer<DBTTaskRunRecord> createPublisher() {
        try {
            DBTTaskRunStorage storage = DBTTaskRunStorage.getInstance();
            // Decide once per execution: turning recording off must not strand an existing RUNNING row,
            // and turning it back on must not create a completion-only row for an unrecorded execution.
            if (storage == null || !storage.isRecordingEnabled()) {
                return run -> { /* nothing to do */ };
            }
            return run -> {
                try {
                    storage.saveRun(run);
                } catch (Exception e) {
                    log.error("Error saving task execution " + run.id(), e);
                }
            };
        } catch (Exception e) {
            // History must not prevent a task from running if its storage cannot be initialized.
            log.error("Error initializing task history", e);
            return run -> { };
        }
    }

    public synchronized void recordError(@Nullable Throwable error) {
        if (error == null) {
            return;
        }
        // Completion callbacks may report cancellation without a canceled progress monitor.
        if (CommonUtils.getCauseOfType(error, InterruptedException.class) != null
            || CommonUtils.getCauseOfType(error, OperationCanceledException.class) != null
        ) {
            canceled = true;
        } else {
            failed = true;
        }
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
            publish(started.finished(System.currentTimeMillis(), canceled ? DBTTaskRunRecord.Status.CANCELED :
                failed ? DBTTaskRunRecord.Status.FAILED : DBTTaskRunRecord.Status.SUCCESS));
        }
    }

    private void publish(@NotNull DBTTaskRunRecord event) {
        try {
            publisher.accept(event);
        } catch (Exception e) {
            log.error("Error recording task execution", e);
        }
    }
}

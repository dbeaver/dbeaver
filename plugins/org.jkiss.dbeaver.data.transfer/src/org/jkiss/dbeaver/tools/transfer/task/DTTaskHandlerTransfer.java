/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.task;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.*;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * DTTaskHandlerTransfer
 */
public class DTTaskHandlerTransfer implements DBTTaskHandler {
    private static final Log log = Log.getLog(DTTaskHandlerTransfer.class);

    @Override
    public void executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull PrintStream logStream,
        @NotNull DBTTaskExecutionListener listener) throws DBException
    {
        DataTransferSettings[] settings = new DataTransferSettings[1];
        try {
            runnableContext.run(true, true, monitor -> {
                settings[0] = new DataTransferSettings(monitor, task, log, Collections.emptyMap(), new DataTransferState());
                settings[0].loadNodeSettings(monitor);
            });
        } catch (InvocationTargetException e) {
            throw new DBException("Error loading task settings", e.getTargetException());
        } catch (InterruptedException e) {
            return;
        }
        executeWithSettings(runnableContext, task, locale, log, listener, settings[0]);
    }

    public void executeWithSettings(@NotNull DBRRunnableContext runnableContext, DBTTask task, @NotNull Locale locale,
                                    @NotNull Log log, @NotNull DBTTaskExecutionListener listener,
                                    DataTransferSettings settings) throws DBException {
        listener.taskStarted(settings);
        int indexOfLastPipeWithDisabledReferentialIntegrity = -1;
        try {
            indexOfLastPipeWithDisabledReferentialIntegrity = initializePipes(runnableContext, settings);
            Throwable error = runDataTransferJobs(runnableContext, task, locale, log, listener, settings);
            listener.taskFinished(settings, null, error);
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                DTMessages.data_transfer_task_handler_unexpected_error_title,
                DTMessages.data_transfer_task_handler_unexpected_error_message,
                e.getCause()
            );
            throw new DBException("Error starting data transfer", e);
        } catch (InterruptedException e) {
            //ignore
        } finally {
            restoreReferentialIntegrity(
                runnableContext,
                settings.getDataPipes().subList(0, indexOfLastPipeWithDisabledReferentialIntegrity + 1)
            );
        }
    }

    private int initializePipes(@NotNull DBRRunnableContext runnableContext, @NotNull DataTransferSettings settings)
            throws InvocationTargetException, InterruptedException, DBException {
        int[] indexOfLastPipeWithDisabledReferentialIntegrity = new int[]{-1};
        DBException[] dbException = {null};
        List<DataTransferPipe> dataPipes = settings.getDataPipes();

        runnableContext.run(true, false, monitor -> {
            monitor.beginTask("Initialize pipes", dataPipes.size());
            try {
                for (int i = 0; i < dataPipes.size(); i++) {
                    DataTransferPipe pipe = dataPipes.get(i);
                    pipe.initPipe(settings, i, dataPipes.size());
                    IDataTransferConsumer<?, ?> consumer = pipe.getConsumer();
                    consumer.startTransfer(monitor);
                    if (enableReferentialIntegrity(consumer, monitor, false)) {
                        indexOfLastPipeWithDisabledReferentialIntegrity[0] = i;
                    }
                    monitor.worked(1);
                }
            } catch (DBException e) {
                dbException[0] = e;
            } finally {
                monitor.done();
            }
        });
        if (dbException[0] != null) {
            throw dbException[0];
        }

        return indexOfLastPipeWithDisabledReferentialIntegrity[0];
    }

    @Nullable
    private Throwable runDataTransferJobs(@NotNull DBRRunnableContext runnableContext, DBTTask task, @NotNull Locale locale,
                                     @NotNull Log log, @NotNull DBTTaskExecutionListener listener,
                                     @NotNull DataTransferSettings settings) {
        int totalJobs = settings.getDataPipes().size();
        if (totalJobs > settings.getMaxJobCount()) {
            totalJobs = settings.getMaxJobCount();
        }
        Throwable error = null;
        for (int i = 0; i < totalJobs; i++) {
            DataTransferJob job = new DataTransferJob(settings, task, locale, log, listener);
            try {
                runnableContext.run(true, true, job);
            } catch (InvocationTargetException e) {
                error = e.getTargetException();
            } catch (InterruptedException e) {
                break;
            }
            listener.subTaskFinished(error);
        }
        return error;
    }

    private void restoreReferentialIntegrity(@NotNull DBRRunnableContext runnableContext,
                                             @NotNull List<DataTransferPipe> pipes) throws DBException {
        DBException[] firstDBException = {null};
        try {
            runnableContext.run(true, false, monitor -> {
                try {
                    monitor.beginTask("Post transfer work", pipes.size());
                    for (DataTransferPipe pipe: pipes) {
                        try {
                            enableReferentialIntegrity(pipe.getConsumer(), monitor, true);
                        } catch (DBException e) {
                            log.debug("enabling referential integrity unexpectedly failed", e);
                            if (firstDBException[0] == null) {
                                firstDBException[0] = e;
                            }
                        }
                        monitor.worked(1);
                    }
                } finally {
                    monitor.done();
                }
            });
        } catch (InterruptedException e) {
            //ignore
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                DTMessages.data_transfer_task_handler_resoring_referential_integrity_unexpected_error_title,
                DTMessages.data_transfer_task_handler_resoring_referential_integrity_unexpected_error_message,
                e.getCause()
            );
        }
        if (firstDBException[0] != null) {
            throw new DBException("Unable to restore referential integrity properly", firstDBException[0]);
        }
    }

    private static boolean enableReferentialIntegrity(@NotNull IDataTransferConsumer<?, ?> consumer,
                                                   @NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        if (!(consumer instanceof DatabaseTransferConsumer)) {
            return false;
        }
        DatabaseTransferConsumer databaseTransferConsumer = (DatabaseTransferConsumer) consumer;
        DatabaseConsumerSettings settings = databaseTransferConsumer.getSettings();
        if (settings.isDisableReferentialIntegrity() && databaseTransferConsumer.supportsChangingReferentialIntegrity(monitor)) {
            databaseTransferConsumer.enableReferentialIntegrity(monitor, enable);
            return true;
        }
        return false;
    }
}

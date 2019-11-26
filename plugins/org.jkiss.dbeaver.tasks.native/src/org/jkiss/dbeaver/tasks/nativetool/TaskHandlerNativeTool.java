/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * TaskHandlerNativeTool
 */
public abstract class TaskHandlerNativeTool<BASE_OBJECT extends DBSObject, PROCESS_ARG> implements DBTTaskHandler {

    private boolean refreshObjects;
    private boolean isSuccess;

    ////////////////////////////////////
    // Methods to implement in real handlers

    public abstract Collection<PROCESS_ARG> getRunInfo(DBTTask task);

    protected abstract List<BASE_OBJECT> getDatabaseObjects(PROCESS_ARG runInfo);

    protected abstract List<String> getCommandLine(PROCESS_ARG arg) throws IOException;

    protected void setupProcessParameters(ProcessBuilder process) {
    }

    protected abstract void startProcessHandler(DBRProgressMonitor monitor, PROCESS_ARG arg, ProcessBuilder processBuilder, Process process);

    protected boolean isNativeClientHomeRequired() {
        return true;
    }

    protected boolean isMergeProcessStreams() {
        return false;
    }

    public boolean isVerbose() {
        return false;
    }

    protected boolean needsModelRefresh() {
        return true;
    }

    protected void onSuccess(long workTime) {

    }

    ////////////////////////////////////
    // Native tool executor

    @Override
    public void executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull DBTTaskExecutionListener listener) throws DBException
    {
        log.debug(task.getType().getName() + " initiated");

        try {
            runnableContext.run(true, true, monitor -> {
                Collection<PROCESS_ARG> runList = getRunInfo(task);
                try {
                    isSuccess = true;
                    for (PROCESS_ARG arg : runList) {
                        if (monitor.isCanceled()) break;
                        if (!executeProcess(monitor, task, log, arg)) {
                            isSuccess = false;
                        }
                    }
                } catch (Exception e){
                    throw new InvocationTargetException(e);
                }
                refreshObjects = isSuccess && !monitor.isCanceled();

                if (refreshObjects && needsModelRefresh()) {
                    // Refresh navigator node (script execution can change everything inside)
                    for (PROCESS_ARG runInfo : runList) {
                        for (BASE_OBJECT object : getDatabaseObjects(runInfo)) {
                            final DBNDatabaseNode node = DBWorkbench.getPlatform().getNavigatorModel().findNode(object);
                            if (node != null) {
                                try {
                                    node.refreshNode(monitor, TaskHandlerNativeTool.this);
                                } catch (DBException e) {
                                    log.debug("Error refreshing node '" + node.getNodeItemPath() + "' after native tool execution", e);
                                }
                            }
                        }
                    }
                }
            });
        } catch (InvocationTargetException e) {
            throw new DBException("Error executing native tool", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
            log.debug("Canceled");
        }

        log.debug(task.getType().getName() + " completed");
    }

    private boolean executeProcess(DBRProgressMonitor monitor, DBTTask task, Log log, PROCESS_ARG arg) throws IOException, DBException, InterruptedException {
        monitor.beginTask(task.getName(), 1);
        try {
            final List<String> commandLine = getCommandLine(arg);
            final File execPath = new File(commandLine.get(0));

            ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
            processBuilder.directory(execPath.getParentFile());
            if (this.isMergeProcessStreams()) {
                processBuilder.redirectErrorStream(true);
            }
            setupProcessParameters(processBuilder);
            Process process = processBuilder.start();

            startProcessHandler(monitor, arg, processBuilder, process);

            Thread.sleep(100);

            for (; ; ) {
                Thread.sleep(100);
                if (monitor.isCanceled()) {
                    process.destroy();
                }
                try {
                    final int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        log.error("Process exit code: " + exitCode);
                        return false;
                    }
                } catch (IllegalThreadStateException e) {
                    // Still running
                    continue;
                }
                break;
            }
            //process.waitFor();
        } catch (IOException e) {
            monitor.done();
            log.error(e);
            //logPage.appendLog(NLS.bind(TaskNativeUIMessages.tools_wizard_log_io_error, e.getMessage()) + "\n", true);
            return false;
        }

        return true;
    }

}

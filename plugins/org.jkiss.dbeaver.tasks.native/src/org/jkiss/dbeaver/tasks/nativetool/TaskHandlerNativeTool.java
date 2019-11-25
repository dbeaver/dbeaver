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

import org.jkiss.dbeaver.model.task.DBTTaskHandler;

/**
 * TaskHandlerNativeTool
 */
public abstract class TaskHandlerNativeTool implements DBTTaskHandler {

/*
    private boolean refreshObjects;
    private boolean isSuccess;

    @Override
    public void executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull DBTTaskExecutionListener listener) throws DBException
    {
        log.debug(task.getType().getName() + " initiated");

        runnableContext.run(true, true, monitor -> {
            isSuccess = true;
            for (PROCESS_ARG arg : getRunInfo()) {
                if (monitor.isCanceled()) break;
                if (!executeProcess(monitor, arg)) {
                    isSuccess = false;
                }
            }
            refreshObjects = isSuccess && !monitor.isCanceled();
        });

        log.debug(task.getType().getName() + " completed");
    }

    public boolean executeProcess(DBRProgressMonitor monitor, DBTTask task, Log log, PROCESS_ARG arg)
        throws IOException, CoreException, InterruptedException {
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
                        String errorMessage = NLS.bind(TaskNativeUIMessages.tools_wizard_log_process_exit_code, exitCode);
                        log.error(errorMessage);
                        //logPage.appendLog(errorMessage + "\n", true);
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
*/

}

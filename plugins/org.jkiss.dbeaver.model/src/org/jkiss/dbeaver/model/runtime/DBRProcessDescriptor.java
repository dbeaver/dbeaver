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

package org.jkiss.dbeaver.model.runtime;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DBRProcessDescriptor
 */
public class DBRProcessDescriptor {

    private static final Log log = Log.getLog(DBRProcessDescriptor.class);

    private final DBRShellCommand command;
    private final ProcessBuilder processBuilder;
    private volatile Process process;
    private volatile int exitValue = -1;
    private DBRProcessListener processListener;

    public DBRProcessDescriptor(@NotNull DBRShellCommand command) {
        this(command, null);
    }

    public DBRProcessDescriptor(@NotNull DBRShellCommand command, @Nullable IVariableResolver variablesResolver) {
        this.command = command;
        List<String> commandParams;
        if (command.getCommandParams() != null) {
            commandParams = command.getCommandParams();
        } else {
            String commandLine = CommonUtils.notEmpty(command.getCommand());
            List<String> rawParams = RuntimeUtils.splitCommandLine(commandLine, !RuntimeUtils.isWindows());
            if (variablesResolver != null) {
                commandParams = new ArrayList<>();
                for (String param : rawParams) {
                    commandParams.add(GeneralUtils.replaceVariables(param, variablesResolver));
                }
            } else {
                commandParams = rawParams;
            }
        }

        processBuilder = new ProcessBuilder(commandParams);
        // Set working directory
        if (!CommonUtils.isEmpty(command.getWorkingDirectory())) {
            processBuilder.directory(new File(command.getWorkingDirectory()));
        }
        //processBuilder.redirectErrorStream(true);
    }

    @NotNull
    public String getName() {
        final List<String> command = processBuilder.command();
        return command.isEmpty() ? "?" : command.getFirst();
    }

    @NotNull
    public DBRShellCommand getCommand() {
        return command;
    }

    @Nullable
    public DBRProcessListener getProcessListener() {
        return processListener;
    }

    public void setProcessListener(@Nullable DBRProcessListener processListener) {
        this.processListener = processListener;
    }

    @NotNull
    public ProcessBuilder getProcessBuilder() {
        return processBuilder;
    }

    @Nullable
    public Process getProcess() {
        return process;
    }

    public boolean isRunning() {
        Process theProcess = this.process;
        return theProcess != null && theProcess.isAlive();
    }

    public int getExitValue() {
        return exitValue;
    }

    public int getUpdatedExitValueCode() {
        var process = this.process;
        if (process == null) {
            log.debug("Process is not running");
            return exitValue;
        }
        try {
            exitValue = process.exitValue();
        } catch (IllegalThreadStateException e) {
            log.debug("Process still executing");
        }
        return exitValue;
    }

    public void execute() throws DBException {
        if (process != null) {
            throw new DBException("Process " + getName() + " already running");
        }
        if (CommonUtils.isEmpty(processBuilder.command())) {
            throw new DBException("Empty command specified");
        }
        try {
            this.process = processBuilder.start();
        } catch (IOException e) {
            throw new DBException("Can't start process", e);
        }
        if (processListener != null) {
            processListener.onProcessStarted();
        }
        if (this.command.getPauseAfterExecute() > 0) {
            try {
                Thread.sleep(this.command.getPauseAfterExecute());
            } catch (InterruptedException e) {
                // it's ok
            }
        }
    }

    public void terminate() {
        var process = this.process;
        if (process != null) {
            process.destroy();
            try {
                exitValue = process.waitFor();
            } catch (InterruptedException e) {
                // Skip
            }
            //exitValue = process.exitValue();
            this.process = null;
            if (processListener != null) {
                processListener.onProcessTerminated(exitValue);
            }
        }
    }

    public int waitFor() {
        return doWaitFor(false, Integer.MAX_VALUE);
    }

    public int waitFor(int timeoutMs) {
        return doWaitFor(true, timeoutMs);
    }

    private int doWaitFor(boolean useTimeout, int timeoutMs) {
        var process = this.process;
        if (process != null) {
            try {
                if (useTimeout) {
                    boolean exited = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                    if (exited) {
                        exitValue = process.exitValue();
                    }
                } else {
                    exitValue = process.waitFor();
                }
            } catch (InterruptedException e) {
                // Skip
            }
        }
        if (processListener != null && (process == null || !process.isAlive())) {
            processListener.onProcessTerminated(exitValue);
        }
        return exitValue;
    }

    @Nullable
    public String dumpErrors() {
        var process = this.process;
        if (process == null) {
            return null;
        }
        StringWriter buf = new StringWriter();
        try {
            InputStream inputStream = process.getErrorStream();
            if (inputStream != null) {
                // Note: do not close reader because it will close process error stream
                Reader input = new InputStreamReader(inputStream, GeneralUtils.getDefaultConsoleEncoding());
                IOUtils.copyText(input, buf);
            }
        } catch (IOException e) {
            e.printStackTrace(new PrintWriter(buf, true));
        }
        return buf.toString();
    }

    @Nullable
    public String dumpOutput() {
        var process = this.process;
        if (process == null) {
            return null;
        }
        StringWriter buf = new StringWriter();
        try {
            InputStream inputStream = process.getInputStream();
            if (inputStream != null) {
                // Note: do not close reader because it will close process error stream
                Reader input = new InputStreamReader(inputStream, GeneralUtils.getDefaultConsoleEncoding());
                IOUtils.copyText(input, buf);
            }
        } catch (IOException e) {
            e.printStackTrace(new PrintWriter(buf, true));
        }
        return buf.toString();
    }

}

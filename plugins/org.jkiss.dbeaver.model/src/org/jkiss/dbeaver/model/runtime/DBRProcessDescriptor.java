/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DBRProcessDescriptor
 */
public class DBRProcessDescriptor
{

    private final DBRShellCommand command;
    private ProcessBuilder processBuilder;
    private volatile Process process;
    private volatile int exitValue = -1;
    private DBRProcessListener processListener;

    public DBRProcessDescriptor(DBRShellCommand command)
    {
        this(command, null);
    }

    public DBRProcessDescriptor(final DBRShellCommand command, final GeneralUtils.IVariableResolver variablesResolver)
    {
        this.command = command;
        String commandLine = variablesResolver == null ?
            command.getCommand() :
            GeneralUtils.replaceVariables(command.getCommand(), variablesResolver);

        processBuilder = new ProcessBuilder(GeneralUtils.parseCommandLine(commandLine));
        // Set working directory
        if (!CommonUtils.isEmpty(command.getWorkingDirectory())) {
            processBuilder.directory(new File(command.getWorkingDirectory()));
        }
        //processBuilder.redirectErrorStream(true);
    }

    public String getName()
    {
        final List<String> command = processBuilder.command();
        return command.isEmpty() ? "?" : command.get(0);
    }

    public DBRShellCommand getCommand()
    {
        return command;
    }

    public DBRProcessListener getProcessListener() {
        return processListener;
    }

    public void setProcessListener(DBRProcessListener processListener) {
        this.processListener = processListener;
    }

    public ProcessBuilder getProcessBuilder()
    {
        return processBuilder;
    }

    public Process getProcess()
    {
        return process;
    }

    public synchronized boolean isRunning()
    {
        return process != null;
    }

    public int getExitValue()
    {
        return exitValue;
    }

    public void execute() throws DBException
    {
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

    public synchronized void terminate()
    {
        if (process != null) {
            process.destroy();
            try {
                exitValue = process.waitFor();
            } catch (InterruptedException e) {
                // Skip
            }
            //exitValue = process.exitValue();
            process = null;
            if (processListener != null) {
                processListener.onProcessTerminated(exitValue);
            }
        }
    }

    public int waitFor()
    {
        return doWaitFor(false, Integer.MAX_VALUE);
    }

    public int waitFor(int timeoutMs)
    {
        return doWaitFor(true, timeoutMs);
    }

    private int doWaitFor(boolean useTimeout, int timeoutMs)
    {
        if (this.process != null) {
            try {
                if (useTimeout) {
                    boolean exited = this.process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                    if (exited) {
                        exitValue = this.process.exitValue();
                    }
                } else {
                    exitValue = this.process.waitFor();
                }
            } catch (InterruptedException e) {
                // Skip
            }
        }
        if (processListener != null) {
            processListener.onProcessTerminated(exitValue);
        }
        return exitValue;
    }

    public String dumpErrors() {
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
}

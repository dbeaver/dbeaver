/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.model.runtime;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DBRProcessDescriptor
 */
public class DBRProcessDescriptor
{

    private final DBRShellCommand command;
    private ProcessBuilder processBuilder;
    private Process process;
    private int exitValue = -1;
    private DBRProcessListener processListener;

    public DBRProcessDescriptor(DBRShellCommand command)
    {
        this(command, null);
    }

    public DBRProcessDescriptor(final DBRShellCommand command, final Map<String, Object> variables)
    {
        this.command = command;
        String commandLine = variables == null ?
            command.getCommand() :
            GeneralUtils.replaceVariables(command.getCommand(), new GeneralUtils.MapResolver(variables));

        processBuilder = new ProcessBuilder(GeneralUtils.parseCommandLine(commandLine));
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
        if (this.process != null) {
            try {
                exitValue = this.process.waitFor();
            } catch (InterruptedException e) {
                // Skip
            }
        }
        if (processListener != null) {
            processListener.onProcessTerminated(exitValue);
        }
        return exitValue;
    }
}

/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.runtime;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.TextUtils;

import java.io.IOException;
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

    public DBRProcessDescriptor(DBRShellCommand command)
    {
        this(command, null);
    }

    public DBRProcessDescriptor(final DBRShellCommand command, final Map<String, Object> variables)
    {
        this.command = command;
        String commandLine = variables == null ?
            command.getCommand() :
            TextUtils.replaceVariables(command.getCommand(), variables);

        processBuilder = new ProcessBuilder(TextUtils.parseCommandLine(commandLine));
        //processBuilder.redirectErrorStream(true);
    }

    public String getName()
    {
        return processBuilder.command().get(0);
    }

    public DBRShellCommand getCommand()
    {
        return command;
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
        try {
            this.process = processBuilder.start();
        } catch (IOException e) {
            throw new DBException("Can't start process", e);
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
            DBRProcessPropertyTester.firePropertyChange(DBRProcessPropertyTester.PROP_RUNNING);
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
        return exitValue;
    }
}

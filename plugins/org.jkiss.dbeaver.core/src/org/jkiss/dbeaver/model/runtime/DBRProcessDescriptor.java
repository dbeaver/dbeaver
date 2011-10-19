/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
    private int exitValue;

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

    public void terminate()
    {
        if (process != null) {
            process.destroy();
            exitValue = process.exitValue();
            process = null;
        }
    }

}

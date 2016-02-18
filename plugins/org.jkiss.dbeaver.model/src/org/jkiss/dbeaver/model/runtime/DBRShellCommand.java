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

/**
 * DBRShellCommand
 */
public class DBRShellCommand
{

    private String command;
    private boolean enabled;
    private boolean showProcessPanel = true;
    private boolean waitProcessFinish;
    private boolean terminateAtDisconnect = true;
    private String workingDirectory;

    public DBRShellCommand(String command)
    {
        this.command = command;
    }

    public DBRShellCommand(DBRShellCommand command)
    {
        this.command = command.command;
        this.enabled = command.enabled;
        this.showProcessPanel = command.showProcessPanel;
        this.waitProcessFinish = command.waitProcessFinish;
        this.terminateAtDisconnect = command.terminateAtDisconnect;
    }

    public String getCommand()
    {
        return command;
    }

    public void setCommand(String command)
    {
        this.command = command;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isShowProcessPanel()
    {
        return showProcessPanel;
    }

    public void setShowProcessPanel(boolean showProcessPanel)
    {
        this.showProcessPanel = showProcessPanel;
    }

    public boolean isWaitProcessFinish()
    {
        return waitProcessFinish;
    }

    public void setWaitProcessFinish(boolean waitProcessFinish)
    {
        this.waitProcessFinish = waitProcessFinish;
    }

    public boolean isTerminateAtDisconnect()
    {
        return terminateAtDisconnect;
    }

    public void setTerminateAtDisconnect(boolean terminateAtDisconnect)
    {
        this.terminateAtDisconnect = terminateAtDisconnect;
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }
}

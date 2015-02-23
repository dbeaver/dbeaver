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

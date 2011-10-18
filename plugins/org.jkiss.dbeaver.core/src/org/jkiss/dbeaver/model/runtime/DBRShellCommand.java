/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.runtime;

/**
 * DBRShellCommand
 */
public class DBRShellCommand
{

    private String command;
    private boolean enabled;
    private boolean showProcessPanel;
    private boolean waitProcessFinish;
    private boolean terminateAtDisconnect;

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
}

/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * DBPConnectionEvent
 */
public class DBPConnectionEvent
{
    public static enum EventType {
        BEFORE_CONNECT,
        AFTER_CONNECT,
        BEFORE_DISCONNECT,
        AFTER_DISCONNECT,
    }

    private EventType eventType;
    private String command;
    private boolean showProcessPanel;
    private boolean terminateAtDisconnect;

    public DBPConnectionEvent(EventType eventType, String command)
    {
        this.eventType = eventType;
        this.command = command;
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public void setEventType(EventType eventType)
    {
        this.eventType = eventType;
    }

    public String getCommand()
    {
        return command;
    }

    public void setCommand(String command)
    {
        this.command = command;
    }

    public boolean isShowProcessPanel()
    {
        return showProcessPanel;
    }

    public void setShowProcessPanel(boolean showProcessPanel)
    {
        this.showProcessPanel = showProcessPanel;
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

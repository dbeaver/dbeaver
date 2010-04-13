/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.eclipse.core.runtime.Status;

import java.io.Serializable;

/**
 * DBeaverLogger
 */
public class DBeaverLogger implements Log, Serializable
{
    private String name;

    public DBeaverLogger()
    {
    }

    public DBeaverLogger(String name)
    {
        this();
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public boolean isDebugEnabled()
    {
        return true;
    }

    public boolean isErrorEnabled()
    {
        return true;
    }

    public boolean isFatalEnabled()
    {
        return true;
    }

    public boolean isInfoEnabled()
    {
        return true;
    }

    public boolean isTraceEnabled()
    {
        return false;
    }

    public boolean isWarnEnabled()
    {
        return true;
    }

    public void trace(Object message)
    {
    }

    public void trace(Object message, Throwable t)
    {
    }

    public void debug(Object message)
    {
    }

    public void debug(Object message, Throwable t)
    {
    }

    public void info(Object message)
    {
        DBeaverCore.getInstance().getPluginLog().log(new Status(
            Status.INFO,
            DBeaverCore.getInstance().getPluginID(),
            message == null ? null : message.toString()));
    }

    public void info(Object message, Throwable t)
    {
        DBeaverCore.getInstance().getPluginLog().log(new Status(
            Status.INFO,
            DBeaverCore.getInstance().getPluginID(),
            message == null ? null : message.toString(),
            t));
    }

    public void warn(Object message)
    {
        DBeaverCore.getInstance().getPluginLog().log(new Status(
            Status.WARNING,
            DBeaverCore.getInstance().getPluginID(),
            message == null ? null : message.toString()));
    }

    public void warn(Object message, Throwable t)
    {
        DBeaverCore.getInstance().getPluginLog().log(new Status(
            Status.WARNING,
            DBeaverCore.getInstance().getPluginID(),
            message == null ? null : message.toString(),
            t));
    }

    public void error(Object message)
    {
        DBeaverCore.getInstance().getPluginLog().log(new Status(
            Status.ERROR,
            DBeaverCore.getInstance().getPluginID(),
            message == null ? null : message.toString()));
    }

    public void error(Object message, Throwable t)
    {
        DBeaverCore.getInstance().getPluginLog().log(new Status(
            Status.ERROR,
            DBeaverCore.getInstance().getPluginID(),
            message == null ? null : message.toString(),
            t));
    }

    public void fatal(Object message)
    {
        error(message);
    }

    public void fatal(Object message, Throwable t)
    {
        error(message, t);
    }
}

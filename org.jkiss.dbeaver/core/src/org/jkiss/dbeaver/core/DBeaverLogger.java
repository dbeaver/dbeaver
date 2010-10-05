/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.*;
import java.util.Date;

/**
 * DBeaverLogger
 */
public class DBeaverLogger implements Log, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4079924783238318027L;
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
        if (message instanceof Throwable) {
            debug(message.toString(), (Throwable)message);
        } else {
            debug(message, null);
        }
    }

    public void debug(Object message, Throwable t)
    {
        PrintStream debugWriter = DBeaverActivator.getInstance().getDebugWriter();
        if (debugWriter != null) {
            synchronized (debugWriter) {
                debugWriter.print(new Date());
                debugWriter.print(" - ");
                if (t == null) {
                    debugWriter.println(message);
                } else {
                    t.printStackTrace(debugWriter);
                }
                debugWriter.flush();
            }
        }
    }

    public void info(Object message)
    {
        if (message instanceof Throwable) {
            info(message.toString(), (Throwable)message);
            return;
        }
        DBeaverCore.getInstance().getPluginLog().log(new Status(
            Status.INFO,
            DBeaverCore.getInstance().getPluginID(),
            message == null ? null : message.toString()));
    }

    public void info(Object message, Throwable t)
    {
        writeExceptionStatus(Status.INFO, message, t);
    }

    public void warn(Object message)
    {
        if (message instanceof Throwable) {
            warn(message.toString(), (Throwable)message);
            return;
        }
        DBeaverCore.getInstance().getPluginLog().log(new Status(
            Status.WARNING,
            DBeaverCore.getInstance().getPluginID(),
            message == null ? null : message.toString()));
    }

    public void warn(Object message, Throwable t)
    {
        writeExceptionStatus(Status.WARNING, message, t);
    }

    public void error(Object message)
    {
        if (message instanceof Throwable) {
            error(message.toString(), (Throwable)message);
            return;
        }
        DBeaverCore.getInstance().getPluginLog().log(new Status(
            Status.ERROR,
            DBeaverCore.getInstance().getPluginID(),
            message == null ? null : message.toString()));
    }

    public void error(Object message, Throwable t)
    {
        writeExceptionStatus(Status.ERROR, message, t);
    }

    public void fatal(Object message)
    {
        error(message);
    }

    public void fatal(Object message, Throwable t)
    {
        error(message, t);
    }

    private static void writeExceptionStatus(int severity, Object message, Throwable t)
    {
        if (t == null) {
            DBeaverCore.getInstance().getPluginLog().log(new Status(
                severity,
                DBeaverCore.getInstance().getPluginID(),
                message == null ? null : message.toString()));
        }
        DBeaverCore.getInstance().getPluginLog().log(new MultiStatus(
            DBeaverCore.getInstance().getPluginID(),
            0,
            new IStatus[]{ DBeaverUtils.makeExceptionStatus(severity, t) },
            message == null ? null : message.toString(),
            t));
    }

}

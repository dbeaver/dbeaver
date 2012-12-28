/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

import java.io.PrintStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
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
    private static String corePluginID;

    private String name;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public DBeaverLogger()
    {
        corePluginID = DBeaverActivator.getInstance().getBundle().getSymbolicName();
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

    @Override
    public boolean isDebugEnabled()
    {
        return true;
    }

    @Override
    public boolean isErrorEnabled()
    {
        return true;
    }

    @Override
    public boolean isFatalEnabled()
    {
        return true;
    }

    @Override
    public boolean isInfoEnabled()
    {
        return true;
    }

    @Override
    public boolean isTraceEnabled()
    {
        return false;
    }

    @Override
    public boolean isWarnEnabled()
    {
        return true;
    }

    @Override
    public void trace(Object message)
    {
    }

    @Override
    public void trace(Object message, Throwable t)
    {
    }

    @Override
    public void debug(Object message)
    {
        if (message instanceof Throwable) {
            debug(message.toString(), (Throwable)message);
        } else {
            debug(message, null);
        }
    }

    @Override
    public void debug(Object message, Throwable t)
    {
        DBeaverActivator activator = DBeaverActivator.getInstance();
        PrintStream debugWriter;
        if (activator == null) {
            debugWriter = System.err;
        } else {
            debugWriter = activator.getDebugWriter();
        }
        if (debugWriter != null) {
            synchronized (DBeaverLogger.class) {
                debugWriter.print(sdf.format(new Date()));
                debugWriter.print(" - "); //$NON-NLS-1$
                if (t == null) {
                    debugWriter.println(message);
                } else {
                    t.printStackTrace(debugWriter);
                }
                debugWriter.flush();
            }
        }
    }

    @Override
    public void info(Object message)
    {
        if (message instanceof Throwable) {
            info(message.toString(), (Throwable)message);
            return;
        }
        DBeaverActivator.getInstance().getLog().log(new Status(
            Status.INFO,
            corePluginID,
            message == null ? null : message.toString()));
    }

    @Override
    public void info(Object message, Throwable t)
    {
        writeExceptionStatus(Status.INFO, message, t);
    }

    @Override
    public void warn(Object message)
    {
        if (message instanceof Throwable) {
            warn(message.toString(), (Throwable)message);
            return;
        }
        DBeaverActivator.getInstance().getLog().log(new Status(
            Status.WARNING,
            corePluginID,
            message == null ? null : message.toString()));
    }

    @Override
    public void warn(Object message, Throwable t)
    {
        writeExceptionStatus(Status.WARNING, message, t);
    }

    @Override
    public void error(Object message)
    {
        if (message instanceof Throwable) {
            error(message.toString(), (Throwable)message);
            return;
        }
        DBeaverActivator.getInstance().getLog().log(new Status(
            Status.ERROR,
            corePluginID,
            message == null ? null : message.toString()));
    }

    @Override
    public void error(Object message, Throwable t)
    {
        writeExceptionStatus(Status.ERROR, message, t);
    }

    @Override
    public void fatal(Object message)
    {
        error(message);
    }

    @Override
    public void fatal(Object message, Throwable t)
    {
        error(message, t);
    }

    private static void writeExceptionStatus(int severity, Object message, Throwable t)
    {
        if (t == null) {
            DBeaverActivator.getInstance().getLog().log(new Status(
                severity,
                corePluginID,
                message == null ? null : message.toString()));
        } else {
            DBeaverActivator.getInstance().getLog().log(new MultiStatus(
                corePluginID,
                0,
                new IStatus[]{ RuntimeUtils.makeExceptionStatus(severity, t) },
                message == null ? null : message.toString(),
                t));
        }
    }

}

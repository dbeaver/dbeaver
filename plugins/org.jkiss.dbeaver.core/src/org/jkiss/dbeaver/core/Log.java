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
package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

import java.io.PrintStream;
import java.util.Date;

/**
 * Log
 */
public class Log
{
    private static String corePluginID = DBeaverActivator.getInstance().getBundle().getSymbolicName();

    private final String name;
    private final ILog eclipseLog;

    public static Log getLog(Class<?> forClass) {
        return new Log(forClass.getName());
    }

    private Log(String name)
    {
        eclipseLog = DBeaverActivator.getInstance().getLog();
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
        DBeaverActivator activator = DBeaverActivator.getInstance();
        PrintStream debugWriter;
        if (activator == null) {
            debugWriter = System.err;
        } else {
            debugWriter = activator.getDebugWriter();
        }
        if (debugWriter != null) {
            synchronized (Log.class) {
                debugWriter.print(new Date().toString());
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

    public void info(Object message)
    {
        if (message instanceof Throwable) {
            info(message.toString(), (Throwable)message);
            return;
        }
        eclipseLog.log(new Status(
            Status.INFO,
            corePluginID,
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
        DBeaverActivator.getInstance().getLog().log(new Status(
            Status.WARNING,
            corePluginID,
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
        DBeaverActivator.getInstance().getLog().log(new Status(
            Status.ERROR,
            corePluginID,
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
        DBeaverActivator activator = DBeaverActivator.getInstance();
        if (activator != null) {
            // Activator may be null in some unclear circumstances (like shutdown is in progress)
            ILog log = activator.getLog();
            if (log != null) {
                if (t == null) {
                    log.log(new Status(
                        severity,
                        corePluginID,
                        message == null ? null : message.toString()));
                } else {
                    log.log(RuntimeUtils.makeExceptionStatus(severity, t));
                }
            }
        }
    }

}

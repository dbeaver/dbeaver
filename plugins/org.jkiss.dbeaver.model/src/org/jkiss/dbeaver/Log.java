/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.bundle.ModelActivator;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Log
 */
public class Log
{
    private static String corePluginID = ModelPreferences.PLUGIN_ID;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-hh HH:mm:ss.SSS");
    private static final ILog eclipseLog = ModelActivator.getInstance().getLog();

    private final String name;

    public static Log getLog(Class<?> forClass) {
        return new Log(forClass.getName());
    }

    private Log(String name)
    {
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
        ModelActivator activator = ModelActivator.getInstance();
        debugMessage(message, t, System.err);
        if (activator != null) {
            debugMessage(message, t, activator.getDebugWriter());
        }
    }

    private static void debugMessage(Object message, Throwable t, PrintStream debugWriter) {
        synchronized (Log.class) {
            debugWriter.print(sdf.format(new Date()) + " - ");
            if (t == null) {
                debugWriter.println(message);
            } else {
                t.printStackTrace(debugWriter);
            }
            debugWriter.flush();
        }
    }

    public void info(Object message)
    {
        if (message instanceof Throwable) {
            info(message.toString(), (Throwable)message);
            return;
        }
        debugMessage(message, null, System.err);
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
        debugMessage(message, null, System.err);
        ModelActivator.getInstance().getLog().log(new Status(
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
        debugMessage(message, null, System.err);
        ModelActivator.getInstance().getLog().log(new Status(
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
        debugMessage(message, t, System.err);
        ModelActivator activator = ModelActivator.getInstance();
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
                    if (message == null) {
                        log.log(GeneralUtils.makeExceptionStatus(severity, t));
                    } else {
                        log.log(GeneralUtils.makeExceptionStatus(severity, message.toString(), t));
                    }
                }
            }
        }
    }

}

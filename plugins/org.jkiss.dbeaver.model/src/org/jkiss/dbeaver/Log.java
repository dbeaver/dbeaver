/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.bundle.ModelActivator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Log
 */
public class Log
{
    private static String corePluginID = ModelPreferences.PLUGIN_ID;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ILog eclipseLog = ModelActivator.getInstance().getLog();
    private static Listener[] listeners = new Listener[0];

    private final String name;

    public static ILog getEclipseLog() {
        return eclipseLog;
    }

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
//        if (activator != null) {
//            debugMessage(message, t, activator.getDebugWriter());
//        }
    }

    private static void debugMessage(Object message, Throwable t, PrintStream debugWriter) {
        synchronized (Log.class) {
            debugWriter.print(sdf.format(new Date()) + " - ");
            debugWriter.println(message);
            if (t != null) {
                t.printStackTrace(debugWriter);
            }
            debugWriter.flush();
            for (Listener listener : listeners) {
                listener.loggedMessage(message, t);
            }
        }
    }

    public void info(Object message)
    {
        if (message instanceof Throwable) {
            info(message.toString(), (Throwable) message);
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
            error(null, (Throwable)message);
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

    public static void addListener(Listener listener) {
        synchronized (Log.class) {
            listeners = ArrayUtils.add(Listener.class, listeners, listener);
        }
    }

    public static void removeListener(Listener listener) {
        synchronized (Log.class) {
            listeners = ArrayUtils.remove(Listener.class, listeners, listener);
        }
    }

    public static interface Listener {
        void loggedMessage(Object message, Throwable t);
    }
}

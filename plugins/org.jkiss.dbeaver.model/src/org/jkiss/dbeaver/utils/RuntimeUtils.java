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
package org.jkiss.dbeaver.utils;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * RuntimeUtils
 */
public class RuntimeUtils {
    private static final Log log = Log.getLog(RuntimeUtils.class);

    @SuppressWarnings("unchecked")
    public static <T> T getObjectAdapter(Object adapter, Class<T> objectType)
    {
        return Platform.getAdapterManager().getAdapter(adapter, objectType);
    }

    public static DBRProgressMonitor makeMonitor(IProgressMonitor monitor)
    {
        if (monitor instanceof DBRProgressMonitor) {
            return (DBRProgressMonitor) monitor;
        }
        return new DefaultProgressMonitor(monitor);
    }

    public static IProgressMonitor getNestedMonitor(DBRProgressMonitor monitor)
    {
        if (monitor instanceof IProgressMonitor) {
            return (IProgressMonitor) monitor;
        }
        return monitor.getNestedMonitor();
    }

    public static File getUserHomeDir()
    {
        String userHome = System.getProperty("user.home"); //$NON-NLS-1$
        if (userHome == null) {
            userHome = ".";
        }
        return new File(userHome);
    }

    public static String getCurrentDate()
    {
        return new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(new Date()); //$NON-NLS-1$
/*
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        final int month = c.get(Calendar.MONTH) + 1;
        final int day = c.get(Calendar.DAY_OF_MONTH);
        return "" + c.get(Calendar.YEAR) + (month < 10 ? "0" + month : month) + (day < 10 ? "0" + day : day);
*/
    }

    public static String getCurrentTimeStamp()
    {
        return new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH).format(new Date()); //$NON-NLS-1$
/*
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        final int month = c.get(Calendar.MONTH) + 1;
        return "" + c.get(Calendar.YEAR) + (month < 10 ? "0" + month : month) + c.get(Calendar.DAY_OF_MONTH) + c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE);
*/
    }

    public static boolean isTypeSupported(Class<?> type, Class[] supportedTypes)
    {
        if (type == null || ArrayUtils.isEmpty(supportedTypes)) {
            return false;
        }
        for (Class<?> tmp : supportedTypes) {
            if (tmp.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    public static String getNativeBinaryName(String binName)
    {
        return Platform.getOS().equals("win32") ? binName + ".exe" : binName;
    }

    @NotNull
    public static IStatus stripStack(@NotNull IStatus status) {
        if (status instanceof MultiStatus) {
            IStatus[] children = status.getChildren();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    children[i] = stripStack(children[i]);
                }
            }
            return new MultiStatus(status.getPlugin(), status.getCode(), children, status.getMessage(), null);
        } else if (status instanceof Status) {
            String messagePrefix = "";
            if (status.getException() != null) {
                messagePrefix = status.getException().getClass().getName() + ": ";
            }
            return new Status(status.getSeverity(), status.getPlugin(), status.getCode(), messagePrefix + status.getMessage(), null);
        }
        return status;
    }

    public static void pause(int ms)
    {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.warn("Sleep interrupted", e);
        }
    }

    public static String formatExecutionTime(long ms)
    {
        if (ms < 60000) {
            // Less than a minute, show just ms
            return String.valueOf(ms) + "ms";
        }
        long sec = ms / 1000;
        long min = sec / 60;
        sec -= min * 60;
        return String.valueOf(min) + " min " + String.valueOf(sec) + " sec";
    }

    public static File getPlatformFile(String platformURL) throws IOException
    {
        URL url = new URL(platformURL);
        URL fileURL = FileLocator.toFileURL(url);
        return getLocalFileFromURL(fileURL);

    }

    public static File getLocalFileFromURL(URL fileURL) throws IOException {
        // Escape spaces to avoid URI syntax error
        String filePath = fileURL.toString().replace(" ", "%20");
        try {
            return new File(new URI(filePath));
        } catch (URISyntaxException e) {
            throw new IOException("Bad local file path: " + filePath, e);
        }
    }

    public static boolean runTask(final DBRRunnableWithProgress task, String taskName, final long waitTime) {
        final MonitoringTask monitoringTask = new MonitoringTask(task);
        Job monitorJob = new AbstractJob(taskName) {
            @Override
            protected IStatus run(DBRProgressMonitor monitor)
            {
                try {
                    monitoringTask.run(monitor);
                } catch (InvocationTargetException e) {
                    log.error(getName() + " - error", e.getTargetException());
                    return Status.OK_STATUS;
                } catch (InterruptedException e) {
                    // do nothing
                }
                return Status.OK_STATUS;
            }
        };
        monitorJob.schedule();

        // Wait for job to finish
        long startTime = System.currentTimeMillis();
        if (waitTime > 0) {
            while (!monitoringTask.finished && System.currentTimeMillis() - startTime < waitTime) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        return monitoringTask.finished;
    }

    public static boolean isPlatformMacOS() {
        return Platform.getOS().toLowerCase().contains("macos");
    }

    public static boolean isPlatformWindows() {
        return Platform.getOS().toLowerCase().contains("win32");
    }

    public static void setThreadName(String name) {
        Thread.currentThread().setName("DBeaver: " + name);
    }

    private static class MonitoringTask implements DBRRunnableWithProgress {
        private final DBRRunnableWithProgress task;
        volatile boolean finished;

        private MonitoringTask(DBRRunnableWithProgress task) {
            this.task = task;
        }

        public boolean isFinished() {
            return finished;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
                task.run(monitor);
            } finally {
                monitor.done();
                finished = true;
            }
        }
    }

}

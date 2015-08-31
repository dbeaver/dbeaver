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
package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.program.Program;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.utils.GeneralUtils;
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
    static final Log log = Log.getLog(RuntimeUtils.class);

    @SuppressWarnings("unchecked")
    public static <T> T getObjectAdapter(Object adapter, Class<T> objectType)
    {
        return (T) Platform.getAdapterManager().getAdapter(adapter, objectType);
    }

    public static DBRProgressMonitor makeMonitor(IProgressMonitor monitor)
    {
        return new DefaultProgressMonitor(monitor);
    }

    public static void run(
        IRunnableContext runnableContext,
        boolean fork,
        boolean cancelable,
        final DBRRunnableWithProgress runnableWithProgress)
        throws InvocationTargetException, InterruptedException
    {
        runnableContext.run(fork, cancelable, new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                runnableWithProgress.run(makeMonitor(monitor));
            }
        });
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
        return new SimpleDateFormat("yyyyMMddhhmm", Locale.ENGLISH).format(new Date()); //$NON-NLS-1$
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
        return DBeaverCore.getInstance().getLocalSystem().isWindows() ? binName + ".exe" : binName;
    }

    public static IStatus stripStack(IStatus status) {
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
        return null;
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
            return String.valueOf(ms) + CoreMessages.controls_time_ms;
        }
        long sec = ms / 1000;
        long min = sec / 60;
        sec -= min * 60;
        return String.valueOf(min) + " min " + String.valueOf(sec) + " sec";
    }

    public static void launchProgram(String path)
    {
        Program.launch(path);
    }

    public static File getPlatformFile(String platformURL) throws IOException
    {
        URL url = new URL(platformURL);
        URL fileURL = FileLocator.toFileURL(url);
        return getFixedPlatformFile(fileURL);

    }

    public static File getFixedPlatformFile(URL fileURL) throws IOException {
        // Escape spaces to avoid URI syntax error
        String filePath = fileURL.toString().replace(" ", "%20");
        try {
            return new File(new URI(filePath));
        } catch (URISyntaxException e) {
            throw new IOException("Bad local file path: " + filePath, e);
        }
    }

    public static boolean runTask(final DBRRunnableWithProgress task, final long waitTime) {
        final MonitoringTask monitoringTask = new MonitoringTask(task);
        Job monitorJob = new AbstractJob("Disconnect from data sources") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor)
            {
                try {
                    monitoringTask.run(monitor);
                } catch (InvocationTargetException e) {
                    return GeneralUtils.makeExceptionStatus(e.getTargetException());
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

    public static <RESULT> LoadingJob<RESULT> createService(
        ILoadService<RESULT> loadingService,
        ILoadVisualizer<RESULT> visualizer)
    {
        return new LoadingJob<RESULT>(loadingService, visualizer);
    }

    public static boolean isPlatformMacOS() {
        return Platform.getOS().toLowerCase().contains("macos");
    }

    public static boolean isPlatformWindows() {
        return Platform.getOS().toLowerCase().contains("win32");
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

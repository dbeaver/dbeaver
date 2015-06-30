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

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.program.Program;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
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

    public static IStatus makeExceptionStatus(Throwable ex)
    {
        return makeExceptionStatus(IStatus.ERROR, ex);
    }

    public static IStatus makeExceptionStatus(int severity, Throwable ex)
    {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return new Status(
                severity,
                DBeaverCore.getCorePluginID(),
                getExceptionMessage(ex),
                ex);
        } else {
            if (ex instanceof DBException && CommonUtils.equalObjects(ex.getMessage(), cause.getMessage())) {
                // Skip empty duplicate DBException
                return makeExceptionStatus(cause);
            }
            return new MultiStatus(
                DBeaverCore.getCorePluginID(),
                0,
                new IStatus[]{makeExceptionStatus(severity, cause)},
                getExceptionMessage(ex),
                ex);
        }
    }

    public static IStatus makeExceptionStatus(String message, Throwable ex)
    {
        return new MultiStatus(
            DBeaverCore.getCorePluginID(),
            0,
            new IStatus[]{makeExceptionStatus(ex)},
            message,
            null);
    }

    public static IStatus getRootStatus(IStatus status) {
        IStatus[] children = status.getChildren();
        if (children == null || children.length == 0) {
            return status;
        } else {
            return getRootStatus(children[0]);
        }
    }

    public static String getStatusText(IStatus status) {
        String text = status.getMessage();
        IStatus[] children = status.getChildren();
        if (children != null && children.length > 0) {
            for (IStatus child : children) {
                text += "\n" + getStatusText(child);
            }
        }
        return text;
    }

    /**
     * Returns first non-null and non-empty message from this exception or it's cause
     */
    public static String getFirstMessage(Throwable ex)
    {
        for (Throwable e = ex; e != null; e = e.getCause()) {
            String message = e.getMessage();
            if (!CommonUtils.isEmpty(message)) {
                return message;
            }
        }
        return null;
    }

    public static String getExceptionMessage(Throwable ex)
    {
        StringBuilder msg = new StringBuilder(/*CommonUtils.getShortClassName(ex.getClass())*/);
        if (ex.getMessage() != null) {
            msg.append(ex.getMessage());
        } else {
            msg.append(ex.getClass().getSimpleName());
        }
        return msg.toString().trim();
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

    public static void savePreferenceStore(DBPPreferenceStore store)
    {
        try {
            store.save();
        } catch (IOException e) {
            log.warn(e);
        }
    }

    public static void setDefaultPreferenceValue(DBPPreferenceStore store, String name, Object value)
    {
        if (CommonUtils.isEmpty(store.getDefaultString(name))) {
            store.setDefault(name, value.toString());
        }
    }

    public static Object getPreferenceValue(DBPPreferenceStore store, String propName, Class<?> valueType)
    {
        try {
            if (valueType == null || CharSequence.class.isAssignableFrom(valueType)) {
                final String str = store.getString(propName);
                return CommonUtils.isEmpty(str) ? null : str;
            } else if (valueType == Boolean.class || valueType == Boolean.TYPE) {
                return store.getBoolean(propName);
            } else if (valueType == Long.class || valueType == Long.TYPE) {
                return store.getLong(propName);
            } else if (valueType == Integer.class || valueType == Integer.TYPE ||
                valueType == Short.class || valueType == Short.TYPE ||
                valueType == Byte.class || valueType == Byte.TYPE) {
                return store.getInt(propName);
            } else if (valueType == Double.class || valueType == Double.TYPE) {
                return store.getDouble(propName);
            } else if (valueType == Float.class || valueType == Float.TYPE) {
                return store.getFloat(propName);
            } else if (valueType == BigInteger.class) {
                final String str = store.getString(propName);
                return str == null ? null : new BigInteger(str);
            } else if (valueType == BigDecimal.class) {
                final String str = store.getString(propName);
                return str == null ? null : new BigDecimal(str);
            }
        } catch (RuntimeException e) {
            log.error(e);
        }
        final String string = store.getString(propName);
        return CommonUtils.isEmpty(string) ? null : string;
    }

    public static void setPreferenceValue(DBPPreferenceStore store, String propName, Object value)
    {
        if (value == null) {
            return;
        }
        if (value instanceof CharSequence) {
            store.setValue(propName, value.toString());
        } else if (value instanceof Boolean) {
            store.setValue(propName, (Boolean) value);
        } else if (value instanceof Long) {
            store.setValue(propName, (Long) value);
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            store.setValue(propName, ((Number) value).intValue());
        } else if (value instanceof Double) {
            store.setValue(propName, (Double) value);
        } else if (value instanceof Float) {
            store.setValue(propName, (Float) value);
        } else {
            store.setValue(propName, value.toString());
        }
    }

    public static void setPreferenceDefaultValue(DBPPreferenceStore store, String propName, Object value)
    {
        if (value == null) {
            return;
        }
        if (value instanceof CharSequence) {
            store.setDefault(propName, value.toString());
        } else if (value instanceof Boolean) {
            store.setDefault(propName, (Boolean) value);
        } else if (value instanceof Long) {
            store.setDefault(propName, (Long) value);
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            store.setDefault(propName, ((Number) value).intValue());
        } else if (value instanceof Double) {
            store.setDefault(propName, (Double) value);
        } else if (value instanceof Float) {
            store.setDefault(propName, (Float) value);
        } else {
            store.setDefault(propName, value.toString());
        }
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
                    return RuntimeUtils.makeExceptionStatus(e.getTargetException());
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

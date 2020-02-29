/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.utils;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.*;
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
    public static <T> T getObjectAdapter(Object adapter, Class<T> objectType) {
        return Platform.getAdapterManager().getAdapter(adapter, objectType);
    }

    public static DBRProgressMonitor makeMonitor(IProgressMonitor monitor) {
        if (monitor instanceof DBRProgressMonitor) {
            return (DBRProgressMonitor) monitor;
        }
        return new DefaultProgressMonitor(monitor);
    }

    public static IProgressMonitor getNestedMonitor(DBRProgressMonitor monitor) {
        if (monitor instanceof IProgressMonitor) {
            return (IProgressMonitor) monitor;
        }
        return monitor.getNestedMonitor();
    }

    public static File getUserHomeDir() {
        String userHome = System.getProperty(StandardConstants.ENV_USER_HOME); //$NON-NLS-1$
        if (userHome == null) {
            userHome = ".";
        }
        return new File(userHome);
    }

    public static String getCurrentDate() {
        return new SimpleDateFormat(GeneralUtils.DEFAULT_DATE_PATTERN, Locale.ENGLISH).format(new Date()); //$NON-NLS-1$
/*
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        final int month = c.get(Calendar.MONTH) + 1;
        final int day = c.get(Calendar.DAY_OF_MONTH);
        return "" + c.get(Calendar.YEAR) + (month < 10 ? "0" + month : month) + (day < 10 ? "0" + day : day);
*/
    }

    public static String getCurrentTimeStamp() {
        return new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.ENGLISH).format(new Date()); //$NON-NLS-1$
/*
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        final int month = c.get(Calendar.MONTH) + 1;
        return "" + c.get(Calendar.YEAR) + (month < 10 ? "0" + month : month) + c.get(Calendar.DAY_OF_MONTH) + c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE);
*/
    }

    public static boolean isTypeSupported(Class<?> type, Class[] supportedTypes) {
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

    public static String getNativeBinaryName(String binName) {
        return GeneralUtils.isWindows() ? binName + ".exe" : binName;
    }

    public static File getNativeClientBinary(@NotNull DBPNativeClientLocation home, @Nullable String binFolder, @NotNull String binName) throws IOException {
        binName = getNativeBinaryName(binName);
        File dumpBinary = new File(home.getPath(),
            binFolder == null ? binName : binFolder + "/" + binName);
        if (!dumpBinary.exists()) {
            dumpBinary = new File(home.getPath(), binName);
            if (!dumpBinary.exists()) {
                throw new IOException("Utility '" + binName + "' not found in client home '" + home.getDisplayName() + "' (" + home.getPath().getAbsolutePath() + ")");
            }
        }
        return dumpBinary;
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
            if (status.getException() != null && (CommonUtils.isEmpty(status.getException().getMessage()))) {
                messagePrefix = status.getException().getClass().getName() + ": ";
                return new Status(status.getSeverity(), status.getPlugin(), status.getCode(), messagePrefix + status.getMessage(), null);
            } else {
                return status;
            }
        }
        return status;
    }

    public static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.warn("Sleep interrupted", e);
        }
    }

    public static String formatExecutionTime(long ms) {
        if (ms < 1000) {
            // Less than a second, show just ms
            return String.valueOf(ms) + "ms";
        }
        if (ms < 60000) {
            // Less than a minute, show sec and ms
            return String.valueOf(ms / 1000) + "." + String.valueOf(ms % 1000) + "s";
        }
        long sec = ms / 1000;
        long min = sec / 60;
        sec -= min * 60;
        return String.valueOf(min) + "m " + String.valueOf(sec) + "s";
    }

    public static File getPlatformFile(String platformURL) throws IOException {
        URL url = new URL(platformURL);
        URL fileURL = FileLocator.toFileURL(url);
        return getLocalFileFromURL(fileURL);

    }

    public static File getLocalFileFromURL(URL fileURL) throws IOException {
        // Escape spaces to avoid URI syntax error
        try {
            URI filePath = GeneralUtils.makeURIFromFilePath(fileURL.toString());
            return new File(filePath);
        } catch (URISyntaxException e) {
            throw new IOException("Bad local file path: " + fileURL, e);
        }
    }

    public static boolean runTask(final DBRRunnableWithProgress task, String taskName, final long waitTime) {
        return runTask(task, taskName, waitTime, false);
    }

    public static boolean runTask(final DBRRunnableWithProgress task, String taskName, final long waitTime, boolean hidden) {
        final MonitoringTask monitoringTask = new MonitoringTask(task);
        Job monitorJob = new AbstractJob(taskName) {
            {
                if (hidden) {
                    setSystem(true);
                    setUser(false);
                }
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
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
        while (!monitoringTask.finished) {
            if (waitTime > 0 && System.currentTimeMillis() - startTime > waitTime) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                log.debug("Task '" + taskName + "' was interrupted");
                break;
            }
        }

        return monitoringTask.finished;
    }

    public static String executeProcess(String binPath, String ... args) throws DBException {
        try {
            String[] cmdBin = {binPath};
            String[] cmd = args == null ? cmdBin : ArrayUtils.concatArrays(cmdBin, args);
            Process p = Runtime.getRuntime().exec(cmd);
            try {
                StringBuilder out = new StringBuilder();
                readStringToBuffer(p.getInputStream(), out);

                if (out.length() == 0) {
                    StringBuilder err = new StringBuilder();
                    readStringToBuffer(p.getErrorStream(), err);
                    return err.toString();
                }

                return out.length() == 0 ? null: out.toString();
            } finally {
                p.destroy();
            }
        }
        catch (Exception ex) {
            throw new DBException("Error executing process " + binPath, ex);
        }
    }

    private static void readStringToBuffer(InputStream is, StringBuilder out) throws IOException {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(is))) {
            for (;;) {
                String line = input.readLine();
                if (line == null) {
                    break;
                }
                if (out.length() > 0) {
                    out.append("\n");
                }
                out.append(line);
            }
        }
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
        private DBRProgressMonitor monitor;
        volatile boolean finished;

        private MonitoringTask(DBRRunnableWithProgress task) {
            this.task = task;
        }

        public boolean isFinished() {
            return finished;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            this.monitor = monitor;
            try {
                task.run(monitor);
            } finally {
                monitor.done();
                finished = true;
            }
        }
    }

}

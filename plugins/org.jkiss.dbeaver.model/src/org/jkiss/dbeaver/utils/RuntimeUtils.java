/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * RuntimeUtils
 */
public final class RuntimeUtils {
    private static final Log log = Log.getLog(RuntimeUtils.class);

    private static final boolean IS_WINDOWS = Platform.getOS().equals(Platform.OS_WIN32);
    private static final boolean IS_MACOS = Platform.getOS().equals(Platform.OS_MACOSX);
    private static final boolean IS_LINUX = Platform.getOS().equals(Platform.OS_LINUX);

    private static final boolean IS_GTK = Platform.getWS().equals(Platform.WS_GTK);

    private static final byte[] NULL_MAC_ADDRESS = new byte[] {0, 0, 0, 0, 0, 0};

    private RuntimeUtils() {
        //intentionally left blank
    }

    public static <T> T getObjectAdapter(Object adapter, Class<T> objectType) {
        return Platform.getAdapterManager().getAdapter(adapter, objectType);
    }

    public static <T> T getObjectAdapter(Object adapter, Class<T> objectType, boolean force) {
        IAdapterManager adapterManager = Platform.getAdapterManager();
        if (force) {
            adapterManager.loadAdapter(adapter, objectType.getName());
        }
        return adapterManager.getAdapter(adapter, objectType);
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

    public static boolean isTypeSupported(Class<?> type, Class<?>[] supportedTypes) {
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
        return isWindows() ? binName + ".exe" : binName;
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
            log.debug("Sleep interrupted", e);
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
            /*
                File can't accept URI with file authority in it. This created a problem for shared folders.
                see dbeaver#15117
             */
            if (filePath.getAuthority() != null) {
                return new File(filePath.getSchemeSpecificPart());
            }
            return new File(filePath);
        } catch (URISyntaxException e) {
            throw new IOException("Bad local file path: " + fileURL, e);
        }
    }

    public static java.nio.file.Path getLocalPathFromURL(URL fileURL) throws IOException {
        // Escape spaces to avoid URI syntax error
        try {
            URI filePath = GeneralUtils.makeURIFromFilePath(fileURL.toString());
            /*
                File can't accept URI with file authority in it. This created a problem for shared folders.
                see dbeaver#15117
             */
            if (filePath.getAuthority() != null) {
                return java.nio.file.Path.of(filePath.getSchemeSpecificPart());
            }
            return java.nio.file.Path.of(filePath);
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
                setSystem(hidden);
                setUser(!hidden);
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                monitor.beginTask(getName(), 1);
                try {
                    monitor.subTask("Execute task");
                    monitoringTask.run(monitor);
                } catch (InvocationTargetException e) {
                    log.error(getName() + " - error", e.getTargetException());
                    return Status.OK_STATUS;
                } catch (InterruptedException e) {
                    // do nothing
                } finally {
                    monitor.done();
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
                if (!DBWorkbench.getPlatformUI().readAndDispatchEvents()) {
                    Thread.sleep(50);
                }
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

    public static boolean isWindows() {
        return IS_WINDOWS;
    }


    /**
     * Checks if current application is shipped from Windows store
     * @return true if shipped from Windows store, false if not.
     */
    public static boolean isWindowsStoreApplication() {
        if (!IS_WINDOWS) {
            return false;
        }
        final String property = System.getProperty(DBConstants.IS_WINDOWS_STORE_APP);
        return property != null && property.equalsIgnoreCase("true");
    }

    public static boolean isMacOS() {
        return IS_MACOS;
    }

    public static boolean isLinux() {
        return IS_LINUX;
    }
    
    public static boolean isGtk() {
        return IS_GTK;
    }

    public static void setThreadName(String name) {
        Thread.currentThread().setName("DBeaver: " + name);
    }

    public static byte[] getLocalMacAddress() throws IOException {
        InetAddress localHost = InetAddress.getLocalHost();
        NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
        if (ni == null) {
            Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
            if (niEnum.hasMoreElements()) {
                ni = niEnum.nextElement();
            }
        }
        return ni == null ? NULL_MAC_ADDRESS : ni.getHardwareAddress();
    }

    /**
     * Splits command line string into a list of separate arguments,
     * respecting quoted strings and escaped characters, similar to
     * how terminals do that.
     *
     * @param input            input string to be split
     * @param escapesSupported whether escapes using {@code \} are supported or not
     * @return a list of separate, unquoted arguments
     */
    @NotNull
    public static List<String> splitCommandLine(@NotNull String input, boolean escapesSupported) {
        final List<String> arguments = new ArrayList<>();
        final StringBuilder argument = new StringBuilder();
        CommandLineState state = CommandLineState.NONE;
        boolean escaped = false;

        for (int index = 0; index < input.length(); index++) {
            final char ch = input.charAt(index);
            final char quote = state == CommandLineState.SINGLE_QUOTE ? '\'' : '"';

            if (escaped) {
                argument.append(ch);
                escaped = false;
                continue;
            }

            switch (state) {
                case NONE:
                case NORMAL:
                    if (ch == '\'') {
                        state = CommandLineState.SINGLE_QUOTE;
                    } else if (ch == '"') {
                        state = CommandLineState.DOUBLE_QUOTE;
                    } else {
                        if (ch == '\\' && escapesSupported) {
                            escaped = true;
                            state = CommandLineState.NORMAL;
                        } else if (!Character.isWhitespace(ch)) {
                            argument.append(ch);
                            state = CommandLineState.NORMAL;
                        } else if (state == CommandLineState.NORMAL) {
                            arguments.add(argument.toString());
                            argument.setLength(0);
                            state = CommandLineState.NONE;
                        }
                    }
                    break;
                case SINGLE_QUOTE:
                case DOUBLE_QUOTE:
                    if (ch == '\\' && escapesSupported) {
                        final char next = input.charAt(++index);
                        if (next != quote && next != '\\') {
                            argument.append(ch);
                        }
                        argument.append(next);
                    } else if (ch == quote) {
                        state = CommandLineState.NORMAL;
                        break;
                    } else {
                        argument.append(ch);
                    }
                    break;
                default:
                    break;
            }
        }

        if (escaped) {
            argument.append('\\');
            arguments.add(argument.toString());
        } else if (state != CommandLineState.NONE) {
            arguments.add(argument.toString());
        }

        return arguments;
    }

    @NotNull
    public static String getWorkingDirectory(String defaultWorkspaceLocation) {
        String osName = (System.getProperty("os.name")).toUpperCase();
        String workingDirectory;
        if (osName.contains("WIN")) {
            String appData = System.getenv("AppData");
            if (appData == null) {
                appData = System.getProperty("user.home");
            }
            workingDirectory = appData + "\\" + defaultWorkspaceLocation;
        } else if (osName.contains("MAC")) {
            workingDirectory = System.getProperty("user.home") + "/Library/" + defaultWorkspaceLocation;
        } else {
            // Linux
            String dataHome = System.getProperty("XDG_DATA_HOME");
            if (dataHome == null) {
                dataHome = System.getProperty("user.home") + "/.local/share";
            }
            String badWorkingDir = dataHome + "/." + defaultWorkspaceLocation;
            String goodWorkingDir = dataHome + "/" + defaultWorkspaceLocation;
            if (!new File(goodWorkingDir).exists() && new File(badWorkingDir).exists()) {
                // Let's use bad working dir if it exists (#6316)
                workingDirectory = badWorkingDir;
            } else {
                workingDirectory = goodWorkingDir;
            }
        }
        return workingDirectory;
    }

    private enum CommandLineState {
        NONE,
        NORMAL,
        SINGLE_QUOTE,
        DOUBLE_QUOTE
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
                finished = true;
            }
        }
    }
}

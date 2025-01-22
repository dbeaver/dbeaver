/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.core.internal.runtime.CommonMessages;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.meta.ComponentReference;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

/**
 * RuntimeUtils
 */
public final class RuntimeUtils {
    private static final Log log = Log.getLog(RuntimeUtils.class);

    private static final boolean IS_OS_ARCH_AARCH64;
    private static final boolean IS_OS_ARCH_AMD64;
    private static final boolean IS_LINUX;
    private static final boolean IS_MACOS;
    private static final boolean IS_WINDOWS;

    private static final boolean IS_GTK = Platform.getWS().equals(Platform.WS_GTK);

    private static final byte[] NULL_MAC_ADDRESS = new byte[] {0, 0, 0, 0, 0, 0};

    static {
        String arch = Platform.getOSArch();
        IS_OS_ARCH_AARCH64 = Platform.ARCH_AARCH64.equals(arch);
        IS_OS_ARCH_AMD64 = Platform.ARCH_X86_64.equals(arch);

        String os = Platform.getOS();
        IS_LINUX = Platform.OS_LINUX.equals(os);
        IS_MACOS = Platform.OS_MACOSX.equals(os);
        IS_WINDOWS = Platform.OS_WIN32.equals(os);
    }

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
        if (monitor instanceof DBRProgressMonitor monitor1) {
            return monitor1;
        }
        return new DefaultProgressMonitor(monitor);
    }

    public static IProgressMonitor getNestedMonitor(DBRProgressMonitor monitor) {
        if (monitor instanceof IProgressMonitor monitor1) {
            return monitor1;
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
    }

    public static String getCurrentTime() {
        return new SimpleDateFormat(GeneralUtils.DEFAULT_TIME_PATTERN, Locale.ENGLISH).format(new Date()); //$NON-NLS-1$
    }

    public static String getCurrentTimeStamp() {
        return new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.ENGLISH).format(new Date()); //$NON-NLS-1$
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
            for (int i = 0; i < children.length; i++) {
                children[i] = stripStack(children[i]);
            }
            return new MultiStatus(status.getPlugin(), status.getCode(), children, status.getMessage(), null);
        } else if (status instanceof Status) {
            String messagePrefix;
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
        return formatExecutionTime(Duration.ofMillis(ms));
    }

    @NotNull
    public static String formatExecutionTime(@NotNull Duration duration) {
        final long hours = duration.toHours();
        final int minutes = duration.toMinutesPart();
        final int seconds = duration.toSecondsPart();
        final int millis = duration.toMillisPart();

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else if (seconds >= 10) {
            return String.format("%ds", seconds);
        } else {
            return String.format("%d.%03ds", seconds, millis);
        }
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

        // Short pause. Eclipse 2023-12 seems to have an issue which locks readAndDispatchEvents
        pause(10);
        // Wait for job to finish
        boolean headlessMode = DBWorkbench.getPlatform().getApplication().isHeadlessMode();
        long startTime = System.currentTimeMillis();
        while (!monitoringTask.finished) {
            if (waitTime > 0 && System.currentTimeMillis() - startTime > waitTime) {
                break;
            }
            if (headlessMode || !DBWorkbench.getPlatformUI().readAndDispatchEvents()) {
                pause(50);
            }
        }

        return monitoringTask.finished;
    }

    public static String executeProcess(String binPath, String... args) throws DBException {
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

                return out.toString();
            } finally {
                p.destroy();
            }
        } catch (Exception ex) {
            throw new DBException("Error executing process " + binPath, ex);
        }
    }

    public static String executeProcessAndCheckResult(String binPath, String... args) throws DBException {
        try {
            String[] cmdBin = {binPath};
            String[] cmd = args == null ? cmdBin : ArrayUtils.concatArrays(cmdBin, args);
            Process p = Runtime.getRuntime().exec(cmd);
            return getProcessResults(p);
        } catch (Exception ex) {
            if (ex instanceof DBException dbe) {
                throw dbe;
            }
            throw new DBException("Error executing process " + binPath, ex);
        }
    }

    @NotNull
    public static String getProcessResults(Process p) throws IOException, InterruptedException, DBException {
        try {
            StringBuilder out = new StringBuilder();
            readStringToBuffer(p.getInputStream(), out);

            StringBuilder err = new StringBuilder();
            readStringToBuffer(p.getErrorStream(), err);

            p.waitFor();
            if (p.exitValue() != 0) {
                throw new DBException(err.toString());
            }

            return out.toString();
        } finally {
            p.destroy();
        }
    }

    private static void readStringToBuffer(InputStream is, StringBuilder out) throws IOException {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(is))) {
            for (; ; ) {
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
     *
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

    /**
     * Determines whether the <i>OS</i> ISA is AArch64.
     *
     * <p>Note that this method is designed to tell the <i>OS</i> ISA, not <i>JVM</i> ISA.
     *
     * @return {@code true} if the OS ISA is AArch64
     */
    public static boolean isOSArchAArch64() {
        return IS_OS_ARCH_AARCH64;
    }

    /**
     * Determines whether the <i>OS</i> ISA is AMD64.
     *
     * <p>Note that this method is designed to tell the <i>OS</i> ISA, not <i>JVM</i> ISA.
     *
     * @return {@code true} if the OS ISA is AMD64
     */
    public static boolean isOSArchAMD64() {
        return IS_OS_ARCH_AMD64;
    }

    /**
     * Retrieves version of the operating system.
     *
     * @return version of the operating system, or {@link Version#emptyVersion} if the version is unknown
     * @see #isOSVersionAtLeast(int, int, int)
     */
    @NotNull
    public static Version getOSVersion() {
        String version = System.getProperty("os.version");
        if (version != null) {
            try {
                return new Version(version);
            } catch (IllegalArgumentException e) {
                log.debug("Error parsing OS version: " + version, e);
            }
        }
        return Version.emptyVersion;
    }

    public static boolean isOSVersionAtLeast(int major, int minor, int micro) {
        Version expected = new Version(major, minor, micro);
        Version actual = getOSVersion();
        return actual.compareTo(expected) >= 0;
    }

    public static void setThreadName(String name) {
        Thread.currentThread().setName("DBeaver: " + name);
    }

    public static byte[] getLocalMacAddress() throws IOException {
        InetAddress localHost = getLocalHostOrLoopback();
        NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
        if (ni == null) {
            Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
            if (niEnum.hasMoreElements()) {
                ni = niEnum.nextElement();
            }
        }
        return ni == null || ni.getHardwareAddress() == null ? NULL_MAC_ADDRESS : ni.getHardwareAddress();
    }

    @NotNull
    public static InetAddress getLocalHostOrLoopback() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // dbeaver/pro#2157
            log.debug("Error resolving localhost address: " + e.getMessage());
            return InetAddress.getLoopbackAddress();
        }
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

    // Extraction from Eclipse source to support old and new API versions
    // Activator.getLocalization became static after 2023-09
    public static ResourceBundle getBundleLocalization(Bundle bundle, String locale) throws MissingResourceException {
        Activator activator = Activator.getDefault();
        if (activator == null) {
            throw new MissingResourceException(CommonMessages.activator_resourceBundleNotStarted,
                bundle.getSymbolicName(), ""); //$NON-NLS-1$
        }
        ServiceCaller<BundleLocalization> localizationTracker;
        try {
            localizationTracker = BeanUtils.getFieldValue(activator, "localizationTracker");
        } catch (Throwable e) {
            throw new MissingResourceException(
                "Resource bundle '" + bundle.getSymbolicName() + "' not found for locale " + locale,
                bundle.getSymbolicName(),
                e.getMessage());
        }
        BundleLocalization location = localizationTracker.current().orElse(null);
        ResourceBundle result = null;
        if (location != null)
            result = location.getLocalization(bundle, locale);
        if (result == null)
            throw new MissingResourceException(
                "Resource bundle '" + bundle.getSymbolicName() + "' not found for locale " + locale,
                bundle.getSymbolicName(),
                ""); //$NON-NLS-1$
        return result;
    }

    public static <T> void executeJobsForEach(List<T> objects, DBRRunnableParametrizedWithProgress<T> task) {
        JobGroup jobGroup = new JobGroup("executeJobsForEach:" + objects, 10, 1);
        for (T object : objects) {
            AbstractJob job = new AbstractJob("Execute for " + object) {
                {
                    setSystem(true);
                    setUser(false);
                }

                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    if (!monitor.isCanceled()) {
                        try {
                            task.run(monitor, object);
                        } catch (InvocationTargetException e) {
                            log.debug(e.getTargetException());
                        } catch (InterruptedException e) {
                            return Status.CANCEL_STATUS;
                        }
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setJobGroup(jobGroup);
            job.schedule();
        }
        try {
            jobGroup.join(0, new NullProgressMonitor());
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Nullable
    public static String getSystemPropertyIgnoreCase(@NotNull String key) {
        final Properties props = System.getProperties();

        for (String name : props.stringPropertyNames()) {
            if (name.equalsIgnoreCase(key)) {
                return props.getProperty(key);
            }
        }

        return null;
    }

    @Nullable
    public static String getSystemEnvIgnoreCase(@NotNull String key) {
        final Map<String, String> env = System.getenv();

        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static <T> T getBundleService(Class<T> theClass, boolean required) throws IllegalStateException {
        Bundle bundle = FrameworkUtil.getBundle(theClass);
        BundleContext bundleContext = bundle.getBundleContext();
        ServiceReference<T> serviceReference = bundleContext.getServiceReference(theClass);
        if (serviceReference == null) {
            if (required) {
                throw new IllegalStateException("Service '" + theClass.getName() + "' is not registered");
            }
            return null;
        }
        T service = bundleContext.getService(serviceReference);
        if (service == null) {
            if (required) {
                throw new IllegalStateException("Service '" + theClass.getName() + "' implementation not found");
            }
        } else {
            RuntimeUtils.injectComponentReferences(service);
        }

        return service;
    }

    public static void injectComponentReferences(Object object) {
        Class<?> aClass = object.getClass();
        for (Field field : aClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            ComponentReference refAnno = field.getAnnotation(ComponentReference.class);
            if (refAnno != null) {
                Class<?> serviceClass = refAnno.service();
                if (serviceClass == Object.class) {
                    serviceClass = field.getType();
                }
                try {
                    Object fieldValue = field.get(object);
                    if (fieldValue == null) {
                        Object bundleService = getBundleService(serviceClass, refAnno.required());
                        field.setAccessible(true);
                        field.set(object, bundleService);

                        if (bundleService != null && !CommonUtils.isEmpty(refAnno.postProcessMethod())) {
                            Method postProcessMethod = bundleService.getClass().getDeclaredMethod(refAnno.postProcessMethod());
                            postProcessMethod.setAccessible(true);
                            postProcessMethod.invoke(bundleService);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error injecting field '" + field.getName() + "' in '" + object + "'", e);
                }
            }
        }
    }

    // Returns plugin state folder and do not create it (as default Eclipse function does)
    public static Path getPluginStateLocation(Plugin plugin) {
        return InternalPlatform.getDefault().getStateLocation(plugin.getBundle(), false).toPath();
    }

    private enum CommandLineState {
        NONE,
        NORMAL,
        SINGLE_QUOTE,
        DOUBLE_QUOTE
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
                finished = true;
            }
        }
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.FrameworkUtil;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Log
 */
public class Log {

    public static class Context {
        private final String contextName;
        private final Map<String, Object> contextParameters = new LinkedHashMap<>();

        private Context(@NotNull String contextName) {
            this.contextName = contextName;
        }

        @NotNull
        public Context withParameter(@NotNull String name, @Nullable Object value) {
            this.contextParameters.put(name, value);
            return this;
        }

        @NotNull
        public String getContextName() {
            return contextName;
        }

        @NotNull
        public Map<String, Object> getContextParameters() {
            return contextParameters;
        }
    }

    @NotNull
    public static Context buildContext(@NotNull String name) {
        return new Context(name);
    }

    private static boolean TRACE_LOG_ENABLED = CommonUtils.getBoolean(System.getProperty("dbeaver.trace.enabled"));
    public static final boolean DEV_DEBUG_ENABLED = CommonUtils.getBoolean(System.getProperty("dbeaver.debug.enabled"));

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$

    @Nullable
    private static ILog eclipseLog;
    private static Listener[] listeners = new Listener[0];

    static {
        try {
            eclipseLog = Platform.getLog(FrameworkUtil.getBundle(Log.class));
        } catch (Throwable e) {
            eclipseLog = null;
        }

        quietMode = ArrayUtils.containsAny(
            Platform.getApplicationArgs(),
            "-q", "--q", "-quiet", "-h", "-help", "--help", "-version", "--version"
        );
    }

    private static final ThreadLocal<Context> activeContext = new ThreadLocal<>();

    private final String name;
    private static final ThreadLocal<PrintStream> logWriter = new ThreadLocal<>();
    private static final boolean quietMode;
    private final boolean doEclipseLog;

    @Nullable
    private static LogHandler handler;
    @Nullable
    private static PrintStream defaultDebugStream;

    public static void setLogHandler(@Nullable LogHandler handler) {
        Log.handler = handler;
    }

    public static void enableTraceLogs(boolean enable) {
        TRACE_LOG_ENABLED = enable;
    }

    public static void setDefaultDebugStream(@NotNull PrintStream defaultDebugStream) {
        Log.defaultDebugStream = defaultDebugStream;
    }

    public static void setContext(@Nullable Context context) {
        activeContext.set(context);
    }

    @NotNull
    public static Log getLog(@NotNull Class<?> forClass) {
        return new Log(forClass.getName(), true);
    }

    @NotNull
    public static Log getLog(@NotNull String name) {
        return new Log(name, true);
    }

    @NotNull
    public static Log getLog(@NotNull String name, boolean doEclipseLog) {
        return new Log(name, doEclipseLog);
    }

    public static boolean isQuietMode() {
        return quietMode;
    }

    @Nullable
    public static PrintStream getLogWriter() {
        return logWriter.get();
    }

    public static void setLogWriter(@Nullable OutputStream logWriter) {
        if (logWriter == null) {
            Log.logWriter.remove();
        } else {
            if (logWriter instanceof PrintStream) {
                Log.logWriter.set((PrintStream) logWriter);
            } else {
                Log.logWriter.set(new PrintStream(logWriter, true));
            }
        }
    }

    public void log(@Nullable IStatus status) {
        if (status == null) {
            // nothing to log
            return;
        }
        int severity = status.getSeverity();
        String message = status.getMessage();
        Throwable exception = status.getException();
        switch (severity) {
            case IStatus.CANCEL:
                debug(message, exception);
                break;
            case IStatus.ERROR:
                error(message, exception);
                break;
            case IStatus.WARNING:
                warn(message, exception);
                break;
            case IStatus.INFO:
                info(message, exception);
                break;
            case IStatus.OK:
                trace(message, exception);
                break;
            default:
                break;
        }
    }

    private Log(@NotNull String name, boolean doEclipseLog) {
        this.name = name;
        this.doEclipseLog = doEclipseLog;
    }

    public void flush() {
        PrintStream logStream = logWriter.get();
        if (logStream != null) {
            logStream.flush();
        }
    }

    @NotNull
    public String getName() {
        return name;
    }

    public boolean isErrorEnabled() {
        return handler == null || handler.isErrorEnabled(name);
    }

    public boolean isFatalEnabled() {
        return handler == null || handler.isFatalEnabled(name);
    }

    public boolean isInfoEnabled() {
        return handler == null || handler.isInfoEnabled(name);
    }

    public boolean isWarnEnabled() {
        return handler == null || handler.isWarnEnabled(name);
    }

    public boolean isDebugEnabled() {
        return handler != null && handler.isDebugEnabled(name);
    }

    public boolean isTraceEnabled() {
        return handler != null && handler.isTraceEnabled(name);
    }


    public void trace(@Nullable Object message) {
        if (message instanceof Throwable) {
            trace(message.toString(), (Throwable) message);
        } else {
            trace(message, null);
        }
    }

    public void trace(@Nullable Object message, @Nullable Throwable t) {
        if (handler != null) {
            handler.trace(name, message, t);
            if (getLogWriter() == null) {
                return;
            }
        }
        if (TRACE_LOG_ENABLED) {
            debug(message, t);
        }
    }

    public void debug(@Nullable Object message) {
        if (message instanceof Throwable) {
            debug(message.toString(), (Throwable) message);
        } else {
            debug(message, null);
        }
    }

    public void debug(@Nullable Object message, @Nullable Throwable t) {
        if (handler != null) {
            handler.debug(name, message, t);
            if (getLogWriter() == null) {
                synchronized (Log.class) {
                    notifyListeners(message, t, true);
                }
                return;
            }
        }
        debugMessage(message, t);
    }

    private void debugMessage(@Nullable Object message, @Nullable Throwable t) {
        debugMessage(message, t, true);
    }

    private void debugMessage(@Nullable Object message, @Nullable Throwable t, boolean notifyUI) {
        PrintStream logStream = logWriter.get();
        synchronized (Log.class) {
            PrintStream debugWriter = logStream != null ? logStream : (quietMode ? null : defaultDebugStream);
            if (debugWriter == null && !quietMode) {
                debugWriter = System.err;
            }
            if (debugWriter == null) {
                return;
            }

            Context context = activeContext.get();
            if (context != null) {
                debugWriter.print("[" + context.getContextName() + "] ");
            }

            debugWriter.print(sdf.format(new Date()) + " - "); //$NON-NLS-1$
            if (message != null) {
                debugWriter.println(message);
            }
            if (t != null) {
                t.printStackTrace(debugWriter);
            }
            if (message == null && t == null) {
                debugWriter.println();
            }
            debugWriter.flush();
            notifyListeners(message, t, notifyUI);
        }
        if (t != null) {
            // Log nested exceptions
            for (Throwable ex = t; ex != null; ex = ex.getCause()) {
                if (ex instanceof SQLException) {
                    // Log all chained SQL exceptions
                    for (SQLException error = ((SQLException) ex).getNextException(); error != null; error = error.getNextException()) {
                        String chainedMessage = error.getMessage();
                        if (!CommonUtils.isEmpty(chainedMessage)) {
                            debug(chainedMessage.trim());
                        }
                    }
                }
            }
        }
    }

    public void info(@Nullable Object message) {
        if (message instanceof Throwable) {
            info(message.toString(), (Throwable) message);
            return;
        }
        if (handler != null) {
            handler.info(name, message);
            if (getLogWriter() == null) {
                return;
            }
        }
        debugMessage(message, null, false);
        int severity = Status.INFO;
        writeEclipseLog(createStatus(severity, message));
    }

    public void info(@Nullable Object message, @Nullable Throwable t) {
        if (handler != null) {
            handler.info(name, message, t);
            if (getLogWriter() == null) {
                return;
            }
        }
        writeExceptionStatus(Status.INFO, message, t);
    }

    public void warn(@Nullable Object message) {
        if (message instanceof Throwable) {
            warn(message.toString(), (Throwable) message);
            return;
        }
        if (handler != null) {
            handler.warn(name, message);
            if (getLogWriter() == null) {
                return;
            }
        }
        debugMessage(message, null, false);
        int severity = Status.WARNING;
        writeEclipseLog(createStatus(severity, message));
    }

    public void warn(@Nullable Object message, @Nullable Throwable t) {
        if (handler != null) {
            handler.warn(name, message, t);
            if (getLogWriter() == null) {
                return;
            }
        }
        writeExceptionStatus(Status.WARNING, message, t);
    }

    public void error(@Nullable Object message) {
        if (message == null) {
            return;
        }
        if (message instanceof Throwable) {
            error(null, (Throwable) message);
            return;
        }
        if (handler != null) {
            handler.error(name, message);
            if (getLogWriter() == null) {
                return;
            }
        }
        debugMessage(message, null, false);
        int severity = Status.ERROR;
        writeEclipseLog(createStatus(severity, message));
    }

    public void error(@Nullable Object message, @Nullable Throwable t) {
        if (handler != null) {
            handler.error(name, message, t);
            if (getLogWriter() == null) {
                return;
            }
        }
        writeExceptionStatus(Status.ERROR, message, t);
    }

    public void fatal(@Nullable Object message) {
        if (handler != null) {
            handler.fatal(name, message);
            if (getLogWriter() == null) {
                return;
            }
        }
        error(message);
    }

    public void fatal(@Nullable Object message, @Nullable Throwable t) {
        if (handler != null) {
            handler.fatal(name, message, t);
            if (getLogWriter() == null) {
                return;
            }
        }
        error(message, t);
    }

    private void writeExceptionStatus(int severity, @Nullable Object message, @Nullable Throwable t) {
        boolean logWritten = false;
        if (logWriter.get() == null) {
            if (t == null) {
                logWritten = writeEclipseLog(createStatus(severity, message));
            } else {
                if (message == null) {
                    logWritten = writeEclipseLog(GeneralUtils.makeExceptionStatus(severity, t));
                } else {
                    logWritten = writeEclipseLog(GeneralUtils.makeExceptionStatus(severity, message.toString(), t));
                }
            }
        }
        if (!logWritten) {
            debugMessage(message, t, false);
        }
    }

    private boolean writeEclipseLog(@NotNull IStatus status) {
        if (doEclipseLog && logWriter.get() == null && eclipseLog != null) {
            eclipseLog.log(status);
            return true;
        } else {
            return false;
        }
    }

    @NotNull
    private static Status createStatus(int severity, @Nullable Object message) {
        //we never include Exception to the status for some reason
        return new Status(
            severity,
            ModelPreferences.PLUGIN_ID,
            message == null ? null : message.toString());
    }

    public static void addListener(@NotNull Listener listener) {
        synchronized (Log.class) {
            listeners = ArrayUtils.add(Listener.class, listeners, listener);
        }
    }

    public static void removeListener(@NotNull Listener listener) {
        synchronized (Log.class) {
            listeners = ArrayUtils.remove(Listener.class, listeners, listener);
        }
    }

    private static void notifyListeners(@Nullable Object message, @Nullable Throwable t, boolean notifyUI) {
        for (Listener listener : listeners) {
            listener.loggedMessage(message, t, notifyUI);
        }
    }

    /** Receives log messages, optionally indicating whether they should update the UI. */
    public interface Listener {
        void loggedMessage(@Nullable Object message, @Nullable Throwable t);

        default void loggedMessage(@Nullable Object message, @Nullable Throwable t, boolean notifyUI) {
            loggedMessage(message, t);
        }
    }

}

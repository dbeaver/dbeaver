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

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.jkiss.code.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurableLogHandler implements LogHandler {

    private static final String LOG_CONFIG_FILE = "conf/dbeaver-logging.conf";

    private final Map<String, LogLevel> logLevels = new HashMap<>();
    private LogLevel rootLevel = LogLevel.INFO;

    public ConfigurableLogHandler() {
        loadConfiguration();
//        logLevels.put("net.snowflake.client.jdbc.SnowflakeUtil", LogLevel.OFF);
//        logLevels.put("net.snowflake.client.jdbc.RestRequest", LogLevel.OFF);
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        Path configFile = findConfigFile();

        if (configFile != null && Files.exists(configFile)) {
            try (InputStream is = Files.newInputStream(configFile)) {
                props.load(is);
            } catch (IOException e) {
                System.err.println("lalala" + e.getMessage());
            }
        }

        for (String name : props.stringPropertyNames()) {
            String value = props.getProperty(name);
            LogLevel level = LogLevel.fromName(value);
            if (level != null) {
                if ("root".equalsIgnoreCase(name)) {
                    rootLevel = level;
                } else {
                    logLevels.put(name, level);
                }
            }
        }
    }

    @Nullable
    private Path findConfigFile() {
        try {
            Bundle bundle = FrameworkUtil.getBundle(ConfigurableLogHandler.class);
            if (bundle != null) {
                URL entry = bundle.getEntry(LOG_CONFIG_FILE);
                if (entry != null) {
                    URL localURL = FileLocator.toFileURL(entry);
                    return Path.of(localURL.toURI());
                }
            }
        } catch (URISyntaxException | IOException ignored) {
            // ignored
        }

        return null;
    }

    private LogLevel getLevel(String name) {
        LogLevel level = logLevels.get(name);
        if (level == null) {
            for (Map.Entry<String, LogLevel> entry : logLevels.entrySet()) {
                if (name.startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return level != null ? level : rootLevel;
    }

    @Override
    public String getName(String name) {
        return name;
    }

    @Override
    public boolean isDebugEnabled(String name) {
        return getLevel(name).isAtLeast(LogLevel.DEBUG);
    }

    @Override
    public boolean isErrorEnabled(String name) {
        return getLevel(name).isAtLeast(LogLevel.ERROR);
    }

    @Override
    public boolean isFatalEnabled(String name) {
        return getLevel(name).isAtLeast(LogLevel.FATAL);
    }

    @Override
    public boolean isInfoEnabled(String name) {
        return getLevel(name).isAtLeast(LogLevel.INFO);
    }

    @Override
    public boolean isTraceEnabled(String name) {
        return getLevel(name).isAtLeast(LogLevel.TRACE);
    }

    @Override
    public boolean isWarnEnabled(String name) {
        return getLevel(name).isAtLeast(LogLevel.WARN);
    }

    private void doLog(LogLevel level, String name, Object message, Throwable t) {
        if (!getLevel(name).isAtLeast(level)) {
            return;
        }
        Log log = Log.getLog(name, false);
        log.writeToConsole(level.toSeverity(), message, t);
    }

    @Override
    public void trace(String name, Object message) {
        doLog(LogLevel.TRACE, name, message, null);
    }

    @Override
    public void trace(String name, Object message, Throwable t) {
        doLog(LogLevel.TRACE, name, message, t);
    }

    @Override
    public void debug(String name, Object message) {
        doLog(LogLevel.DEBUG, name, message, null);
    }

    @Override
    public void debug(String name, Object message, Throwable t) {
        doLog(LogLevel.DEBUG, name, message, t);
    }

    @Override
    public void info(String name, Object message) {
        doLog(LogLevel.INFO, name, message, null);
    }

    @Override
    public void info(String name, Object message, Throwable t) {
        doLog(LogLevel.INFO, name, message, t);
    }

    @Override
    public void warn(String name, Object message) {
        doLog(LogLevel.WARN, name, message, null);
    }

    @Override
    public void warn(String name, Object message, Throwable t) {
        doLog(LogLevel.WARN, name, message, t);
    }

    @Override
    public void error(String name, Object message) {
        doLog(LogLevel.ERROR, name, message, null);
    }

    @Override
    public void error(String name, Object message, Throwable t) {
        doLog(LogLevel.ERROR, name, message, t);
    }

    @Override
    public void fatal(String name, Object message) {
        doLog(LogLevel.FATAL, name, message, null);
    }

    @Override
    public void fatal(String name, Object message, Throwable t) {
        doLog(LogLevel.FATAL, name, message, t);
    }

    private enum LogLevel {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        FATAL(5),
        OFF(6);

        private final int priority;

        LogLevel(int priority) {
            this.priority = priority;
        }

        public boolean isAtLeast(LogLevel other) {
            return this.priority <= other.priority;
        }

        public int toSeverity() {
            return switch (this) {
                case INFO -> IStatus.INFO;
                case WARN -> IStatus.WARNING;
                case ERROR, FATAL -> IStatus.ERROR;
                default -> IStatus.CANCEL;
            };
        }

        @Nullable
        public static LogLevel fromName(String name) {
            try {
                return valueOf(name.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        rootLogger.addHandler(new JULHandler(Log.getLog(Log.class)));
        rootLogger.setLevel(Level.ALL);
    }
}

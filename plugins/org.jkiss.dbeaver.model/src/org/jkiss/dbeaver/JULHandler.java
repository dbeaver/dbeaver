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

import org.eclipse.core.runtime.IStatus;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Implementation of {@link Handler} that redirects JUL logging to DBeaver log.
 */
public class JULHandler extends Handler {
    private final Log log;

    public JULHandler(Log log) {
        this.log = log;
    }

    @Override
    public void publish(LogRecord record) {
        String loggerName = record.getLoggerName();
        String message = record.getMessage();
        Throwable thrown = record.getThrown();
        int severity = getSeverity(record.getLevel());
        Log logger = loggerName == null ? log : Log.getLog(loggerName);
        switch (severity) {
            case IStatus.ERROR -> logger.error(message, thrown);
            case IStatus.WARNING -> logger.warn(message, thrown);
            case IStatus.INFO -> logger.info(message, thrown);
            default -> logger.debug(message, thrown);
        }
    }

    @Override
    public void flush() {
        // No op
    }

    @Override
    public void close() throws SecurityException {
        // No op
    }


    // TRACE -> deb -> info -> err pochemy ne bilo inf0?
    private static int getSeverity(Level level) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            return IStatus.ERROR;
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            return IStatus.WARNING;
        } else if (level.intValue() >= Level.INFO.intValue()) {
            return IStatus.INFO;
        } else {
            return IStatus.CANCEL;
        }
    }
}

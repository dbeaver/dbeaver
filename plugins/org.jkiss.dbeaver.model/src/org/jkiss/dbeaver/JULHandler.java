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
package org.jkiss.dbeaver;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

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
        int severity = getSeverity(record.getLevel());
        if (severity == IStatus.ERROR || severity == IStatus.WARNING) {
            log.log(new Status(
                getSeverity(record.getLevel()),
                record.getLoggerName(),
                record.getMessage(),
                record.getThrown()
            ));
        } else {
            log.debug(record.getMessage(), record.getThrown());
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

    private static int getSeverity(Level level) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            return IStatus.ERROR;
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            return IStatus.WARNING;
        } else {
            return IStatus.CANCEL;
        }
    }
}

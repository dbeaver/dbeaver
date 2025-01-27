/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.slf4j;

import org.jkiss.dbeaver.Log;
import org.jkiss.utils.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

public class SLFLogger implements Logger {
    private final Log log;

    public SLFLogger(String name) {
        // Do not write to Eclipse log from 3rd party loggers
        log = Log.getLog(name, false);
    }

    @Override
    public String getName() {
        return log.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        log.trace(formatMessage(s));
    }

    @Override
    public void trace(String s, Object o) {
        log.trace(formatMessage(s, o));
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        log.trace(formatMessage(s, o, o1));
    }

    @Override
    public void trace(String s, Object... objects) {
        log.trace(formatMessage(s, objects));
    }

    @Override
    public void trace(String s, Throwable throwable) {
        log.trace(formatMessage(s), throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return log.isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String s) {
        this.trace(s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        this.trace(s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        this.trace(s, o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        this.trace(s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        this.trace(s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        if (Log.DEV_DEBUG_ENABLED) {
            log.debug(formatMessage(s));
        }
    }

    @Override
    public void debug(String s, Object o) {
        if (Log.DEV_DEBUG_ENABLED) {
            log.debug(formatMessage(s, o));
        }
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        if (Log.DEV_DEBUG_ENABLED) {
            log.debug(formatMessage(s, o, o1));
        }
    }

    @Override
    public void debug(String s, Object... objects) {
        if (Log.DEV_DEBUG_ENABLED) {
            log.debug(formatMessage(s, objects));
        }
    }

    @Override
    public void debug(String s, Throwable throwable) {
        if (Log.DEV_DEBUG_ENABLED) {
            log.debug(formatMessage(s), throwable);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String s) {
        this.debug(s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        this.debug(s, o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        this.debug(s, o, o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        this.debug(s, objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        this.debug(s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        log.info(formatMessage(s));
    }

    @Override
    public void info(String s, Object o) {
        log.info(formatMessage(s, o));
    }

    @Override
    public void info(String s, Object o, Object o1) {
        log.info(formatMessage(s, o, o1));
    }

    @Override
    public void info(String s, Object... objects) {
        log.info(formatMessage(s, objects));
    }

    @Override
    public void info(String s, Throwable throwable) {
        log.info(formatMessage(s), throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return log.isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String s) {
        this.info(s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        this.info(s, o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        this.info(s, o, o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        this.info(s, objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        this.info(s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        log.warn(formatMessage(s));
    }

    @Override
    public void warn(String s, Object o) {
        log.warn(formatMessage(s, o));
    }

    @Override
    public void warn(String s, Object... objects) {
        log.warn(formatMessage(s, objects));
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        log.warn(formatMessage(s, o1, o1));
    }

    @Override
    public void warn(String s, Throwable throwable) {
        log.warn(formatMessage(s), throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String s) {
        this.warn(s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        this.warn(s, o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        this.warn(s, o, o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        this.warn(s, objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        this.warn(s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        log.error(formatMessage(s));
    }

    @Override
    public void error(String s, Object o) {
        log.error(formatMessage(s, o));
    }

    @Override
    public void error(String s, Object o, Object o1) {
        log.error(formatMessage(s, o, o1));
    }

    @Override
    public void error(String s, Object... objects) {
        log.error(formatMessage(s, objects));
    }

    @Override
    public void error(String s, Throwable throwable) {
        log.error(formatMessage(s), throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return log.isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String s) {
        this.error(s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        this.error(s, o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        this.error(s, o, o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        this.error(s, objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        this.error(s, throwable);
    }

    private Object formatMessage(String message, Object... params) {
        return getName() + ": " + (ArrayUtils.isEmpty(params) ?
            message : MessageFormatter.arrayFormat(message, params).getMessage());
    }
}

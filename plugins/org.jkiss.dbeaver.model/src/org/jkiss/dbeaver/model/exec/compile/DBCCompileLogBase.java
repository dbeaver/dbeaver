/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.exec.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DBCCompileLogBase implements DBCCompileLog {

    /** "Trace" level logging. */
    public static final int LOG_LEVEL_TRACE  = 1;
    /** "Debug" level logging. */
    public static final int LOG_LEVEL_DEBUG  = 2;
    /** "Info" level logging. */
    public static final int LOG_LEVEL_INFO   = 3;
    /** "Warn" level logging. */
    public static final int LOG_LEVEL_WARN   = 4;
    /** "Error" level logging. */
    public static final int LOG_LEVEL_ERROR  = 5;
    /** "Fatal" level logging. */
    public static final int LOG_LEVEL_FATAL  = 6;

    private Throwable error;
    private List<DBCCompileError> errorStack = new ArrayList<>();

    public DBCCompileLogBase()
    {

    }

    protected void log(final int type, final Object message, final Throwable t)
    {
        if (t != null) {
            error = t;
        } else if (message instanceof DBCCompileError) {
            errorStack.add((DBCCompileError) message);
        }
    }

    @Override
    public void trace(String trace) {
        log(LOG_LEVEL_TRACE, trace, null);
    }

    @Override
    public void info(String trace) {
        log(LOG_LEVEL_INFO, trace, null);
    }

    @Override
    public void warn(DBCCompileError error) {
        log(LOG_LEVEL_WARN, error, null);
    }

    @Override
    public void error(DBCCompileError error) {
        log(LOG_LEVEL_ERROR, error, null);
    }

    @Override
    public Throwable getError()
    {
        return error;
    }

    @Override
    public Collection<DBCCompileError> getErrorStack()
    {
        return errorStack;
    }

    @Override
    public void clearLog()
    {
        error = null;
        errorStack.clear();
    }
}

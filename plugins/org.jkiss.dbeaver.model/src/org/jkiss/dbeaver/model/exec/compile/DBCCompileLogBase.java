/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

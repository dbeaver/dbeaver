/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
    private List<DBCCompileError> errorStack = new ArrayList<DBCCompileError>();

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

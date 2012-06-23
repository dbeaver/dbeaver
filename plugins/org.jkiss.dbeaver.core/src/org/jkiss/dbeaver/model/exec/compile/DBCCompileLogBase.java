/*
 * Copyright (C) 2010-2012 Serge Rieder
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

import org.apache.commons.logging.impl.SimpleLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DBCCompileLogBase extends SimpleLog implements DBCCompileLog {

    private Throwable error;
    private List<DBCCompileError> errorStack = new ArrayList<DBCCompileError>();

    public DBCCompileLogBase()
    {
        super("Compile log");
    }

    @Override
    protected void log(final int type, final Object message, final Throwable t)
    {
        if (t != null) {
            error = t;
        } else if (message instanceof DBCCompileError) {
            errorStack.add((DBCCompileError) message);
        }
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

/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public Throwable getError()
    {
        return error;
    }

    public Collection<DBCCompileError> getErrorStack()
    {
        return errorStack;
    }

    public void clearLog()
    {
        error = null;
        errorStack.clear();
    }
}

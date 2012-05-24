/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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

package org.jkiss.dbeaver.ext.oracle.model.source;

import org.apache.commons.logging.impl.SimpleLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OracleCompileLogBase extends SimpleLog implements OracleCompileLog {

    private Throwable error;
    private List<OracleCompileError> errorStack = new ArrayList<OracleCompileError>();

    public OracleCompileLogBase()
    {
        super("Compile log");
    }

    @Override
    protected void log(final int type, final Object message, final Throwable t)
    {
        if (t != null) {
            error = t;
        } else if (message instanceof OracleCompileError) {
            errorStack.add((OracleCompileError) message);
        }
    }

    public Throwable getError()
    {
        return error;
    }

    public Collection<OracleCompileError> getErrorStack()
    {
        return errorStack;
    }

    public void clearLog()
    {
        error = null;
        errorStack.clear();
    }
}

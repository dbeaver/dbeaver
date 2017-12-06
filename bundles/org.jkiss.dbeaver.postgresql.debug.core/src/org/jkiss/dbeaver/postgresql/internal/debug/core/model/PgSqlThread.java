package org.jkiss.dbeaver.postgresql.internal.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.debug.core.model.DatabaseThread;
import org.jkiss.dbeaver.debug.core.model.IDatabaseDebugTarget;

public class PgSqlThread extends DatabaseThread {
    
    private final PgSqlDebugController controller;

    public PgSqlThread(IDatabaseDebugTarget target, PgSqlDebugController controller)
    {
        super(target);
        this.controller = controller;
    }

    @Override
    public String getName() throws DebugException
    {
        String name = NLS.bind("Thread: pldbg", controller.getSessionId());
        return name;
    }

}

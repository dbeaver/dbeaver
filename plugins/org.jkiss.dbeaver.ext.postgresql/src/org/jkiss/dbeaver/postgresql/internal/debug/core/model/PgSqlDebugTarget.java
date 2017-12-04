package org.jkiss.dbeaver.postgresql.internal.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.jkiss.dbeaver.debug.core.model.DatabaseDebugTarget;
import org.jkiss.dbeaver.debug.core.model.IDatabaseDebugController;
import org.jkiss.dbeaver.postgresql.debug.core.PgSqlDebugCore;

public class PgSqlDebugTarget extends DatabaseDebugTarget {
    
    public PgSqlDebugTarget(ILaunch launch, IProcess process, IDatabaseDebugController controller)
    {
        super(PgSqlDebugCore.MODEL_IDENTIFIER, launch, process, controller);
    }

    @Override
    public String getName() throws DebugException
    {
        return "PL/pgSQL Debug";
    }

}

package org.jkiss.dbeaver.postgresql.internal.debug.core.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.jkiss.dbeaver.debug.core.model.DatabaseDebugTarget;
import org.jkiss.dbeaver.debug.core.model.DatabaseThread;
import org.jkiss.dbeaver.postgresql.debug.core.PostgreSqlDebugCore;

public class PgSqlDebugTarget extends DatabaseDebugTarget<PgSqlDebugController> {
    
    public PgSqlDebugTarget(ILaunch launch, IProcess process, PgSqlDebugController controller)
    {
        super(PostgreSqlDebugCore.MODEL_IDENTIFIER, launch, process, controller);
    }

    @Override
    protected DatabaseThread newThread(PgSqlDebugController controller)
    {
        return new PgSqlThread(this, controller);
    }

    @Override
    protected String getConfiguredName(ILaunchConfiguration configuration) throws CoreException
    {
        return configuration.getName();
    }

    @Override
    protected String getDefaultName()
    {
        return "PL/pgSQL Debug";
    }

}

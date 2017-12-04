package org.jkiss.dbeaver.postgresql.internal.debug.core.model;

import java.util.Map;

import org.eclipse.debug.core.ILaunch;
import org.jkiss.dbeaver.debug.core.model.DatabaseDebugController;
import org.jkiss.dbeaver.debug.core.model.DatabaseDebugTarget;
import org.jkiss.dbeaver.debug.core.model.DatabaseLaunchDelegate;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;
import org.jkiss.dbeaver.debug.core.model.IDatabaseDebugController;

public class PgSqlLaunchDelegate extends DatabaseLaunchDelegate {

    @Override
    protected DatabaseDebugController createController(String datasourceId, String databaseName,
            Map<String, Object> attributes)
    {
        return new DatabaseDebugController();
    }

    @Override
    protected DatabaseProcess createProcess(ILaunch launch, String name)
    {
        return new DatabaseProcess(launch, name);
    }

    @Override
    protected DatabaseDebugTarget createDebugTarget(ILaunch launch, IDatabaseDebugController controller,
            DatabaseProcess process)
    {
        return new PgSqlDebugTarget(launch, process, controller);
    }

}

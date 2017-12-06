package org.jkiss.dbeaver.postgresql.internal.debug.core.model;

import java.util.Map;

import org.eclipse.debug.core.ILaunch;
import org.jkiss.dbeaver.debug.core.model.DatabaseLaunchDelegate;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;

public class PgSqlLaunchDelegate extends DatabaseLaunchDelegate<PgSqlDebugController> {

    @Override
    protected PgSqlDebugController createController(String datasourceId, String databaseName,
            Map<String, Object> attributes)
    {
        return new PgSqlDebugController(datasourceId, databaseName, attributes);
    }

    @Override
    protected DatabaseProcess createProcess(ILaunch launch, String name)
    {
        return new DatabaseProcess(launch, name);
    }

    @Override
    protected PgSqlDebugTarget createDebugTarget(ILaunch launch, PgSqlDebugController controller,
            DatabaseProcess process)
    {
        return new PgSqlDebugTarget(launch, process, controller);
    }

}

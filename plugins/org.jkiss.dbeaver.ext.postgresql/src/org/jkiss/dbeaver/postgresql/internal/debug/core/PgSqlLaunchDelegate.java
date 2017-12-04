package org.jkiss.dbeaver.postgresql.internal.debug.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;
import org.jkiss.dbeaver.debug.core.model.IDatabaseDebugController;
import org.jkiss.dbeaver.postgresql.internal.debug.core.model.PgSqlDebugTarget;

public class PgSqlLaunchDelegate implements ILaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException
    {
        //FIXME:AF: extract attributes from configuration
        IDatabaseDebugController controller = new PgSqlDebugController();
        DatabaseProcess process = new DatabaseProcess(launch, configuration.getName());
        PgSqlDebugTarget target = new PgSqlDebugTarget(launch, process, controller);
        launch.addDebugTarget(target);
    }

}

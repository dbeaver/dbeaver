package org.jkiss.dbeaver.postgresql.internal.debug.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.jkiss.dbeaver.postgresql.debug.core.IPgSqlDebugController;
import org.jkiss.dbeaver.postgresql.internal.debug.core.model.PgSqlDebugTarget;
import org.jkiss.dbeaver.postgresql.internal.debug.core.model.PgSqlProcess;

public class PgSqlLaunchDelegate implements ILaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException
    {
        //FIXME:AF: extract attributes from configuration
        IPgSqlDebugController controller = new PgSqlDebugController();
        PgSqlProcess process = new PgSqlProcess(launch, configuration.getName());
        PgSqlDebugTarget target = new PgSqlDebugTarget(launch, process, controller);
        launch.addDebugTarget(target);
    }

}

package org.jkiss.dbeaver.postgresql.internal.debug.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.jkiss.dbeaver.postgresql.internal.debug.core.model.PgSqlDebugTarget;

public class PgSqlLaunchDelegate implements ILaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException
    {
        //FIXME:AF: extract attributes from configuration and create controller here
        PgSqlDebugTarget target = new PgSqlDebugTarget(launch);

    }

}

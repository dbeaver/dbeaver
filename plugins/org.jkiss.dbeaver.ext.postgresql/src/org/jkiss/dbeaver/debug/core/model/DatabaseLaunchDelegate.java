package org.jkiss.dbeaver.debug.core.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.jkiss.dbeaver.debug.core.DebugCore;

public abstract class DatabaseLaunchDelegate<C extends IDatabaseDebugController> extends LaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException
    {
        String datasourceId = DebugCore.extractDatasource(configuration);
        String databaseName = DebugCore.extractDatabase(configuration);
        Map<String, Object> attributes = extractAttributes(configuration);
        C controller = createController(datasourceId, databaseName, attributes);
        DatabaseProcess process = createProcess(launch, configuration.getName());
        DatabaseDebugTarget<C> target = createDebugTarget(launch, controller, process);
        launch.addDebugTarget(target);
        controller.connect(monitor);
    }

    protected Map<String, Object> extractAttributes(ILaunchConfiguration configuration)
    {
        Map<String, Object> attributes = new HashMap<>();
        return attributes;
    }

    protected abstract C createController(String datasourceId, String databaseName, Map<String, Object> attributes);

    protected abstract DatabaseProcess createProcess(ILaunch launch, String name);

    protected abstract DatabaseDebugTarget<C> createDebugTarget(ILaunch launch, C controller, DatabaseProcess process);

}

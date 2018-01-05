/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.debug.core.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.DBRResult;
import org.jkiss.dbeaver.runtime.core.RuntimeCore;

public abstract class DatabaseLaunchDelegate<C extends DBGController> extends LaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
        throws CoreException {
        String datasourceId = DebugCore.extractDatasourceId(configuration);
        DataSourceDescriptor datasourceDescriptor = DataSourceRegistry.findDataSource(datasourceId);
        if (datasourceDescriptor == null) {
            String message = NLS.bind("Unable to find data source with id {0}", datasourceId);
            throw new CoreException(DebugCore.newErrorStatus(message));
        }
        String databaseName = DebugCore.extractDatabaseName(configuration);
        Map<String, Object> attributes = extractAttributes(configuration);
        C controller = createController(datasourceDescriptor, databaseName, attributes);
        DatabaseProcess process = createProcess(launch, configuration.getName());
        DatabaseDebugTarget<C> target = createDebugTarget(launch, controller, process);
        launch.addDebugTarget(target);
        DefaultProgressMonitor progress = new DefaultProgressMonitor(monitor);
        DBRResult connectResult = controller.connect(progress);
        IStatus status = RuntimeCore.toStatus(connectResult);
        if (!status.isOK()) {
            throw new CoreException(status);
        }
    }

    protected Map<String, Object> extractAttributes(ILaunchConfiguration configuration) {
        Map<String, Object> attributes = new HashMap<>();
        return attributes;
    }

    protected abstract C createController(DataSourceDescriptor datasourceDescriptor, String databaseName, Map<String, Object> attributes) throws CoreException;

    protected abstract DatabaseProcess createProcess(ILaunch launch, String name);

    protected abstract DatabaseDebugTarget<C> createDebugTarget(ILaunch launch, C controller, DatabaseProcess process);

}

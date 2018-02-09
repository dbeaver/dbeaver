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
package org.jkiss.dbeaver.debug.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.core.model.DatabaseDebugTarget;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

public abstract class DatabaseLaunchDelegate extends LaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
        throws CoreException {
        String datasourceId = DebugCore.extractDatasourceId(configuration);
        DataSourceDescriptor datasourceDescriptor = DataSourceRegistry.findDataSource(datasourceId);
        if (datasourceDescriptor == null) {
            String message = NLS.bind("Unable to find data source with id {0}", datasourceId);
            throw new CoreException(DebugCore.newErrorStatus(message));
        }
        Map<String, Object> attributes = extractAttributes(configuration);
        DBGController controller = createController(datasourceDescriptor);
        if (controller == null) {
            String message = NLS.bind("Unable to find debug controller for datasource {0}", datasourceDescriptor);
            throw new CoreException(DebugCore.newErrorStatus(message));
        }
        controller.init(attributes);
        DatabaseProcess process = createProcess(launch, configuration.getName());
        DatabaseDebugTarget target = createDebugTarget(launch, controller, process);
        target.connect(monitor);
        launch.addDebugTarget(target);
    }

    protected Map<String, Object> extractAttributes(ILaunchConfiguration configuration) throws CoreException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(DBGController.DATABASE_NAME, DebugCore.extractDatabaseName(configuration));
        attributes.put(DBGController.SCHEMA_NAME, DebugCore.extractSchemaName(configuration));
        attributes.put(DBGController.PROCEDURE_OID, DebugCore.extractProcedureOid(configuration));
        attributes.put(DBGController.PROCEDURE_NAME, DebugCore.extractProcedureName(configuration));
        attributes.put(DBGController.PROCEDURE_CALL, DebugCore.extractProcedureCall(configuration));
        //FIXME:AF:extract from launch configuration
        //FIXME -1 - target PID (-1 for ANY PID)
        attributes.put(DBGController.PROCESS_ID, -1);
        //Well, put it all for now
        attributes.putAll(configuration.getAttributes());
        return attributes;
    }

    protected abstract DBGController createController(DBPDataSourceContainer dataSourceContainer) throws CoreException;

    protected abstract DatabaseProcess createProcess(ILaunch launch, String name);

    protected abstract DatabaseDebugTarget createDebugTarget(ILaunch launch, DBGController controller, DatabaseProcess process);

}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGControllerFactory;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.core.model.DatabaseDebugTarget;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Map;

public class DatabaseLaunchDelegate extends LaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {
        String datasourceId = configuration.getAttribute(DBGConstants.ATTR_DATASOURCE_ID, (String)null);
        DBPDataSourceContainer datasourceDescriptor = DBUtils.findDataSource(datasourceId);
        if (datasourceDescriptor == null) {
            String message = NLS.bind("Unable to find data source with id {0}", datasourceId);
            throw new CoreException(DebugUtils.newErrorStatus(message));
        }
        DBGController controller = createController(datasourceDescriptor, configuration.getAttributes());
        if (controller == null) {
            String message = NLS.bind("Unable to find debug controller for datasource {0}", datasourceDescriptor);
            throw new CoreException(DebugUtils.newErrorStatus(message));
        }
        DatabaseProcess process = createProcess(launch, configuration.getName());
        DatabaseDebugTarget target = createDebugTarget(launch, controller, process);
        target.connect(monitor);
        launch.addDebugTarget(target);
    }

    protected DBGController createController(DBPDataSourceContainer dataSourceContainer, Map<String, Object> attributes) throws CoreException {
        try {
            DBGControllerFactory controllerFactory = GeneralUtils.adapt(dataSourceContainer, DBGControllerFactory.class);
            if (controllerFactory != null) {
                return controllerFactory.createController(dataSourceContainer, attributes);
            }
            throw new DBGException(
                NLS.bind("Unable to find controller factory for datasource \"{0}\"", dataSourceContainer.getDriver().getProviderId())
            );
        } catch (DBGException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    protected DatabaseProcess createProcess(ILaunch launch, String name) {
        return new DatabaseProcess(launch, name);
    }

    protected DatabaseDebugTarget createDebugTarget(ILaunch launch, DBGController controller, DatabaseProcess process) {
        return new DatabaseDebugTarget(DBGConstants.MODEL_IDENTIFIER_DATABASE, launch, process, controller);
    }

}

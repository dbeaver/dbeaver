/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.debug.sourcelookup;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

public class ProcedureSourcePathComputer implements ISourcePathComputerDelegate {

    @Override
    public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor)
            throws CoreException {
        String datasourceId = DebugCore.extractDatasourceId(configuration);
        String nodePath = DebugCore.extractNodePath(configuration);
        DataSourceDescriptor descriptor = DataSourceRegistry.findDataSource(datasourceId);
        final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
        IProject project = descriptor.getRegistry().getProject();
        DBNNode node;
        try {
            node = navigatorModel.getNodeByPath(new DefaultProgressMonitor(monitor), project, nodePath);
        } catch (DBException e) {
            String message = NLS.bind("Unable to extract node {0}", nodePath);
            throw new CoreException(DebugCore.newErrorStatus(message, e));
        }
        DatasourceSourceContainer container = new DatasourceSourceContainer(descriptor, node);
        return new ISourceContainer[] {container};
    }

}

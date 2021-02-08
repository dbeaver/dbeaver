/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.core.DebugUtils;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public class DatabaseNavigatorSourceContainer extends CompositeSourceContainer {

    private final DBPDataSourceContainer datasource;
    private final DBPProject project;

    public DatabaseNavigatorSourceContainer(DBPDataSourceContainer descriptor) {
        this.datasource = descriptor;
        this.project = datasource.getRegistry().getProject();
    }

    @Override
    public String getName() {
        return datasource.getName();
    }

    @Override
    protected Object[] findSourceElements(String name, ISourceContainer[] containers) throws CoreException {
        DBNNode node;
        try {
            VoidProgressMonitor monitor = new VoidProgressMonitor();
            node = DBWorkbench.getPlatform().getNavigatorModel().getNodeByPath(monitor, project, name);
        } catch (DBException e) {
            String message = NLS.bind(DebugCoreMessages.DatasourceSourceContainer_e_extract_node, name);
            throw new CoreException(DebugUtils.newErrorStatus(message, e));
        }
        if (node != null) {
            return new Object[] { node };
        }
        return super.findSourceElements(name, containers);
    }

    @Override
    public ISourceContainerType getType() {
        return getSourceContainerType(DBGConstants.SOURCE_CONTAINER_TYPE_DATASOURCE);
    }

    @Override
    protected ISourceContainer[] createSourceContainers() throws CoreException {
        return new ISourceContainer[0];
    }

}

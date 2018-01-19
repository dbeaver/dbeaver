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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

public class DatasourceSourceContainer extends CompositeSourceContainer {
    
    private final DBPDataSourceContainer datasource;
    private final DBNNode startNode;

    public DatasourceSourceContainer(DataSourceDescriptor descriptor, DBNNode node) {
        this.datasource = descriptor;
        this.startNode = node;
    }

    @Override
    public String getName() {
        return datasource.getName();
    }
    
    @Override
    protected Object[] findSourceElements(String name, ISourceContainer[] containers) throws CoreException {
        if (startNode != null) {
            return new Object[] {startNode};
        }
        return super.findSourceElements(name, containers);
    }
    
    @Override
    public ISourceContainerType getType() {
        return getSourceContainerType(DebugCore.SOURCE_CONTAINER_TYPE_DATASOURCE);
    }

    @Override
    protected ISourceContainer[] createSourceContainers() throws CoreException {
        // TODO Auto-generated method stub
        return new ISourceContainer[0];
    }

}

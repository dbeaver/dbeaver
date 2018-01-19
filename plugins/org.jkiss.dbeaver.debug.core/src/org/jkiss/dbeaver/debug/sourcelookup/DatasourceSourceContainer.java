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

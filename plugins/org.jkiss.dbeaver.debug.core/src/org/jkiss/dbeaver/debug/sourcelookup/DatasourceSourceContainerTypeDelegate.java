package org.jkiss.dbeaver.debug.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
import org.jkiss.dbeaver.model.navigator.DBNNode;

public class DatasourceSourceContainerTypeDelegate extends AbstractSourceContainerTypeDelegate {

    @Override
    public ISourceContainer createSourceContainer(String memento) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMemento(ISourceContainer container) throws CoreException {
        // TODO Auto-generated method stub
        return DBNNode.NodePathType.database.getPrefix();
    }

}

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

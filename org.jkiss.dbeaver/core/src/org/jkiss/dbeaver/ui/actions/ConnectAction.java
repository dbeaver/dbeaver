package org.jkiss.dbeaver.ui.actions;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.jface.action.IAction;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.ui.ICommandIds;

public class ConnectAction extends DataSourceAction
{
    @Override
    protected void updateAction(IAction action) {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(false);
        action.setEnabled(dataSourceContainer != null && !dataSourceContainer.isConnected());
    }

    public void run(IAction action)
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(false);
        if (dataSourceContainer != null) {
            try {
                dataSourceContainer.connect(this);
            }
            catch (DBException ex) {
                DBeaverUtils.showErrorDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
                    "Connect", "Can't connect to '" + dataSourceContainer.getName() + "'", ex);
            }
        }
    }

}
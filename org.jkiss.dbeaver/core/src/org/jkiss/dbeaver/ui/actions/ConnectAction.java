package org.jkiss.dbeaver.ui.actions;

import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.ui.ICommandIds;

public class ConnectAction extends ConnectionAction
{

    public ConnectAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_CONNECT);
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_CONNECT);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/sql/connect.png"));
        setText("Connect");
        setToolTipText("Connect to database");
    }

    public void run()
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
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
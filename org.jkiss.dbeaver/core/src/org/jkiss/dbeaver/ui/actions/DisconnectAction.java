package org.jkiss.dbeaver.ui.actions;

import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.DBeaverUtils;
import org.jkiss.dbeaver.ui.ICommandIds;

public class DisconnectAction extends ConnectionAction
{

    public DisconnectAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_DISCONNECT);
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_DISCONNECT);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/sql/disconnect.png"));
        setText("Disconnect");
        setToolTipText("Disconnect from database");
    }

    public void run()
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            try {
                dataSourceContainer.disconnect(this);
            }
            catch (DBException ex) {
                DBeaverUtils.showErrorDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                    "Disconnect", "Can't disconnect from '" + dataSourceContainer.getName() + "'", ex);
            }
        }
    }

}
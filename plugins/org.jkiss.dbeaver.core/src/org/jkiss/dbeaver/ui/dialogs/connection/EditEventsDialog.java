package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;

/**
 * Events edit dialog
 */
public class EditEventsDialog extends HelpEnabledDialog {

    private DBPConnectionInfo connectionInfo;

    protected EditEventsDialog(Shell shell, DBPConnectionInfo connectionInfo)
    {
        super(shell, IHelpContextIds.CTX_EDIT_CONNECTION_EVENTS);
        this.connectionInfo = connectionInfo;
    }

}

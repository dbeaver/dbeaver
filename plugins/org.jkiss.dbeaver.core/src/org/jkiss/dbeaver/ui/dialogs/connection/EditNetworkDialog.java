/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;

/**
 * Network handlers edit dialog
 */
public class EditNetworkDialog extends HelpEnabledDialog {

    private EditNetworkDialogPage page;

    protected EditNetworkDialog(Shell shell, DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        super(shell, IHelpContextIds.CTX_EDIT_CONNECTION_TUNNELS);

        page = new EditNetworkDialogPage(driver, connectionInfo);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_tunnel_title);
        Control area = super.createDialogArea(parent);
        page.createControl((Composite) area);

        return area;
    }

    @Override
    protected void okPressed()
    {
        page.saveConfigurations();
        super.okPressed();
    }

    @Override
    protected void cancelPressed()
    {
        super.cancelPressed();
    }
}

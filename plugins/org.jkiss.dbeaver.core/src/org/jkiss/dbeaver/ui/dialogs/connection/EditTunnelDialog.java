/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPConnectionEventType;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Events edit dialog
 */
public class EditTunnelDialog extends HelpEnabledDialog {

    private DBPConnectionInfo connectionInfo;

    protected EditTunnelDialog(Shell shell, DBPConnectionInfo connectionInfo)
    {
        super(shell, IHelpContextIds.CTX_EDIT_CONNECTION_TUNNELS);
        this.connectionInfo = connectionInfo;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Edit connection tunneling");
        //getShell().setImage(DBIcon.EVENT.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        TabFolder tunnelTypeFolder = new TabFolder(composite, SWT.TOP);
        tunnelTypeFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        createTabSSH(tunnelTypeFolder);
        
        return composite;
    }

    private void createTabSSH(TabFolder folder)
    {
        TabItem tabItem = new TabItem(folder, SWT.NONE);
        tabItem.setText("SSH");
        tabItem.setToolTipText("SSH tunnel");

        Composite composite = new Composite(folder, SWT.NONE);
        tabItem.setControl(composite);
        
        UIUtils.createCheckbox(composite, "Use SSH Tunnel", false);
    }


    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    @Override
    protected void cancelPressed()
    {
        super.cancelPressed();
    }
}

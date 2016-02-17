/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;

/**
 * Events edit dialog
 */
public class EditShellCommandsDialog extends HelpEnabledDialog {

    private DataSourceDescriptor dataSource;
    private EditShellCommandsDialogPage page;

    protected EditShellCommandsDialog(Shell shell, DataSourceDescriptor dataSource)
    {
        super(shell, IHelpContextIds.CTX_EDIT_CONNECTION_EVENTS);
        this.dataSource = dataSource;
        page = new EditShellCommandsDialogPage(dataSource);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_connection_events_title);
        getShell().setImage(DBeaverIcons.getImage(UIIcon.EVENT));

        page.createControl(parent);
        return page.getControl();
    }

    @Override
    protected void okPressed()
    {
        page.saveConfigurations(dataSource);
        super.okPressed();
    }

    @Override
    protected void cancelPressed()
    {
        super.cancelPressed();
    }
}

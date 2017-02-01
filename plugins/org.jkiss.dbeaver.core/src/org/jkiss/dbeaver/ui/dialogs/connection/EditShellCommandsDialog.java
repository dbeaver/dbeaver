/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

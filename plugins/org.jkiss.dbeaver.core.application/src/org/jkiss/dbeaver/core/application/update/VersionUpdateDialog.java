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
package org.jkiss.dbeaver.core.application.update;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;

class VersionUpdateDialog extends Dialog {

    private VersionDescriptor newVersion;
    private static final int INFO_ID = 1000;
    private Font boldFont;

    public VersionUpdateDialog(Shell parentShell, VersionDescriptor newVersion)
    {
        super(parentShell);
        this.newVersion = newVersion;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_version_update_title);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(1, false));
        Composite propGroup = UIUtils.createControlGroup(composite, CoreMessages.dialog_version_update_title, 2, GridData.FILL_BOTH, 0);

        boldFont = UIUtils.makeBoldFont(composite.getFont());

        final Label titleLabel = new Label(propGroup, SWT.NONE);
        titleLabel.setText(newVersion == null ? CoreMessages.dialog_version_update_no_new_version : CoreMessages.dialog_version_update_available_new_version);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        titleLabel.setLayoutData(gd);
        titleLabel.setFont(boldFont);

        UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_current_version);
        new Label(propGroup, SWT.NONE)
            .setText(DBeaverCore.getVersion().toString());

        UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_new_version);
        new Label(propGroup, SWT.NONE)
            .setText(newVersion == null ? CoreMessages.dialog_version_update_n_a : newVersion.getProgramVersion().toString() + "    (" + newVersion.getUpdateTime() + ")"); //$NON-NLS-2$ //$NON-NLS-3$

        if (newVersion != null) {
            final Label notesLabel = UIUtils.createControlLabel(propGroup, CoreMessages.dialog_version_update_notes);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            notesLabel.setLayoutData(gd);

            final Label notesText = new Label(propGroup, SWT.NONE);
            notesText.setText(newVersion.getReleaseNotes());
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            notesText.setLayoutData(gd);

            final Label hintLabel = new Label(propGroup, SWT.NONE);
            hintLabel.setText(CoreMessages.dialog_version_update_press_more_info_);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            hintLabel.setLayoutData(gd);
            hintLabel.setFont(boldFont);
        }

        return parent;
    }

    @Override
    public boolean close()
    {
        boldFont.dispose();
        return super.close();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        if (newVersion != null) {
            boolean hasUpdate = Platform.getBundle(CheckForUpdateAction.P2_PLUGIN_ID) != null;
            if (hasUpdate) {
                createButton(
                    parent,
                    IDialogConstants.PROCEED_ID,
                    "Update",
                    true);
            }
            createButton(
                parent,
                INFO_ID,
                CoreMessages.dialog_version_update_button_more_info,
                !hasUpdate);
        }

        createButton(
            parent,
            IDialogConstants.CLOSE_ID,
            IDialogConstants.CLOSE_LABEL,
            newVersion == null);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == INFO_ID) {
            if (newVersion != null) {
                UIUtils.launchProgram(newVersion.getBaseURL());
            }
        } else if (buttonId == IDialogConstants.PROCEED_ID) {
            final IWorkbenchWindow window = DBeaverUI.getActiveWorkbenchWindow();
            CheckForUpdateAction.activateStandardHandler(window);
            try {
                ActionUtils.runCommand(CheckForUpdateAction.P2_UPDATE_COMMAND, PlatformUI.getWorkbench());
            } finally {
                CheckForUpdateAction.deactivateStandardHandler(window);
            }
        }
        close();
    }
}

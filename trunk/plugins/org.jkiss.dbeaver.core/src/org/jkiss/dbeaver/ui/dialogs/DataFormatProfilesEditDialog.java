/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.registry.formatter.DataFormatterRegistry;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

/**
* DataFormatProfilesEditDialog
*/
public class DataFormatProfilesEditDialog extends org.eclipse.jface.dialogs.Dialog {
    private static final int NEW_ID = IDialogConstants.CLIENT_ID + 1;
    private static final int DELETE_ID = IDialogConstants.CLIENT_ID + 2;
    private org.eclipse.swt.widgets.List profileList;

    public DataFormatProfilesEditDialog(Shell parentShell)
    {
        super(parentShell);
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_data_format_profiles_title);

        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        profileList = new org.eclipse.swt.widgets.List(group, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 200;
        profileList.setLayoutData(gd);

        profileList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                getButton(DELETE_ID).setEnabled(profileList.getSelectionIndex() >= 0);
            }
        });

        loadProfiles();
        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, NEW_ID, CoreMessages.dialog_data_format_profiles_button_new_profile, false);
        createButton(parent, DELETE_ID, CoreMessages.dialog_data_format_profiles_button_delete_profile, false);
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);

        getButton(DELETE_ID).setEnabled(false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        DataFormatterRegistry registry = DataFormatterRegistry.getInstance();
        if (buttonId == NEW_ID) {
            String profileName = EnterNameDialog.chooseName(getShell(), CoreMessages.dialog_data_format_profiles_dialog_name_chooser_title);
            if (registry.getCustomProfile(profileName) != null) {
                UIUtils.showMessageBox(
                        getShell(),
                        CoreMessages.dialog_data_format_profiles_error_title,
                        NLS.bind(CoreMessages.dialog_data_format_profiles_error_message, profileName), SWT.ICON_ERROR);
            } else {
                registry.createCustomProfile(profileName);
                loadProfiles();
            }
        } else if (buttonId == DELETE_ID) {
            int selectionIndex = profileList.getSelectionIndex();
            if (selectionIndex >= 0) {
                DBDDataFormatterProfile profile = registry.getCustomProfile(profileList.getItem(selectionIndex));
                if (profile != null) {
                    if (UIUtils.confirmAction(
                            getShell(),
                            CoreMessages.dialog_data_format_profiles_confirm_delete_title,
                            CoreMessages.dialog_data_format_profiles_confirm_delete_message)) {
                        registry.deleteCustomProfile(profile);
                        loadProfiles();
                    }
                }
            }
        } else {
            super.buttonPressed(buttonId);
        }
    }

    private void loadProfiles()
    {
        profileList.removeAll();
        List<DBDDataFormatterProfile> profiles = DataFormatterRegistry.getInstance().getCustomProfiles();
        for (DBDDataFormatterProfile profile : profiles) {
            profileList.add(profile.getProfileName());
        }
        Button deleteButton = getButton(DELETE_ID);
        if (deleteButton != null) {
            deleteButton.setEnabled(false);
        }
    }
}

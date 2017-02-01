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

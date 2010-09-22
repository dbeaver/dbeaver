/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.misc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.registry.DataFormatterRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;

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

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Manage data format profiles");

        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        profileList = new org.eclipse.swt.widgets.List(group, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 200;
        profileList.setLayoutData(gd);

        profileList.addSelectionListener(new SelectionAdapter() {
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
        createButton(parent, NEW_ID, "New Profile", false);
        createButton(parent, DELETE_ID, "Delete Profile", false);
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);

        getButton(DELETE_ID).setEnabled(false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        DataFormatterRegistry registry = DBeaverCore.getInstance().getDataFormatterRegistry();
        if (buttonId == NEW_ID) {
            String profileName = EnterNameDialog.chooseName(getShell(), "Profile Name");
            if (registry.getCustomProfile(profileName) != null) {
                UIUtils.showMessageBox(getShell(), "Create profile", "Profile '" + profileName + "' already exists");
            } else {
                registry.createCustomProfile(profileName);
                loadProfiles();
            }
        } else if (buttonId == DELETE_ID) {
            int selectionIndex = profileList.getSelectionIndex();
            if (selectionIndex >= 0) {
                DBDDataFormatterProfile profile = registry.getCustomProfile(profileList.getItem(selectionIndex));
                if (profile != null) {
                    if (UIUtils.confirmAction(getShell(), "Delete Profile", "Are You Sure?")) {
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
        List<DBDDataFormatterProfile> profiles = DBeaverCore.getInstance().getDataFormatterRegistry().getCustomProfiles();
        for (DBDDataFormatterProfile profile : profiles) {
            profileList.add(profile.getProfileName());
        }
        Button deleteButton = getButton(DELETE_ID);
        if (deleteButton != null) {
            deleteButton.setEnabled(false);
        }
    }
}

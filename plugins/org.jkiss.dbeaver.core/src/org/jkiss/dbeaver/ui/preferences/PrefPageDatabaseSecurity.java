/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageDatabaseSecurity
 */
public class PrefPageDatabaseSecurity extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.security"; //$NON-NLS-1$

    private Button useSecurePreferences;
    private Button useMasterPassword;
    private Text masterPasswordText;

    public PrefPageDatabaseSecurity()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBeaverCore.getGlobalPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group preferencesGroup = UIUtils.createControlGroup(composite, "Security", 1, SWT.NONE, 0);

            useSecurePreferences = UIUtils.createCheckbox(preferencesGroup, "Use secure passwords storage", false);
            new Label(preferencesGroup, SWT.NONE).setText(
                "Using secure storage is more safe than just keeping encrypted passwords in datasources configuration.\n" +
                "But it prevents configuration sharing among team of developers.\n" +
                "Also it is not portable because passwords are stored in OS-specific storage.");

            // Link to secure storage config
            PreferenceLinkArea storageLinkArea = new PreferenceLinkArea(preferencesGroup, SWT.NONE,
                "org.eclipse.equinox.security.ui.storage",
                "See <a>''{0}''</a> for settings related to the encrypted storage system.",
                (IWorkbenchPreferenceContainer) getContainer(), null); //$NON-NLS-1$
            storageLinkArea.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        }

        {
            Group masterPasswordGroup = UIUtils.createControlGroup(composite, "Master password", 2, SWT.NONE, 0);

            useMasterPassword = UIUtils.createCheckbox(masterPasswordGroup, "Use master password", false);
            GridData gd = new GridData();
            gd.horizontalSpan = 2;
            useMasterPassword.setLayoutData(gd);

            masterPasswordText = UIUtils.createLabelText(masterPasswordGroup, "Master password", "", SWT.BORDER | SWT.PASSWORD);
            final Label label = new Label(masterPasswordGroup, SWT.NONE);
            label.setText(
                "Master password can be used to prevent usage of DBeaver by unauthorized users.\n" +
                "This password will be asked during DBeaver startup.\n" +
                "Usually users enable secure storage OR master password. Having both might be 'over-secured'.");
            gd = new GridData();
            gd.horizontalSpan = 2;
            label.setLayoutData(gd);

            useMasterPassword.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    masterPasswordText.setEnabled(useMasterPassword.getSelection());
                }
            });
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        useSecurePreferences.setSelection(store.getBoolean(DBeaverPreferences.SECURITY_USE_SECURE_PASSWORDS_STORAGE));
        useMasterPassword.setSelection(store.getBoolean(DBeaverPreferences.SECURITY_USE_MASTER_PASSWORD));
        masterPasswordText.setEnabled(useMasterPassword.getSelection());
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        store.setValue(DBeaverPreferences.SECURITY_USE_SECURE_PASSWORDS_STORAGE, useSecurePreferences.getSelection());
        final boolean hasMasterPassword = useMasterPassword.getSelection();
        store.setValue(DBeaverPreferences.SECURITY_USE_MASTER_PASSWORD, hasMasterPassword);
        if (hasMasterPassword) {
            // Update master password hash
        }

        PrefUtils.savePreferenceStore(store);

        return true;
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

    @Nullable
    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {
    }

}
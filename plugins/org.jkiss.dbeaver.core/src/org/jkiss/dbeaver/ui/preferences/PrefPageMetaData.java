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

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageMetaData
 */
public class PrefPageMetaData extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.meta"; //$NON-NLS-1$

    private Button readExpensiveCheck;
    private Button separateMetaConnectionCheck;
    private Button caseSensitiveNamesCheck;

    private Button overrideClientApplicationNameCheck;
    private Text clientApplicationNameText;

    public PrefPageMetaData()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES) ||
            store.contains(ModelPreferences.META_SEPARATE_CONNECTION) ||
            store.contains(ModelPreferences.META_CASE_SENSITIVE) ||
            store.contains(ModelPreferences.META_CLIENT_NAME_OVERRIDE) ||
            store.contains(ModelPreferences.META_CLIENT_NAME_VALUE)
            ;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    @Override
    protected Control createPreferenceContent(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        {
            Group metadataGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_metadata, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            separateMetaConnectionCheck = UIUtils.createCheckbox(metadataGroup, CoreMessages.pref_page_database_general_separate_meta_connection, false);
            caseSensitiveNamesCheck = UIUtils.createCheckbox(metadataGroup, CoreMessages.pref_page_database_general_checkbox_case_sensitive_names, false);
            readExpensiveCheck = UIUtils.createCheckbox(metadataGroup, CoreMessages.pref_page_database_general_checkbox_show_row_count, false);
        }

        {
            Group clientNameGroup = UIUtils.createControlGroup(composite, "Client Application Name", 2, GridData.FILL_HORIZONTAL, 0);

            final Label label = UIUtils.createLabel(clientNameGroup,
                "Client application name is passed to database server on connect to identify client connections.\n" +
                "By default it is set to product name + product version. You can set it to any custom value.");
            GridData gd = new GridData();
            gd.horizontalSpan = 2;
            label.setLayoutData(gd);
            overrideClientApplicationNameCheck = UIUtils.createCheckbox(clientNameGroup, "Override client application name", null, false, 2);
            overrideClientApplicationNameCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateClientAppEnablement();
                }
            });
            clientApplicationNameText = UIUtils.createLabelText(clientNameGroup, "Client Application Name", "");
        }

        return composite;
    }

    private void updateClientAppEnablement() {
        clientApplicationNameText.setEnabled(overrideClientApplicationNameCheck.getSelection());
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            readExpensiveCheck.setSelection(store.getBoolean(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES));
            separateMetaConnectionCheck.setSelection(store.getBoolean(ModelPreferences.META_SEPARATE_CONNECTION));
            caseSensitiveNamesCheck.setSelection(store.getBoolean(ModelPreferences.META_CASE_SENSITIVE));

            overrideClientApplicationNameCheck.setSelection(store.getBoolean(ModelPreferences.META_CLIENT_NAME_OVERRIDE));
            clientApplicationNameText.setText(store.getString(ModelPreferences.META_CLIENT_NAME_VALUE));

            updateClientAppEnablement();
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES, readExpensiveCheck.getSelection());
            store.setValue(ModelPreferences.META_SEPARATE_CONNECTION, separateMetaConnectionCheck.getSelection());
            store.setValue(ModelPreferences.META_CASE_SENSITIVE, caseSensitiveNamesCheck.getSelection());

            store.setValue(ModelPreferences.META_CLIENT_NAME_OVERRIDE, overrideClientApplicationNameCheck.getSelection());
            store.setValue(ModelPreferences.META_CLIENT_NAME_VALUE, clientApplicationNameText.getText());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES);
        store.setToDefault(ModelPreferences.META_SEPARATE_CONNECTION);
        store.setToDefault(ModelPreferences.META_CASE_SENSITIVE);

        store.setToDefault(ModelPreferences.META_CLIENT_NAME_OVERRIDE);
        store.setToDefault(ModelPreferences.META_CLIENT_NAME_VALUE);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
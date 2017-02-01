/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
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
            store.contains(ModelPreferences.META_CASE_SENSITIVE)
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

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            readExpensiveCheck.setSelection(store.getBoolean(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES));
            separateMetaConnectionCheck.setSelection(store.getBoolean(ModelPreferences.META_SEPARATE_CONNECTION));
            caseSensitiveNamesCheck.setSelection(store.getBoolean(ModelPreferences.META_CASE_SENSITIVE));
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
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
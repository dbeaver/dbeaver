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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageDatabaseEditors
 */
public class PrefPageDatabaseEditors extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.editors"; //$NON-NLS-1$
    private Button syncEditorDataSourceWithNavigator;
    private Button showGeneralToolbarEverywhere;
    private Button showEditToolbar;
    private Spinner toolbarDatabaseSelectorWidth;
    private Spinner toolbarSchemaSelectorWidth;

    public PrefPageDatabaseEditors()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
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
            Group toolbarsGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_toolbars, 2, SWT.NONE, 0);

            syncEditorDataSourceWithNavigator = UIUtils.createCheckbox(toolbarsGroup, CoreMessages.pref_page_database_general_label_sync_editor_connection_with_navigator, CoreMessages.pref_page_database_general_label_sync_editor_connection_with_navigator_tip, false, 2);

            showGeneralToolbarEverywhere = UIUtils.createCheckbox(toolbarsGroup, CoreMessages.pref_page_database_general_label_show_general_toolbar_everywhere, CoreMessages.pref_page_database_general_label_show_general_toolbar_everywhere_tip, false, 2);
            showEditToolbar = UIUtils.createCheckbox(toolbarsGroup, CoreMessages.pref_page_database_general_label_show_edit_toolbar, CoreMessages.pref_page_database_general_label_show_edit_toolbar_tip, false, 2);
            toolbarDatabaseSelectorWidth = UIUtils.createLabelSpinner(toolbarsGroup, CoreMessages.pref_page_database_general_label_database_selector_width, CoreMessages.pref_page_database_general_label_database_selector_width_tip, 20, 10, 200);
            toolbarSchemaSelectorWidth = UIUtils.createLabelSpinner(toolbarsGroup, CoreMessages.pref_page_database_general_label_schema_selector_width, CoreMessages.pref_page_database_general_label_schema_selector_width_tip, 20, 10, 200);
        }

            performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        syncEditorDataSourceWithNavigator.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE));

        showGeneralToolbarEverywhere.setSelection(store.getBoolean(DBeaverPreferences.TOOLBARS_SHOW_GENERAL_ALWAYS));
        showEditToolbar.setSelection(store.getBoolean(DBeaverPreferences.TOOLBARS_SHOW_EDIT));
        toolbarDatabaseSelectorWidth.setSelection(store.getInt(DBeaverPreferences.TOOLBARS_DATABASE_SELECTOR_WIDTH));
        toolbarSchemaSelectorWidth.setSelection(store.getInt(DBeaverPreferences.TOOLBARS_SCHEMA_SELECTOR_WIDTH));
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        store.setValue(DBeaverPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE, syncEditorDataSourceWithNavigator.getSelection());
        store.setValue(DBeaverPreferences.TOOLBARS_SHOW_GENERAL_ALWAYS, showGeneralToolbarEverywhere.getSelection());
        store.setValue(DBeaverPreferences.TOOLBARS_SHOW_EDIT, showEditToolbar.getSelection());
        store.setValue(DBeaverPreferences.TOOLBARS_DATABASE_SELECTOR_WIDTH, toolbarDatabaseSelectorWidth.getSelection());
        store.setValue(DBeaverPreferences.TOOLBARS_SCHEMA_SELECTOR_WIDTH, toolbarSchemaSelectorWidth.getSelection());

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
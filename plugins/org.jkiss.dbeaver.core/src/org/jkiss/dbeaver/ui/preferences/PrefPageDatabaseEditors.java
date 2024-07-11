/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageDatabaseEditors
 */
public class PrefPageDatabaseEditors extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.editors"; //$NON-NLS-1$

    private Button keepEditorsOnRestart;
    private Button keepEditorsOnDisconnect;
    private Button disconnectOnEditorsClose;
    private Button refreshEditorOnOpen;
    private Button editorFullName;
    private Button showTableGrid;
    private Button showPreviewOnSave;
    private Button syncEditorDataSourceWithNavigator;

    public PrefPageDatabaseEditors()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        {
            Group groupEditors = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_editors, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            keepEditorsOnRestart = UIUtils.createCheckbox(
                groupEditors,
                CoreMessages.pref_page_ui_general_keep_database_editors,
                store.getBoolean(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS));
            keepEditorsOnRestart.setToolTipText(CoreMessages.pref_page_ui_general_keep_database_editors_tip);

            keepEditorsOnDisconnect = UIUtils.createCheckbox(
                groupEditors,
                CoreMessages.pref_page_ui_general_keep_database_editors_on_disconnect,
                CoreMessages.pref_page_ui_general_keep_database_editors_on_disconnect_tip,
                store.getBoolean(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS_ON_DISCONNECT),
                1
            );

            disconnectOnEditorsClose = UIUtils.createCheckbox(
                groupEditors,
                CoreMessages.pref_page_ui_general_disconnect_on_editors_close,
                CoreMessages.pref_page_ui_general_disconnect_on_editors_close_tip,
                store.getBoolean(DBeaverPreferences.UI_DISCONNECT_ON_EDITORS_CLOSE),
                1
            );

            refreshEditorOnOpen = UIUtils.createCheckbox(
                groupEditors,
                CoreMessages.pref_page_ui_general_refresh_editor_on_open,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN));
            refreshEditorOnOpen.setToolTipText(CoreMessages.pref_page_ui_general_refresh_editor_on_open_tip);

            editorFullName = UIUtils.createCheckbox(
                groupEditors,
                CoreMessages.pref_page_ui_general_show_full_name_in_editor,
                store.getBoolean(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME));
            showTableGrid = UIUtils.createCheckbox(
                groupEditors,
                CoreMessages.pref_page_ui_general_show_table_grid,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_EDITOR_SHOW_TABLE_GRID));
            showPreviewOnSave = UIUtils.createCheckbox(
                groupEditors,
                CoreMessages.pref_page_ui_general_show_preview_on_save,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_SQL_PREVIEW));

            syncEditorDataSourceWithNavigator = UIUtils.createCheckbox(
                groupEditors,
                CoreMessages.pref_page_database_general_label_sync_editor_connection_with_navigator,
                CoreMessages.pref_page_database_general_label_sync_editor_connection_with_navigator_tip,
                store.getBoolean(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE),
                2);
        }

        return composite;
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        keepEditorsOnRestart.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS));
        keepEditorsOnDisconnect.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS_ON_DISCONNECT));
        disconnectOnEditorsClose.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_DISCONNECT_ON_EDITORS_CLOSE));
        refreshEditorOnOpen.setSelection(store.getDefaultBoolean(NavigatorPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN));
        editorFullName.setSelection(store.getDefaultBoolean(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME));
        showTableGrid.setSelection(store.getDefaultBoolean(NavigatorPreferences.NAVIGATOR_EDITOR_SHOW_TABLE_GRID));
        showPreviewOnSave.setSelection(store.getDefaultBoolean(NavigatorPreferences.NAVIGATOR_SHOW_SQL_PREVIEW));
        syncEditorDataSourceWithNavigator.setSelection(store.getDefaultBoolean(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE));
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        store.setValue(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS, keepEditorsOnRestart.getSelection());
        store.setValue(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS_ON_DISCONNECT, keepEditorsOnDisconnect.getSelection());
        store.setValue(DBeaverPreferences.UI_DISCONNECT_ON_EDITORS_CLOSE, disconnectOnEditorsClose.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN, refreshEditorOnOpen.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, editorFullName.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_EDITOR_SHOW_TABLE_GRID, showTableGrid.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SHOW_SQL_PREVIEW, showPreviewOnSave.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE, syncEditorDataSourceWithNavigator.getSelection());

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
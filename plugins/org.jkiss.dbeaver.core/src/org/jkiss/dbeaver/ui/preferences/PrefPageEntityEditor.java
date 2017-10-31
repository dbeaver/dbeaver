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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageEntityEditor
 */
public class PrefPageEntityEditor extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.entityeditor"; //$NON-NLS-1$

    private Button keepEditorsOnRestart;
    private Button refreshEditorOnOpen;
    private Button editorFullName;
    private Button showPreviewOnSave;

    public PrefPageEntityEditor()
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

        // Editors settings
        {
            Group groupEditors = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_editors, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            keepEditorsOnRestart = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_keep_database_editors, false);
            keepEditorsOnRestart.setToolTipText(CoreMessages.pref_page_ui_general_keep_database_editors_tip);

            refreshEditorOnOpen = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_refresh_editor_on_open, false);
            refreshEditorOnOpen.setToolTipText(CoreMessages.pref_page_ui_general_refresh_editor_on_open_tip);

            editorFullName = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_show_full_name_in_editor, false);
            showPreviewOnSave = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_show_preview_on_save, false);
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        keepEditorsOnRestart.setSelection(store.getBoolean(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS));
        refreshEditorOnOpen.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN));
        editorFullName.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME));
        showPreviewOnSave.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SHOW_SQL_PREVIEW));
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        store.setValue(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS, keepEditorsOnRestart.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN, refreshEditorOnOpen.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, editorFullName.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_SHOW_SQL_PREVIEW, showPreviewOnSave.getSelection());

        PrefUtils.savePreferenceStore(store);

        return true;
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
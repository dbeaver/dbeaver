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
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageSQLResources
 */
public class PrefPageSQLResources extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.resources"; //$NON-NLS-1$

    private Combo deleteEmptyCombo;
    private Button autoFoldersCheck;
    private Button connectionFoldersCheck;
    private Text scriptTitlePattern;

    public PrefPageSQLResources()
    {
        super();
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        // Scripts
        {
            Composite scriptsGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_editor_group_resources, 2, GridData.FILL_HORIZONTAL, 0);
            ((GridData)scriptsGroup.getLayoutData()).horizontalSpan = 2;

            deleteEmptyCombo = UIUtils.createLabelCombo(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_delete_empty_scripts, SWT.DROP_DOWN | SWT.READ_ONLY);
            for (SQLPreferenceConstants.EmptyScriptCloseBehavior escb : SQLPreferenceConstants.EmptyScriptCloseBehavior.values()) {
                deleteEmptyCombo.add(escb.getTitle());
            }
            deleteEmptyCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            deleteEmptyCombo.select(0);
            autoFoldersCheck = UIUtils.createCheckbox(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_put_new_scripts, null, false, 2);
            connectionFoldersCheck = UIUtils.createCheckbox(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_create_script_folders, null, false, 2);
            scriptTitlePattern = UIUtils.createLabelText(scriptsGroup, CoreMessages.pref_page_sql_editor_title_pattern, "");
            UIUtils.installContentProposal(
                    scriptTitlePattern,
                    new TextContentAdapter(),
                    new SimpleContentProposalProvider(new String[] {
                        GeneralUtils.variablePattern(SQLEditor.VAR_CONNECTION_NAME),
                        GeneralUtils.variablePattern(SQLEditor.VAR_DRIVER_NAME),
                        GeneralUtils.variablePattern(SQLEditor.VAR_FILE_NAME),
                        GeneralUtils.variablePattern(SQLEditor.VAR_FILE_EXT)}));
            UIUtils.setContentProposalToolTip(scriptTitlePattern, "Output file name patterns",
                    SQLEditor.VAR_CONNECTION_NAME, SQLEditor.VAR_DRIVER_NAME, SQLEditor.VAR_FILE_NAME, SQLEditor.VAR_FILE_EXT);
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        deleteEmptyCombo.setText(SQLPreferenceConstants.EmptyScriptCloseBehavior.getByName(
            store.getString(SQLPreferenceConstants.SCRIPT_DELETE_EMPTY)).getTitle());
        autoFoldersCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS));
        connectionFoldersCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS));
        scriptTitlePattern.setText(store.getString(SQLPreferenceConstants.SCRIPT_TITLE_PATTERN));

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        store.setValue(SQLPreferenceConstants.SCRIPT_DELETE_EMPTY,
            SQLPreferenceConstants.EmptyScriptCloseBehavior.getByTitle(deleteEmptyCombo.getText()).name());
        store.setValue(SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS, autoFoldersCheck.getSelection());
        store.setValue(SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS, connectionFoldersCheck.getSelection());
        store.setValue(SQLPreferenceConstants.SCRIPT_TITLE_PATTERN, scriptTitlePattern.getText());

        PrefUtils.savePreferenceStore(store);

        return super.performOk();
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    public IAdaptable getElement() {
        return null;
    }

    @Override
    public void setElement(IAdaptable element) {

    }
}
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
package org.jkiss.dbeaver.ui.editors.sql.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageSQLEditor
 */
public class PrefPageSQLEditor extends TargetPrefPage
{
    private static final Log log = Log.getLog(PrefPageSQLEditor.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqleditor"; //$NON-NLS-1$

    private static final String TEXT_EDITOR_PAGE_ID = "org.eclipse.ui.preferencePages.GeneralTextEditor"; //$NON-NLS-1$

    private Button editorSeparateConnectionCheck;
    private Button connectOnActivationCheck;
    private Button connectOnExecuteCheck;

    private Button saveOnQueryExecution;
    private Button autoSaveOnClose;

    private Button csFoldingEnabled;
    private Button csMarkOccurrencesUnderCursor;
    private Button csMarkOccurrencesForSelection;

    private Button closeTabOnErrorCheck;
    private Combo resultsOrientationCombo;

    public PrefPageSQLEditor()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION) ||
            store.contains(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE) ||
            store.contains(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE) ||
    
            store.contains(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE) ||
            store.contains(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE) ||

            store.contains(SQLPreferenceConstants.FOLDING_ENABLED) ||
            store.contains(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR) ||

            store.contains(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR) ||
            store.contains(SQLPreferenceConstants.RESULT_SET_ORIENTATION)
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
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        {
            Group connectionsGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_editor_group_connections, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
            ((GridData)connectionsGroup.getLayoutData()).horizontalSpan = 2;
            editorSeparateConnectionCheck = UIUtils.createCheckbox(connectionsGroup, SQLEditorMessages.pref_page_sql_editor_label_separate_connection_each_editor, false);

            connectOnActivationCheck = UIUtils.createCheckbox(connectionsGroup, SQLEditorMessages.pref_page_sql_editor_label_connect_on_editor_activation, false);
            connectOnExecuteCheck = UIUtils.createCheckbox(connectionsGroup, SQLEditorMessages.pref_page_sql_editor_label_connect_on_query_execute, false);
        }

        {
            Group autoSaveGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_editor_group_auto_save, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
            autoSaveOnClose = UIUtils.createCheckbox(autoSaveGroup, SQLEditorMessages.pref_page_sql_editor_label_auto_save_on_close, false);
            saveOnQueryExecution = UIUtils.createCheckbox(autoSaveGroup, SQLEditorMessages.pref_page_sql_editor_label_save_on_query_execute, false);
        }

        // Folding
        {
            Composite foldingGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_completion_group_misc, 2, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);

            csMarkOccurrencesUnderCursor = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences_tip, false, 2);
            csMarkOccurrencesForSelection = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences_for_selections, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences_for_selections_tip, false, 2);
            csFoldingEnabled = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_folding_enabled, SQLEditorMessages.pref_page_sql_completion_label_folding_enabled_tip, false, 2);
        }

        {
            Composite layoutGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_editor_group_result_view, 2, GridData.FILL_HORIZONTAL, 0);
            ((GridData)layoutGroup.getLayoutData()).horizontalSpan = 2;

            closeTabOnErrorCheck = UIUtils.createCheckbox(layoutGroup, SQLEditorMessages.pref_page_sql_editor_label_close_results_tab_on_error, null, false, 2);

            resultsOrientationCombo = UIUtils.createLabelCombo(layoutGroup, SQLEditorMessages.pref_page_sql_editor_label_results_orientation, SQLEditorMessages.pref_page_sql_editor_label_results_orientation_tip, SWT.READ_ONLY | SWT.DROP_DOWN);
            ((GridData)resultsOrientationCombo.getLayoutData()).grabExcessHorizontalSpace = false;
            for (SQLEditor.ResultSetOrientation orientation : SQLEditor.ResultSetOrientation.values()) {
                if (orientation.isSupported()) {
                    resultsOrientationCombo.add(orientation.getLabel());
                }
            }
        }

        {
            new PreferenceLinkArea(composite, SWT.NONE,
                PrefPageSQLEditor.TEXT_EDITOR_PAGE_ID,
                "<a>''{0}''</a>" + SQLEditorMessages.pref_page_sql_editor_link_text_editor,
                (IWorkbenchPreferenceContainer) getContainer(), null); //$NON-NLS-1$

        }
        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            editorSeparateConnectionCheck.setSelection(store.getBoolean(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION));
            connectOnActivationCheck.setSelection(store.getBoolean(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE));
            connectOnExecuteCheck.setSelection(store.getBoolean(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE));

            autoSaveOnClose.setSelection(store.getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE));
            saveOnQueryExecution.setSelection(store.getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE));

            csFoldingEnabled.setSelection(store.getBoolean(SQLPreferenceConstants.FOLDING_ENABLED));
            csMarkOccurrencesUnderCursor.setSelection(store.getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR));
            csMarkOccurrencesForSelection.setSelection(store.getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION));

            closeTabOnErrorCheck.setSelection(store.getBoolean(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR));
            SQLEditor.ResultSetOrientation orientation = SQLEditor.ResultSetOrientation.valueOf(store.getString(SQLPreferenceConstants.RESULT_SET_ORIENTATION));
            resultsOrientationCombo.setText(orientation.getLabel());
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION, editorSeparateConnectionCheck.getSelection());
            store.setValue(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE, connectOnActivationCheck.getSelection());
            store.setValue(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE, connectOnExecuteCheck.getSelection());

            store.setValue(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE, autoSaveOnClose.getSelection());
            store.setValue(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE, saveOnQueryExecution.getSelection());

            store.setValue(SQLPreferenceConstants.FOLDING_ENABLED, csFoldingEnabled.getSelection());
            store.setValue(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR, csMarkOccurrencesUnderCursor.getSelection());
            store.setValue(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION, csMarkOccurrencesForSelection.getSelection());

            store.setValue(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR, closeTabOnErrorCheck.getSelection());
            String orientationLabel = resultsOrientationCombo.getText();
            for (SQLEditor.ResultSetOrientation orientation : SQLEditor.ResultSetOrientation.values()) {
                if (orientationLabel.equals(orientation.getLabel())) {
                    store.setValue(SQLPreferenceConstants.RESULT_SET_ORIENTATION, orientation.name());
                    break;
                }
            }
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION);
        store.setToDefault(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE);
        store.setToDefault(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE);

        store.setToDefault(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE);
        store.setToDefault(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE);

        store.setToDefault(SQLPreferenceConstants.FOLDING_ENABLED);
        store.setToDefault(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR);
        store.setToDefault(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION);

        store.setToDefault(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR);
        store.setToDefault(SQLPreferenceConstants.RESULT_SET_ORIENTATION);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
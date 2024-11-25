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
package org.jkiss.dbeaver.ui.editors.sql.preferences;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences.SeparateConnectionBehavior;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.List;

/**
 * PrefPageSQLEditor
 */
public class PrefPageSQLEditor extends TargetPrefPage {
    private static final Log log = Log.getLog(PrefPageSQLEditor.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqleditor"; //$NON-NLS-1$

    private static final String TEXT_EDITOR_PAGE_ID = "org.eclipse.ui.preferencePages.GeneralTextEditor"; //$NON-NLS-1$

    private Combo editorSeparateConnectionCombo;
    private Button connectOnActivationCheck;
    private Button connectOnExecuteCheck;

    private Button autoSaveOnChange;
    private Button saveOnQueryExecution;
    private Button autoSaveOnClose;
    private Button autoSaveActiveSchema;

    private Button closeTabOnErrorCheck;
    private Combo resultsOrientationCombo;
    private Button autoOpenOutputView;
    private Button replaceCurrentTab;
    private Spinner sizeWarningThresholdSpinner;

    public PrefPageSQLEditor() {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor) {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION) ||
            store.contains(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE) ||
            store.contains(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE) ||
    
            store.contains(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE) ||
            store.contains(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE) ||
            store.contains(SQLPreferenceConstants.AUTO_SAVE_ACTIVE_SCHEMA) ||

            store.contains(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }

    private static final List<SeparateConnectionBehavior> editorUseSeparateConnectionValues = List.of(
        SeparateConnectionBehavior.ALWAYS,
        SeparateConnectionBehavior.DEFAULT,
        SeparateConnectionBehavior.NEVER
    );

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        {
            Group connectionsGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_editor_group_connections, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            ((GridData) connectionsGroup.getLayoutData()).horizontalSpan = 2;
            editorSeparateConnectionCombo = UIUtils.createLabelCombo(
                UIUtils.createComposite(connectionsGroup, 3),
                SQLEditorMessages.pref_page_sql_editor_label_separate_connection_each_editor,
                NLS.bind(SQLEditorMessages.pref_page_sql_editor_label_separate_connection_each_editor_tip, PrefUtils.collectSingleConnectionDrivers()),
                SWT.READ_ONLY | SWT.DROP_DOWN
            );
            if (this.getDataSourceContainer() != null && this.getDataSourceContainer().getDriver().isEmbedded()) {
                editorSeparateConnectionCombo.setEnabled(false);
            } else {
                editorSeparateConnectionCombo.setItems(editorUseSeparateConnectionValues.stream()
                    .map(SeparateConnectionBehavior::getTitle).toArray(String[]::new));
            }
            editorSeparateConnectionCombo.setToolTipText(
                NLS.bind(SQLEditorMessages.pref_page_sql_editor_label_separate_connection_each_editor_tip, PrefUtils.collectSingleConnectionDrivers())
            );
            ((GridData) editorSeparateConnectionCombo.getLayoutData()).grabExcessHorizontalSpace = false;
            connectOnActivationCheck = UIUtils.createCheckbox(connectionsGroup, SQLEditorMessages.pref_page_sql_editor_label_connect_on_editor_activation, false);
            connectOnExecuteCheck = UIUtils.createCheckbox(connectionsGroup, SQLEditorMessages.pref_page_sql_editor_label_connect_on_query_execute, false);
        }

        {
            Group autoSaveGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_editor_group_auto_save, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            autoSaveOnChange = UIUtils.createCheckbox(autoSaveGroup, SQLEditorMessages.pref_page_sql_editor_label_auto_save_on_change, SQLEditorMessages.pref_page_sql_editor_label_auto_save_on_change_tip, false, 1);
            autoSaveOnClose = UIUtils.createCheckbox(autoSaveGroup, SQLEditorMessages.pref_page_sql_editor_label_auto_save_on_close, false);
            saveOnQueryExecution = UIUtils.createCheckbox(autoSaveGroup, SQLEditorMessages.pref_page_sql_editor_label_save_on_query_execute, SQLEditorMessages.pref_page_sql_editor_label_save_on_query_execute, false, 1);
            autoSaveActiveSchema = UIUtils.createCheckbox(autoSaveGroup, SQLEditorMessages.pref_page_sql_editor_label_save_active_schema, false);
        }

        {
            Composite layoutGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_editor_group_result_view, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
            ((GridData) layoutGroup.getLayoutData()).horizontalSpan = 2;

            closeTabOnErrorCheck = UIUtils.createCheckbox(
                layoutGroup,
                SQLEditorMessages.pref_page_sql_editor_label_close_results_tab_on_error,
                SQLEditorMessages.pref_page_sql_editor_label_close_results_tab_on_error_tip,
                false,
                1);
            replaceCurrentTab = UIUtils.createCheckbox(layoutGroup, SQLEditorMessages.pref_page_sql_editor_label_replace_on_single_query_exec_view, SQLEditorMessages.pref_page_sql_editor_label_replace_on_single_query_exec_view_tip, true, 2);

            Composite orientationComposite = UIUtils.createComposite(layoutGroup, 2);
            resultsOrientationCombo = UIUtils.createLabelCombo(orientationComposite, SQLEditorMessages.pref_page_sql_editor_label_results_orientation, SQLEditorMessages.pref_page_sql_editor_label_results_orientation_tip, SWT.READ_ONLY | SWT.DROP_DOWN);
            resultsOrientationCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            for (SQLEditor.ResultSetOrientation orientation : SQLEditor.ResultSetOrientation.values()) {
                if (orientation.isSupported()) {
                    resultsOrientationCombo.add(orientation.getLabel());
                }
            }

            autoOpenOutputView = UIUtils.createCheckbox(layoutGroup, SQLEditorMessages.pref_page_sql_editor_label_auto_open_output_view, SQLEditorMessages.pref_page_sql_editor_label_auto_open_output_view_tip, false, 2);

            Composite rsSizeComposite = UIUtils.createComposite(layoutGroup, 2);
            sizeWarningThresholdSpinner = UIUtils.createLabelSpinner(rsSizeComposite,
                SQLEditorMessages.pref_page_sql_editor_label_size_warning_threshold,
                SQLEditorMessages.pref_page_sql_editor_label_size_warning_threshold_tip, 20, 2, 200);
            sizeWarningThresholdSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        }

        {
            Composite linksGroup = UIUtils.createControlGroup(composite, "", 1, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            UIUtils.createPreferenceLink(
                linksGroup,
                "<a>''{0}''</a> " + SQLEditorMessages.pref_page_sql_editor_link_text_editor,
                PrefPageSQLEditor.TEXT_EDITOR_PAGE_ID,
                (IWorkbenchPreferenceContainer) getContainer(), null
            );
            UIUtils.createPreferenceLink(
                linksGroup,
                SQLEditorMessages.pref_page_sql_editor_link_colors_and_fonts,
                EditorUtils.COLORS_AND_FONTS_PAGE_ID,
                (IWorkbenchPreferenceContainer) getContainer(), null
            );
        }
        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store) {
        loadPreferences(getTargetPreferenceStore(), false);
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store) {
        try {
            store.setValue(
                SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION,
                editorUseSeparateConnectionValues.get(editorSeparateConnectionCombo.getSelectionIndex()).name()
            );
            store.setValue(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE, connectOnActivationCheck.getSelection());
            store.setValue(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE, connectOnExecuteCheck.getSelection());

            store.setValue(SQLPreferenceConstants.AUTO_SAVE_ON_CHANGE, autoSaveOnChange.getSelection());
            store.setValue(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE, autoSaveOnClose.getSelection());
            store.setValue(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE, saveOnQueryExecution.getSelection());
            store.setValue(SQLPreferenceConstants.AUTO_SAVE_ACTIVE_SCHEMA, autoSaveActiveSchema.getSelection());

            store.setValue(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR, closeTabOnErrorCheck.getSelection());
            store.setValue(SQLPreferenceConstants.RESULT_SET_REPLACE_CURRENT_TAB, replaceCurrentTab.getSelection());
            String orientationLabel = resultsOrientationCombo.getText();
            for (SQLEditor.ResultSetOrientation orientation : SQLEditor.ResultSetOrientation.values()) {
                if (orientationLabel.equals(orientation.getLabel())) {
                    DBWorkbench.getPlatform().getPreferenceStore().setValue(SQLPreferenceConstants.RESULT_SET_ORIENTATION, orientation.name());
                    break;
                }
            }
            store.setValue(SQLPreferenceConstants.OUTPUT_PANEL_AUTO_SHOW, autoOpenOutputView.getSelection());
            store.setValue(SQLPreferenceConstants.RESULT_SET_MAX_TABS_PER_QUERY, sizeWarningThresholdSpinner.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store) {
        store.setToDefault(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION);
        store.setToDefault(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE);
        store.setToDefault(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE);
        store.setToDefault(SQLPreferenceConstants.RESULT_SET_MAX_TABS_PER_QUERY);
        store.setToDefault(SQLPreferenceConstants.AUTO_SAVE_ON_CHANGE);
        store.setToDefault(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE);
        store.setToDefault(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE);
        store.setToDefault(SQLPreferenceConstants.AUTO_SAVE_ACTIVE_SCHEMA);

        store.setToDefault(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR);
        store.setToDefault(SQLPreferenceConstants.RESULT_SET_REPLACE_CURRENT_TAB);
        store.setToDefault(SQLPreferenceConstants.RESULT_SET_ORIENTATION);
        store.setToDefault(SQLPreferenceConstants.OUTPUT_PANEL_AUTO_SHOW);
    }

    @Override
    protected void performDefaults() {
        loadPreferences(getTargetPreferenceStore(), true);
        super.performDefaults();
    }

    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }

    private void loadPreferences(DBPPreferenceStore store, boolean useDefaults) {
        try {
            UIUtils.setComboSelection(
                editorSeparateConnectionCombo,
                SeparateConnectionBehavior.parse(
                    useDefaults
                        ? store.getDefaultString(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION)
                        : store.getString(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION)
                ).getTitle()
            );
            connectOnActivationCheck.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE)
                    : store.getBoolean(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE)
            );
            connectOnExecuteCheck.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE)
                    : store.getBoolean(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE)
            );

            autoSaveOnChange.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_CHANGE)
                    : store.getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_CHANGE)
            );
            autoSaveOnClose.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE)
                    : store.getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE)
            );
            saveOnQueryExecution.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE)
                    : store.getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE)
            );
            autoSaveActiveSchema.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.AUTO_SAVE_ACTIVE_SCHEMA)
                    : store.getBoolean(SQLPreferenceConstants.AUTO_SAVE_ACTIVE_SCHEMA)
            );

            closeTabOnErrorCheck.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR)
                    : store.getBoolean(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR)
            );
            replaceCurrentTab.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.RESULT_SET_REPLACE_CURRENT_TAB)
                    : store.getBoolean(SQLPreferenceConstants.RESULT_SET_REPLACE_CURRENT_TAB)
            );
            UIUtils.setComboSelection(
                resultsOrientationCombo,
                SQLEditor.ResultSetOrientation.valueOf(
                    useDefaults
                        ? store.getDefaultString(SQLPreferenceConstants.RESULT_SET_ORIENTATION)
                        : store.getString(SQLPreferenceConstants.RESULT_SET_ORIENTATION)
                ).getLabel()
            );
            autoOpenOutputView.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.OUTPUT_PANEL_AUTO_SHOW)
                    : store.getBoolean(SQLPreferenceConstants.OUTPUT_PANEL_AUTO_SHOW)
            );
            sizeWarningThresholdSpinner.setSelection(
                useDefaults
                    ? store.getDefaultInt(SQLPreferenceConstants.RESULT_SET_MAX_TABS_PER_QUERY)
                    : store.getInt(SQLPreferenceConstants.RESULT_SET_MAX_TABS_PER_QUERY)
            );
        } catch (Exception e) {
            log.warn(e);
        }
    }


}
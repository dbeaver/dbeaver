/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.data.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.Locale;

/**
 * PrefPageResultSetMain
 */
public class PrefPageResultSetMain extends TargetPrefPage
{
    static final Log log = Log.getLog(PrefPageResultSetMain.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset"; //$NON-NLS-1$

    private Button autoFetchNextSegmentCheck;
    private Button rereadOnScrollingCheck;
    private Text resultSetSize;
    private Button resultSetUseSQLCheck;
    private Button serverSideOrderingCheck;
    private Button readQueryMetadata;
    private Button readQueryReferences;
    private Text queryCancelTimeout;
    private Button filterForceSubselect;

    private Button keepStatementOpenCheck;
    private Button alwaysUseAllColumns;
    private Button newRowsAfter;
    private Button refreshAfterUpdate;
    private Button useNavigatorFilters;

    private Button showErrorsInDialog;

    private Button advUseFetchSize;

    private Button ignoreColumnLabelCheck;

    public PrefPageResultSetMain()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ResultSetPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT) ||
            store.contains(ModelPreferences.RESULT_SET_REREAD_ON_SCROLLING) ||
            store.contains(ModelPreferences.RESULT_SET_MAX_ROWS) ||
            store.contains(ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL) ||
            store.contains(ModelPreferences.RESULT_SET_READ_METADATA) ||
            store.contains(ResultSetPreferences.RESULT_SET_CANCEL_TIMEOUT) ||
            store.contains(ModelPreferences.SQL_FILTER_FORCE_SUBSELECT) ||
            store.contains(ResultSetPreferences.RS_EDIT_USE_ALL_COLUMNS) ||
            store.contains(ResultSetPreferences.RS_EDIT_NEW_ROWS_AFTER) ||
            store.contains(ResultSetPreferences.RS_EDIT_REFRESH_AFTER_UPDATE) ||
            store.contains(ResultSetPreferences.KEEP_STATEMENT_OPEN) ||
            store.contains(ResultSetPreferences.RESULT_SET_ORDER_SERVER_SIDE) ||
            store.contains(ModelPreferences.RESULT_SET_USE_FETCH_SIZE) ||
            store.contains(ResultSetPreferences.RESULT_SET_USE_NAVIGATOR_FILTERS) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_ERRORS_IN_DIALOG) ||
                    store.contains(ModelPreferences.RESULT_SET_IGNORE_COLUMN_LABEL)
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
        Composite leftPane = UIUtils.createComposite(composite, 1);
        leftPane.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite rightPane = UIUtils.createComposite(composite, 1);
        rightPane.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        {
            Group queriesGroup = UIUtils.createControlGroup(leftPane, ResultSetMessages.pref_page_database_general_group_queries, 2, SWT.NONE, 0);
            queriesGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

            resultSetSize = UIUtils.createLabelText(queriesGroup, ResultSetMessages.pref_page_database_general_label_result_set_max_size, "0", SWT.BORDER);
            resultSetSize.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            autoFetchNextSegmentCheck = UIUtils.createCheckbox(queriesGroup, ResultSetMessages.pref_page_database_resultsets_label_auto_fetch_segment, ResultSetMessages.pref_page_database_resultsets_label_auto_fetch_segment_tip, true, 2);
            rereadOnScrollingCheck = UIUtils.createCheckbox(queriesGroup, ResultSetMessages.pref_page_database_resultsets_label_reread_on_scrolling, ResultSetMessages.pref_page_database_resultsets_label_reread_on_scrolling_tip, true, 2);
            resultSetUseSQLCheck = UIUtils.createCheckbox(queriesGroup, ResultSetMessages.pref_page_database_resultsets_label_use_sql, ResultSetMessages.pref_page_database_resultsets_label_use_sql_tip, false, 2);
            serverSideOrderingCheck = UIUtils.createCheckbox(queriesGroup, ResultSetMessages.pref_page_database_resultsets_label_server_side_order, null, false, 2);
            readQueryMetadata = UIUtils.createCheckbox(queriesGroup, ResultSetMessages.pref_page_database_resultsets_label_read_metadata,
               ResultSetMessages.pref_page_database_resultsets_label_read_metadata_tip, false, 2);
            readQueryReferences = UIUtils.createCheckbox(queriesGroup, ResultSetMessages.pref_page_database_resultsets_label_read_references,
                ResultSetMessages.pref_page_database_resultsets_label_read_references_tip, false, 2);
            queryCancelTimeout = UIUtils.createLabelText(queriesGroup, ResultSetMessages.pref_page_database_general_label_result_set_cancel_timeout + UIMessages.label_ms, "0");
            queryCancelTimeout.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            queryCancelTimeout.setToolTipText(ResultSetMessages.pref_page_database_general_label_result_set_cancel_timeout_tip);
            queryCancelTimeout.setEnabled(false);

            filterForceSubselect = UIUtils.createCheckbox(queriesGroup, ResultSetMessages.pref_page_database_resultsets_label_filter_force_subselect,
                ResultSetMessages.pref_page_database_resultsets_label_filter_force_subselect_tip, false, 2);

            readQueryMetadata.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateOptionsEnablement();
                }
            });
        }
        {
            Group advGroup = UIUtils.createControlGroup(leftPane, ResultSetMessages.pref_page_results_group_advanced, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            ignoreColumnLabelCheck = UIUtils.createCheckbox(advGroup, ResultSetMessages.pref_page_database_general_use_column_names, ResultSetMessages.pref_page_database_general_use_column_names_tip, false, 1);
            advUseFetchSize = UIUtils.createCheckbox(advGroup, ResultSetMessages.pref_page_database_resultsets_label_fetch_size, ResultSetMessages.pref_page_database_resultsets_label_fetch_size_tip, false, 1);
        }


        // Misc settings
        {
            Group miscGroup = UIUtils.createControlGroup(rightPane, ResultSetMessages.pref_page_sql_editor_group_misc, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            miscGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

            keepStatementOpenCheck = UIUtils.createCheckbox(miscGroup, ResultSetMessages.pref_page_database_general_checkbox_keep_cursor, false);
            alwaysUseAllColumns = UIUtils.createCheckbox(miscGroup, ResultSetMessages.pref_page_content_editor_checkbox_keys_always_use_all_columns, false);
            newRowsAfter = UIUtils.createCheckbox(miscGroup, ResultSetMessages.pref_page_content_editor_checkbox_new_rows_after, false);
            refreshAfterUpdate = UIUtils.createCheckbox(miscGroup, ResultSetMessages.pref_page_content_editor_checkbox_refresh_after_update, false);
            useNavigatorFilters = UIUtils.createCheckbox(miscGroup, ResultSetMessages.pref_page_content_editor_checkbox_use_navigator_filters, ResultSetMessages.pref_page_content_editor_checkbox_use_navigator_filters_tip, false, 1);
        }

        {
            Group uiGroup = UIUtils.createControlGroup(rightPane, "UI", 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            uiGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

            showErrorsInDialog = UIUtils.createCheckbox(uiGroup, "Show errors in dialog", "Show errors in modal dialog. Otherwise show errors in special data presentation (default)", false, 1);
        }

        return composite;
    }

    private void updateOptionsEnablement() {
        readQueryReferences.setEnabled(readQueryMetadata.isEnabled() && readQueryMetadata.getSelection());
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            autoFetchNextSegmentCheck.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT));
            rereadOnScrollingCheck.setSelection(store.getBoolean(ModelPreferences.RESULT_SET_REREAD_ON_SCROLLING));
            resultSetSize.setText(store.getString(ModelPreferences.RESULT_SET_MAX_ROWS));
            resultSetUseSQLCheck.setSelection(store.getBoolean(ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL));
            serverSideOrderingCheck.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_ORDER_SERVER_SIDE));
            readQueryMetadata.setSelection(store.getBoolean(ModelPreferences.RESULT_SET_READ_METADATA));
            readQueryReferences.setSelection(store.getBoolean(ModelPreferences.RESULT_SET_READ_REFERENCES));
            queryCancelTimeout.setText(store.getString(ResultSetPreferences.RESULT_SET_CANCEL_TIMEOUT));
            filterForceSubselect.setSelection(store.getBoolean(ModelPreferences.SQL_FILTER_FORCE_SUBSELECT));

            keepStatementOpenCheck.setSelection(store.getBoolean(ResultSetPreferences.KEEP_STATEMENT_OPEN));
            alwaysUseAllColumns.setSelection(store.getBoolean(ResultSetPreferences.RS_EDIT_USE_ALL_COLUMNS));
            newRowsAfter.setSelection(store.getBoolean(ResultSetPreferences.RS_EDIT_NEW_ROWS_AFTER));
            refreshAfterUpdate.setSelection(store.getBoolean(ResultSetPreferences.RS_EDIT_REFRESH_AFTER_UPDATE));
            useNavigatorFilters.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_USE_NAVIGATOR_FILTERS));

            advUseFetchSize.setSelection(store.getBoolean(ModelPreferences.RESULT_SET_USE_FETCH_SIZE));
            ignoreColumnLabelCheck.setSelection(store.getBoolean(ModelPreferences.RESULT_SET_IGNORE_COLUMN_LABEL));

            showErrorsInDialog.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ERRORS_IN_DIALOG));

            updateOptionsEnablement();
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(ResultSetPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT, autoFetchNextSegmentCheck.getSelection());
            store.setValue(ModelPreferences.RESULT_SET_REREAD_ON_SCROLLING, rereadOnScrollingCheck.getSelection());
            store.setValue(ModelPreferences.RESULT_SET_MAX_ROWS, resultSetSize.getText());
            store.setValue(ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL, resultSetUseSQLCheck.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_ORDER_SERVER_SIDE, serverSideOrderingCheck.getSelection());
            store.setValue(ModelPreferences.RESULT_SET_READ_METADATA, readQueryMetadata.getSelection());
            store.setValue(ModelPreferences.RESULT_SET_READ_REFERENCES, readQueryReferences.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_CANCEL_TIMEOUT, queryCancelTimeout.getText());
            store.setValue(ModelPreferences.SQL_FILTER_FORCE_SUBSELECT, filterForceSubselect.getSelection());

            store.setValue(ResultSetPreferences.KEEP_STATEMENT_OPEN, keepStatementOpenCheck.getSelection());
            store.setValue(ResultSetPreferences.RS_EDIT_USE_ALL_COLUMNS, alwaysUseAllColumns.getSelection());
            store.setValue(ResultSetPreferences.RS_EDIT_NEW_ROWS_AFTER, newRowsAfter.getSelection());
            store.setValue(ResultSetPreferences.RS_EDIT_REFRESH_AFTER_UPDATE, refreshAfterUpdate.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_USE_NAVIGATOR_FILTERS, useNavigatorFilters.getSelection());

            store.setValue(ModelPreferences.RESULT_SET_USE_FETCH_SIZE, advUseFetchSize.getSelection());
            store.setValue(ModelPreferences.RESULT_SET_IGNORE_COLUMN_LABEL, ignoreColumnLabelCheck.getSelection());

            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_ERRORS_IN_DIALOG, showErrorsInDialog.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(ResultSetPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT);
        store.setToDefault(ModelPreferences.RESULT_SET_REREAD_ON_SCROLLING);
        store.setToDefault(ModelPreferences.RESULT_SET_MAX_ROWS);
        store.setToDefault(ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL);
        store.setToDefault(ResultSetPreferences.RESULT_SET_ORDER_SERVER_SIDE);
        store.setToDefault(ModelPreferences.RESULT_SET_READ_METADATA);
        store.setToDefault(ModelPreferences.RESULT_SET_READ_REFERENCES);
        store.setToDefault(ResultSetPreferences.RESULT_SET_CANCEL_TIMEOUT);
        store.setToDefault(ModelPreferences.SQL_FILTER_FORCE_SUBSELECT);

        store.setToDefault(ResultSetPreferences.KEEP_STATEMENT_OPEN);
        store.setToDefault(ResultSetPreferences.RS_EDIT_USE_ALL_COLUMNS);
        store.setToDefault(ResultSetPreferences.RS_EDIT_NEW_ROWS_AFTER);
        store.setToDefault(ResultSetPreferences.RS_EDIT_REFRESH_AFTER_UPDATE);
        store.setToDefault(ResultSetPreferences.RESULT_SET_USE_NAVIGATOR_FILTERS);

        store.setToDefault(ModelPreferences.RESULT_SET_USE_FETCH_SIZE);
        store.setToDefault(ModelPreferences.RESULT_SET_IGNORE_COLUMN_LABEL);

        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_ERRORS_IN_DIALOG);

        updateOptionsEnablement();
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
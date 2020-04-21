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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageResultSetPresentation
 */
public class PrefPageResultSetPresentation extends TargetPrefPage
{
    private static final Log log = Log.getLog(PrefPageResultSetPresentation.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset.presentation"; //$NON-NLS-1$

    private Button autoSwitchMode;
    private Button showDescription;
    private Button columnWidthByValue;
    private Button showConnectionName;
    private Button transformComplexTypes;
    private Button rightJustifyNumbers;
    private Button rightJustifyDateTime;
    private Button autoCompleteProposal;

    public PrefPageResultSetPresentation()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
        	store.contains(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION) ||
            store.contains(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME) ||	
        	store.contains(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES) ||
            store.contains(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS) ||
            store.contains(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME) ||
            store.contains(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL);
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
            Group uiGroup = UIUtils.createControlGroup(composite, ResultSetMessages.pref_page_database_resultsets_group_common, 1, SWT.NONE, 0);
            ((GridData)uiGroup.getLayoutData()).horizontalSpan = 2;
            autoSwitchMode = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_switch_mode_on_rows, false);
            showDescription = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_show_column_description, false);
            columnWidthByValue = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_calc_column_width_by_values, ResultSetMessages.pref_page_database_resultsets_label_calc_column_width_by_values_tip, false, 1);
            showConnectionName = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_show_connection_name, false);
            transformComplexTypes = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_structurize_complex_types, ResultSetMessages.pref_page_database_resultsets_label_structurize_complex_types_tip, false, 1);
            rightJustifyNumbers = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_right_justify_numbers_and_date, null, false, 1);
            rightJustifyDateTime = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_right_justify_datetime, null, false, 1);
            autoCompleteProposal = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_auto_completion, ResultSetMessages.pref_page_database_resultsets_label_auto_completion_tip, true, 1);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
        	autoSwitchMode.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE));
            showDescription.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION));
            columnWidthByValue.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES));
            showConnectionName.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME));
            rightJustifyNumbers.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS));
            rightJustifyDateTime.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME));
            transformComplexTypes.setSelection(store.getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES));
            autoCompleteProposal.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE, autoSwitchMode.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION, showDescription.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES, columnWidthByValue.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME, showConnectionName.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS, rightJustifyNumbers.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME, rightJustifyDateTime.getSelection());
            store.setValue(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES, transformComplexTypes.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL, autoCompleteProposal.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
    	store.setToDefault(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION);
        store.setToDefault(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME);
        store.setToDefault(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME);
        store.setToDefault(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES);  
        store.setToDefault(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
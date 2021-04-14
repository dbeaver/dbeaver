/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ValueFormatSelector;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageResultSetPlainText
 */
public class PrefPageResultSetPresentationPlainText extends TargetPrefPage
{
    private static final Log log = Log.getLog(PrefPageResultSetPresentationPlainText.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset.plain.text"; //$NON-NLS-1$

    private Spinner textTabSize;
    private Spinner textMaxColumnSize;
    private ValueFormatSelector textValueFormat;
    private Button showNulls;
    private Button textDelimiterLeading;
    private Button textDelimiterTrailing;
    private Button textDelimiterTop;
    private Button textDelimiterBottom;
    private Button textExtraSpaces;

    public PrefPageResultSetPresentationPlainText()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ResultSetPreferences.RESULT_TEXT_TAB_SIZE) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_DELIMITER_TOP) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_DELIMITER_BOTTOM) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES);
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
            Group uiGroup = UIUtils.createControlGroup(composite, DataEditorsMessages.pref_page_database_resultsets_group_plain_text, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            textTabSize = UIUtils.createLabelSpinner(uiGroup, DataEditorsMessages.pref_page_database_resultsets_label_tab_width, 0, 1, 100);
            textMaxColumnSize = UIUtils.createLabelSpinner(uiGroup, DataEditorsMessages.pref_page_database_resultsets_label_maximum_column_length, 0, 10, Integer.MAX_VALUE);
            textValueFormat = new ValueFormatSelector(uiGroup);
            showNulls = UIUtils.createCheckbox(uiGroup, DataEditorsMessages.pref_page_database_resultsets_label_text_show_nulls, null, false, 2);
            textDelimiterLeading = UIUtils.createCheckbox(uiGroup, DataEditorsMessages.pref_page_database_resultsets_label_text_delimiter_leading, null, false, 2);
            textDelimiterTrailing = UIUtils.createCheckbox(uiGroup, DataEditorsMessages.pref_page_database_resultsets_label_text_delimiter_trailing, null, false, 2);
            textDelimiterTop = UIUtils.createCheckbox(uiGroup, DataEditorsMessages.pref_page_database_resultsets_label_text_delimiter_top, null, false, 2);
            textDelimiterBottom = UIUtils.createCheckbox(uiGroup, DataEditorsMessages.pref_page_database_resultsets_label_text_delimiter_bottom, null, false, 2);
            textExtraSpaces = UIUtils.createCheckbox(uiGroup, DataEditorsMessages.pref_page_database_resultsets_label_text_extra_spaces, null, false, 2);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            textTabSize.setSelection(store.getInt(ResultSetPreferences.RESULT_TEXT_TAB_SIZE));
            textMaxColumnSize.setSelection(store.getInt(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE));
            textValueFormat.select(DBDDisplayFormat.safeValueOf(store.getString(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT)));
            showNulls.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS));
            textDelimiterLeading.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING));
            textDelimiterTrailing.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING));
            textDelimiterTop.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_TOP));
            textDelimiterBottom.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_BOTTOM));
            textExtraSpaces.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(ResultSetPreferences.RESULT_TEXT_TAB_SIZE, textTabSize.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE, textMaxColumnSize.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT, textValueFormat.getSelection().name());
            store.setValue(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS, showNulls.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING, textDelimiterLeading.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING, textDelimiterTrailing.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_DELIMITER_TOP, textDelimiterTop.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_DELIMITER_BOTTOM, textDelimiterBottom.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES, textExtraSpaces.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_TAB_SIZE);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_DELIMITER_TOP);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_DELIMITER_BOTTOM);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
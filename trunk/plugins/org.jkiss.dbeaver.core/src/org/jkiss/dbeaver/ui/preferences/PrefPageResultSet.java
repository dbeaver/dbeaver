/*
 * Copyright (C) 2010-2012 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

/**
 * PrefPageResultSet
 */
public class PrefPageResultSet extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset"; //$NON-NLS-1$

    private Spinner resultSetSize;
    private Button binaryShowStrings;
    private Spinner binaryStringMaxLength;

    private Button keepStatementOpenCheck;
    private Button rollbackOnErrorCheck;
    private Spinner memoryContentSize;
    private Button readExpensiveCheck;

    public PrefPageResultSet()
    {
        super();
        setPreferenceStore(DBeaverCore.getGlobalPreferenceStore());
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(PrefConstants.RESULT_SET_MAX_ROWS) ||
                store.contains(PrefConstants.QUERY_ROLLBACK_ON_ERROR) ||
                store.contains(PrefConstants.KEEP_STATEMENT_OPEN) ||
                store.contains(PrefConstants.MEMORY_CONTENT_MAX_SIZE) ||
                store.contains(PrefConstants.READ_EXPENSIVE_PROPERTIES) ||
                store.contains(PrefConstants.RESULT_SET_BINARY_SHOW_STRINGS) ||
            store.contains(PrefConstants.RESULT_SET_BINARY_STRING_MAX_LEN)
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
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        {
            Group queriesGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_queries, 2, SWT.NONE, 0);
            UIUtils.createControlLabel(queriesGroup, CoreMessages.pref_page_database_general_label_result_set_max_size);

            resultSetSize = new Spinner(queriesGroup, SWT.BORDER);
            resultSetSize.setSelection(0);
            resultSetSize.setDigits(0);
            resultSetSize.setIncrement(1);
            resultSetSize.setMinimum(1);
            resultSetSize.setMaximum(1024 * 1024);
        }

        // General settings
        {
            Group txnGroup = new Group(composite, SWT.NONE);
            txnGroup.setText(CoreMessages.pref_page_database_general_group_transactions);
            txnGroup.setLayout(new GridLayout(2, false));

            keepStatementOpenCheck = UIUtils.createLabelCheckbox(txnGroup, CoreMessages.pref_page_database_general_checkbox_keep_cursor, false);
            rollbackOnErrorCheck = UIUtils.createLabelCheckbox(txnGroup, CoreMessages.pref_page_database_general_checkbox_rollback_on_error, false);
        }

        {
            Group performanceGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_performance, 2, SWT.NONE, 0);

            readExpensiveCheck = UIUtils.createLabelCheckbox(performanceGroup, CoreMessages.pref_page_database_general_checkbox_show_row_count, false);

            UIUtils.createControlLabel(performanceGroup, CoreMessages.pref_page_database_general_label_max_lob_length);

            memoryContentSize = new Spinner(performanceGroup, SWT.BORDER);
            memoryContentSize.setSelection(0);
            memoryContentSize.setDigits(0);
            memoryContentSize.setIncrement(1);
            memoryContentSize.setMinimum(0);
            memoryContentSize.setMaximum(1024 * 1024 * 1024);
        }

        {
            Group performanceGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_resultsets_group_binary, 2, SWT.NONE, 0);

            binaryShowStrings = UIUtils.createLabelCheckbox(performanceGroup, CoreMessages.pref_page_database_resultsets_label_binary_use_strings, false);

            UIUtils.createControlLabel(performanceGroup, CoreMessages.pref_page_database_resultsets_label_binary_strings_max_length);

            binaryStringMaxLength = new Spinner(performanceGroup, SWT.BORDER);
            binaryStringMaxLength.setSelection(0);
            binaryStringMaxLength.setDigits(0);
            binaryStringMaxLength.setIncrement(1);
            binaryStringMaxLength.setMinimum(0);
            binaryStringMaxLength.setMaximum(10000);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            resultSetSize.setSelection(store.getInt(PrefConstants.RESULT_SET_MAX_ROWS));
            keepStatementOpenCheck.setSelection(store.getBoolean(PrefConstants.KEEP_STATEMENT_OPEN));
            rollbackOnErrorCheck.setSelection(store.getBoolean(PrefConstants.QUERY_ROLLBACK_ON_ERROR));
            memoryContentSize.setSelection(store.getInt(PrefConstants.MEMORY_CONTENT_MAX_SIZE));
            readExpensiveCheck.setSelection(store.getBoolean(PrefConstants.READ_EXPENSIVE_PROPERTIES));
            binaryShowStrings.setSelection(store.getBoolean(PrefConstants.RESULT_SET_BINARY_SHOW_STRINGS));
            binaryStringMaxLength.setSelection(store.getInt(PrefConstants.RESULT_SET_BINARY_STRING_MAX_LEN));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(PrefConstants.RESULT_SET_MAX_ROWS, resultSetSize.getSelection());
            store.setValue(PrefConstants.KEEP_STATEMENT_OPEN, keepStatementOpenCheck.getSelection());
            store.setValue(PrefConstants.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
            store.setValue(PrefConstants.MEMORY_CONTENT_MAX_SIZE, memoryContentSize.getSelection());
            store.setValue(PrefConstants.READ_EXPENSIVE_PROPERTIES, readExpensiveCheck.getSelection());
            store.setValue(PrefConstants.RESULT_SET_BINARY_SHOW_STRINGS, binaryShowStrings.getSelection());
            store.setValue(PrefConstants.RESULT_SET_BINARY_STRING_MAX_LEN, binaryStringMaxLength.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(PrefConstants.RESULT_SET_MAX_ROWS);
        store.setToDefault(PrefConstants.KEEP_STATEMENT_OPEN);
        store.setToDefault(PrefConstants.QUERY_ROLLBACK_ON_ERROR);
        store.setToDefault(PrefConstants.MEMORY_CONTENT_MAX_SIZE);
        store.setToDefault(PrefConstants.READ_EXPENSIVE_PROPERTIES);
        store.setToDefault(PrefConstants.RESULT_SET_BINARY_SHOW_STRINGS);
        store.setToDefault(PrefConstants.RESULT_SET_BINARY_STRING_MAX_LEN);
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
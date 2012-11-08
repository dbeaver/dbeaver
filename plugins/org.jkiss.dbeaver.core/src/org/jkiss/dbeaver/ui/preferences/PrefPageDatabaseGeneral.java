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
 * PrefPageDatabaseGeneral
 */
public class PrefPageDatabaseGeneral extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.common"; //$NON-NLS-1$

    private Button autoCommitCheck;
    private Button keepStatementOpenCheck;
    private Button rollbackOnErrorCheck;
    private Spinner memoryContentSize;
    private Button readExpensiveCheck;
    private Button caseSensitiveNamesCheck;

    public PrefPageDatabaseGeneral()
    {
        super();
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(PrefConstants.QUERY_ROLLBACK_ON_ERROR) ||
            store.contains(PrefConstants.DEFAULT_AUTO_COMMIT) ||
            store.contains(PrefConstants.KEEP_STATEMENT_OPEN) ||
            store.contains(PrefConstants.MEMORY_CONTENT_MAX_SIZE) ||
            store.contains(PrefConstants.READ_EXPENSIVE_PROPERTIES) ||
            store.contains(PrefConstants.META_CASE_SENSITIVE)
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

        // General settings
        {
            Group txnGroup = new Group(composite, SWT.NONE);
            txnGroup.setText(CoreMessages.pref_page_database_general_group_transactions);
            txnGroup.setLayout(new GridLayout(2, false));

            autoCommitCheck = UIUtils.createLabelCheckbox(txnGroup, CoreMessages.pref_page_database_general_checkbox_auto_commit_by_default, false);
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
            Group metadataGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_metadata, 2, SWT.NONE, 0);

            caseSensitiveNamesCheck = UIUtils.createLabelCheckbox(metadataGroup, CoreMessages.pref_page_database_general_checkbox_case_sensitive_names, false);
        }
        return composite;
    }

    @Override
    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            autoCommitCheck.setSelection(store.getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT));
            keepStatementOpenCheck.setSelection(store.getBoolean(PrefConstants.KEEP_STATEMENT_OPEN));
            rollbackOnErrorCheck.setSelection(store.getBoolean(PrefConstants.QUERY_ROLLBACK_ON_ERROR));
            memoryContentSize.setSelection(store.getInt(PrefConstants.MEMORY_CONTENT_MAX_SIZE));
            readExpensiveCheck.setSelection(store.getBoolean(PrefConstants.READ_EXPENSIVE_PROPERTIES));
            caseSensitiveNamesCheck.setSelection(store.getBoolean(PrefConstants.META_CASE_SENSITIVE));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(PrefConstants.DEFAULT_AUTO_COMMIT, autoCommitCheck.getSelection());
            store.setValue(PrefConstants.KEEP_STATEMENT_OPEN, keepStatementOpenCheck.getSelection());
            store.setValue(PrefConstants.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
            store.setValue(PrefConstants.MEMORY_CONTENT_MAX_SIZE, memoryContentSize.getSelection());
            store.setValue(PrefConstants.READ_EXPENSIVE_PROPERTIES, readExpensiveCheck.getSelection());
            store.setValue(PrefConstants.META_CASE_SENSITIVE, caseSensitiveNamesCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(PrefConstants.DEFAULT_AUTO_COMMIT);
        store.setToDefault(PrefConstants.KEEP_STATEMENT_OPEN);
        store.setToDefault(PrefConstants.QUERY_ROLLBACK_ON_ERROR);
        store.setToDefault(PrefConstants.MEMORY_CONTENT_MAX_SIZE);
        store.setToDefault(PrefConstants.READ_EXPENSIVE_PROPERTIES);
        store.setToDefault(PrefConstants.META_CASE_SENSITIVE);
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
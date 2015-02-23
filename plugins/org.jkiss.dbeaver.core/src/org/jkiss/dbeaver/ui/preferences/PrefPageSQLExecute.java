/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageSQLExecute
 */
public class PrefPageSQLExecute extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqlexecute"; //$NON-NLS-1$

    private Button invalidateBeforeExecuteCheck;
    private Spinner executeTimeoutText;

    private Combo commitTypeCombo;
    private Combo errorHandlingCombo;
    private Spinner commitLinesText;
    private Button fetchResultSetsCheck;

    public PrefPageSQLExecute()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE) ||
            store.contains(DBeaverPreferences.STATEMENT_TIMEOUT) ||
            store.contains(DBeaverPreferences.SCRIPT_COMMIT_TYPE) ||
            store.contains(DBeaverPreferences.SCRIPT_ERROR_HANDLING) ||
            store.contains(DBeaverPreferences.SCRIPT_COMMIT_LINES) ||
            store.contains(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS)
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
            Composite commonGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_editor_group_common, 2, GridData.FILL_HORIZONTAL, 0);
            {
                invalidateBeforeExecuteCheck = UIUtils.createLabelCheckbox(commonGroup, CoreMessages.pref_page_sql_editor_label_invalidate_before_execute, false);

                UIUtils.createControlLabel(commonGroup, CoreMessages.pref_page_sql_editor_label_sql_timeout);
                executeTimeoutText = new Spinner(commonGroup, SWT.BORDER);
                executeTimeoutText.setSelection(0);
                executeTimeoutText.setDigits(0);
                executeTimeoutText.setIncrement(1);
                executeTimeoutText.setMinimum(1);
                executeTimeoutText.setMaximum(100000);
            }
        }

        // Scripts
        {
            Composite scriptsGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_editor_group_scripts, 2, GridData.FILL_HORIZONTAL, 0);

            {
                UIUtils.createControlLabel(scriptsGroup, CoreMessages.pref_page_sql_editor_label_commit_type);

                commitTypeCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                commitTypeCombo.add(CoreMessages.pref_page_sql_editor_combo_item_script_end, SQLScriptCommitType.AT_END.ordinal());
                commitTypeCombo.add(CoreMessages.pref_page_sql_editor_combo_item_each_line_autocommit, SQLScriptCommitType.AUTOCOMMIT.ordinal());
                commitTypeCombo.add(CoreMessages.pref_page_sql_editor_combo_item_each_spec_line, SQLScriptCommitType.NLINES.ordinal());
                commitTypeCombo.add(CoreMessages.pref_page_sql_editor_combo_item_no_commit, SQLScriptCommitType.NO_COMMIT.ordinal());
            }

            {
                UIUtils.createControlLabel(scriptsGroup, CoreMessages.pref_page_sql_editor_label_commit_after_line);
                commitLinesText = new Spinner(scriptsGroup, SWT.BORDER);
                commitLinesText.setSelection(0);
                commitLinesText.setDigits(0);
                commitLinesText.setIncrement(1);
                commitLinesText.setMinimum(1);
                commitLinesText.setMaximum(1024 * 1024);
            }

            {
                UIUtils.createControlLabel(scriptsGroup, CoreMessages.pref_page_sql_editor_label_error_handling);

                errorHandlingCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                errorHandlingCombo.add(CoreMessages.pref_page_sql_editor_combo_item_stop_rollback, SQLScriptErrorHandling.STOP_ROLLBACK.ordinal());
                errorHandlingCombo.add(CoreMessages.pref_page_sql_editor_combo_item_stop_commit, SQLScriptErrorHandling.STOP_COMMIT.ordinal());
                errorHandlingCombo.add(CoreMessages.pref_page_sql_editor_combo_item_ignore, SQLScriptErrorHandling.IGNORE.ordinal());
            }

            fetchResultSetsCheck = UIUtils.createLabelCheckbox(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_fetch_resultsets, false);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            invalidateBeforeExecuteCheck.setSelection(store.getBoolean(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE));
            executeTimeoutText.setSelection(store.getInt(DBeaverPreferences.STATEMENT_TIMEOUT));

            commitTypeCombo.select(SQLScriptCommitType.valueOf(store.getString(DBeaverPreferences.SCRIPT_COMMIT_TYPE)).ordinal());
            errorHandlingCombo.select(SQLScriptErrorHandling.valueOf(store.getString(DBeaverPreferences.SCRIPT_ERROR_HANDLING)).ordinal());
            commitLinesText.setSelection(store.getInt(DBeaverPreferences.SCRIPT_COMMIT_LINES));
            fetchResultSetsCheck.setSelection(store.getBoolean(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE, invalidateBeforeExecuteCheck.getSelection());
            store.setValue(DBeaverPreferences.STATEMENT_TIMEOUT, executeTimeoutText.getSelection());

            store.setValue(DBeaverPreferences.SCRIPT_COMMIT_TYPE, CommonUtils.fromOrdinal(SQLScriptCommitType.class, commitTypeCombo.getSelectionIndex()).name());
            store.setValue(DBeaverPreferences.SCRIPT_COMMIT_LINES, commitLinesText.getSelection());
            store.setValue(DBeaverPreferences.SCRIPT_ERROR_HANDLING, CommonUtils.fromOrdinal(SQLScriptErrorHandling.class, errorHandlingCombo.getSelectionIndex()).name());
            store.setValue(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS, fetchResultSetsCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE);
        store.setToDefault(DBeaverPreferences.STATEMENT_TIMEOUT);

        store.setToDefault(DBeaverPreferences.SCRIPT_COMMIT_TYPE);
        store.setToDefault(DBeaverPreferences.SCRIPT_COMMIT_LINES);
        store.setToDefault(DBeaverPreferences.SCRIPT_ERROR_HANDLING);
        store.setToDefault(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS);
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
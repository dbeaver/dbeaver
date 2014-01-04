/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.eclipse.ui.texteditor.rulers.RulerColumnPreferenceAdapter;
import org.eclipse.ui.texteditor.rulers.RulerColumnRegistry;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * PrefPageSQL
 */
public class PrefPageSQLEditor extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqleditor"; //$NON-NLS-1$

    private Button invalidateBeforeExecuteCheck;
    private Spinner executeTimeoutText;

    private Combo commitTypeCombo;
    private Combo errorHandlingCombo;
    private Spinner commitLinesText;
    private Button fetchResultSetsCheck;
    private Button autoFoldersCheck;
    private Button csAutoActivationCheck;
    private Spinner csAutoActivationDelaySpinner;
    private Button csAutoInsertCheck;
    private Combo csInsertCase;
    private Map<RulerColumnDescriptor, Button> rulerChecks = new HashMap<RulerColumnDescriptor, Button>();
    private Button acSingleQuotesCheck;
    private Button acDoubleQuotesCheck;
    private Button acBracketsCheck;

    public PrefPageSQLEditor()
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
            store.contains(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS) ||
            store.contains(DBeaverPreferences.SCRIPT_AUTO_FOLDERS) ||
            store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES) ||
            store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES) ||
            store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS)
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

        Composite composite2 = UIUtils.createPlaceholder(composite, 2);
        ((GridLayout)composite2.getLayout()).horizontalSpacing = 5;
        composite2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // Content assistant
        {
            Composite assistGroup = UIUtils.createControlGroup(composite2, "Content assistant", 2, GridData.FILL_HORIZONTAL, 0);
            assistGroup.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));

            csAutoActivationCheck = UIUtils.createLabelCheckbox(assistGroup, "Enable auto activation", "Enables the content assistant's auto activation", false);
            UIUtils.createControlLabel(assistGroup, "Auto activation delay");
            csAutoActivationDelaySpinner = new Spinner(assistGroup, SWT.BORDER);
            csAutoActivationDelaySpinner.setSelection(0);
            csAutoActivationDelaySpinner.setDigits(0);
            csAutoActivationDelaySpinner.setIncrement(50);
            csAutoActivationDelaySpinner.setMinimum(0);
            csAutoActivationDelaySpinner.setMaximum(1000000);
            csAutoInsertCheck = UIUtils.createLabelCheckbox(
                assistGroup,
                "Auto-insert proposal",
                "Enables the content assistant's auto insertion mode.\nIf enabled, the content assistant inserts a proposal automatically if it is the only proposal.\nIn the case of ambiguities, the user must make the choice.",
                false);
            UIUtils.createControlLabel(assistGroup, "Insert case");
            csInsertCase = new Combo(assistGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            csInsertCase.add("Default");
            csInsertCase.add("Upper case");
            csInsertCase.add("Lower case");
        }

        // Autoclose
        {
            Composite acGroup = UIUtils.createControlGroup(composite2, "Auto close", 2, GridData.FILL_BOTH, 0);

            acSingleQuotesCheck = UIUtils.createLabelCheckbox(acGroup, "Single quotes", false);
            acDoubleQuotesCheck = UIUtils.createLabelCheckbox(acGroup, "Double quotes", false);
            acBracketsCheck = UIUtils.createLabelCheckbox(acGroup, "Brackets", false);
        }

        // Reulers
        {
            Composite rulersGroup = UIUtils.createControlGroup(composite2, "Rulers", 2, GridData.FILL_HORIZONTAL, 0);
            rulersGroup.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));

            for (Object obj : RulerColumnRegistry.getDefault().getColumnDescriptors()) {
                final RulerColumnDescriptor descriptor = (RulerColumnDescriptor)obj;
                if (!descriptor.isGlobal())
                    continue;
                Button checkbox = UIUtils.createCheckbox(rulersGroup, descriptor.getName(), false);
                rulerChecks.put(descriptor, checkbox);
            }
        }

        // Scripts
        {
            Composite scriptsGroup = UIUtils.createControlGroup(composite2, CoreMessages.pref_page_sql_editor_group_resources, 2, GridData.FILL_BOTH, 0);

            autoFoldersCheck = UIUtils.createLabelCheckbox(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_put_new_scripts, false);
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
            autoFoldersCheck.setSelection(store.getBoolean(DBeaverPreferences.SCRIPT_AUTO_FOLDERS));
            csAutoActivationCheck.setSelection(store.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION));
            csAutoActivationDelaySpinner.setSelection(store.getInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
            csAutoInsertCheck.setSelection(store.getBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
            csInsertCase.select(store.getInt(SQLPreferenceConstants.PROPOSAL_INSERT_CASE));
            acSingleQuotesCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES));
            acDoubleQuotesCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES));
            acBracketsCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS));

            final RulerColumnPreferenceAdapter adapter = new RulerColumnPreferenceAdapter(
                store,
                AbstractTextEditor.PREFERENCE_RULER_CONTRIBUTIONS);
            for (Map.Entry<RulerColumnDescriptor, Button> entry : rulerChecks.entrySet()) {
                entry.getValue().setSelection(adapter.isEnabled(entry.getKey()));
            }

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

            store.setValue(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, csAutoActivationCheck.getSelection());
            store.setValue(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, csAutoActivationDelaySpinner.getSelection());
            store.setValue(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, csAutoInsertCheck.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSAL_INSERT_CASE, csInsertCase.getSelectionIndex());

            store.setValue(DBeaverPreferences.SCRIPT_COMMIT_TYPE, CommonUtils.fromOrdinal(SQLScriptCommitType.class, commitTypeCombo.getSelectionIndex()).name());
            store.setValue(DBeaverPreferences.SCRIPT_COMMIT_LINES, commitLinesText.getSelection());
            store.setValue(DBeaverPreferences.SCRIPT_ERROR_HANDLING, CommonUtils.fromOrdinal(SQLScriptErrorHandling.class, errorHandlingCombo.getSelectionIndex()).name());
            store.setValue(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS, fetchResultSetsCheck.getSelection());
            store.setValue(DBeaverPreferences.SCRIPT_AUTO_FOLDERS, autoFoldersCheck.getSelection());

            store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, acSingleQuotesCheck.getSelection());
            store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, acDoubleQuotesCheck.getSelection());
            store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, acBracketsCheck.getSelection());

            final RulerColumnPreferenceAdapter adapter = new RulerColumnPreferenceAdapter(
                    store,
                    AbstractTextEditor.PREFERENCE_RULER_CONTRIBUTIONS);
            for (Map.Entry<RulerColumnDescriptor, Button> entry : rulerChecks.entrySet()) {
                adapter.setEnabled(entry.getKey(), entry.getValue().getSelection());
            }
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

        store.setToDefault(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION);
        store.setToDefault(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY);
        store.setToDefault(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO);
        store.setToDefault(SQLPreferenceConstants.PROPOSAL_INSERT_CASE);

        store.setToDefault(DBeaverPreferences.SCRIPT_COMMIT_TYPE);
        store.setToDefault(DBeaverPreferences.SCRIPT_COMMIT_LINES);
        store.setToDefault(DBeaverPreferences.SCRIPT_ERROR_HANDLING);
        store.setToDefault(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS);
        store.setToDefault(DBeaverPreferences.SCRIPT_AUTO_FOLDERS);

        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS);
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
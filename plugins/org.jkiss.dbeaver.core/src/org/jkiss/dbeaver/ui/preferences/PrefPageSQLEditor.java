/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

/**
 * PrefPageSQL
 */
public class PrefPageSQLEditor extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqleditor";

    private Spinner executeTimeoutText;

    private Combo commitTypeCombo;
    private Combo errorHandlingCombo;
    private Spinner commitLinesText;
    private Button fetchResultSetsCheck;
    private Button autoFoldersCheck;

    public PrefPageSQLEditor()
    {
        super();
    }

    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(PrefConstants.STATEMENT_TIMEOUT) ||
            store.contains(PrefConstants.SCRIPT_COMMIT_TYPE) ||
            store.contains(PrefConstants.SCRIPT_ERROR_HANDLING) ||
            store.contains(PrefConstants.SCRIPT_COMMIT_LINES) ||
            store.contains(PrefConstants.SCRIPT_FETCH_RESULT_SETS) ||
            store.contains(PrefConstants.SCRIPT_AUTO_FOLDERS)
        ;
    }

    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    protected Control createPreferenceContent(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        // General settings
        {
            Composite commonGroup = UIUtils.createControlGroup(composite, "Common", 2, SWT.NONE, 0);
            {
                UIUtils.createControlLabel(commonGroup, "SQL statement timeout");

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
            Composite scriptsGroup = UIUtils.createControlGroup(composite, "Scripts", 2, SWT.NONE, 0);

            {
                UIUtils.createControlLabel(scriptsGroup, "Commit type");

                commitTypeCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                commitTypeCombo.add("At script end", SQLScriptCommitType.AT_END.ordinal());
                commitTypeCombo.add("After each line (autocommit)", SQLScriptCommitType.AUTOCOMMIT.ordinal());
                commitTypeCombo.add("After each specified line", SQLScriptCommitType.NLINES.ordinal());
                commitTypeCombo.add("No commit", SQLScriptCommitType.NO_COMMIT.ordinal());
            }

            {
                UIUtils.createControlLabel(scriptsGroup, "Commit after line");
                commitLinesText = new Spinner(scriptsGroup, SWT.BORDER);
                commitLinesText.setSelection(0);
                commitLinesText.setDigits(0);
                commitLinesText.setIncrement(1);
                commitLinesText.setMinimum(1);
                commitLinesText.setMaximum(1024 * 1024);
            }

            {
                UIUtils.createControlLabel(scriptsGroup, "Error handling");

                errorHandlingCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                errorHandlingCombo.add("Stop + rollback", SQLScriptErrorHandling.STOP_ROLLBACK.ordinal());
                errorHandlingCombo.add("Stop + commit", SQLScriptErrorHandling.STOP_COMMIT.ordinal());
                errorHandlingCombo.add("Ignore", SQLScriptErrorHandling.IGNORE.ordinal());
            }

            fetchResultSetsCheck = UIUtils.createLabelCheckbox(scriptsGroup, "Fetch resultsets", false);
        }

        // Scripts
        {
            Composite scriptsGroup = UIUtils.createControlGroup(composite, "Resources", 2, SWT.NONE, 0);

            autoFoldersCheck = UIUtils.createLabelCheckbox(scriptsGroup, "Put new scripts in folders", false);
        }
        return composite;
    }

    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            executeTimeoutText.setSelection(store.getInt(PrefConstants.STATEMENT_TIMEOUT));

            commitTypeCombo.select(SQLScriptCommitType.valueOf(store.getString(PrefConstants.SCRIPT_COMMIT_TYPE)).ordinal());
            errorHandlingCombo.select(SQLScriptErrorHandling.valueOf(store.getString(PrefConstants.SCRIPT_ERROR_HANDLING)).ordinal());
            commitLinesText.setSelection(store.getInt(PrefConstants.SCRIPT_COMMIT_LINES));
            fetchResultSetsCheck.setSelection(store.getBoolean(PrefConstants.SCRIPT_FETCH_RESULT_SETS));
            autoFoldersCheck.setSelection(store.getBoolean(PrefConstants.SCRIPT_AUTO_FOLDERS));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(PrefConstants.STATEMENT_TIMEOUT, executeTimeoutText.getSelection());

            store.setValue(PrefConstants.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.fromOrdinal(commitTypeCombo.getSelectionIndex()).name());
            store.setValue(PrefConstants.SCRIPT_COMMIT_LINES, commitLinesText.getSelection());
            store.setValue(PrefConstants.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.fromOrdinal(errorHandlingCombo.getSelectionIndex()).name());
            store.setValue(PrefConstants.SCRIPT_FETCH_RESULT_SETS, fetchResultSetsCheck.getSelection());
            store.setValue(PrefConstants.SCRIPT_AUTO_FOLDERS, autoFoldersCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(PrefConstants.STATEMENT_TIMEOUT);

        store.setToDefault(PrefConstants.SCRIPT_COMMIT_TYPE);
        store.setToDefault(PrefConstants.SCRIPT_COMMIT_LINES);
        store.setToDefault(PrefConstants.SCRIPT_ERROR_HANDLING);
        store.setToDefault(PrefConstants.SCRIPT_FETCH_RESULT_SETS);
        store.setToDefault(PrefConstants.SCRIPT_AUTO_FOLDERS);
    }

    public void applyData(Object data)
    {
        super.applyData(data);
    }

    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
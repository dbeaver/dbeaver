/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.DBeaverUtils;

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
    private Button fetchResultSets;

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
            store.contains(PrefConstants.SCRIPT_FETCH_RESULT_SETS)
        ;
    }

    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    protected Control createPreferenceContent(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        // General settings
        {
            Group commonGroup = new Group(composite, SWT.NONE);
            commonGroup.setText("Common");
            commonGroup.setLayout(new GridLayout(2, false));

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
            Group scriptsGroup = new Group(composite, SWT.NONE);
            scriptsGroup.setText("Scripts");
            scriptsGroup.setLayout(new GridLayout(2, false));

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

            fetchResultSets = UIUtils.createLabelCheckbox(scriptsGroup, "Fetch resultsets", false);
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
            fetchResultSets.setSelection(store.getBoolean(PrefConstants.SCRIPT_FETCH_RESULT_SETS));
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
            store.setValue(PrefConstants.SCRIPT_FETCH_RESULT_SETS, fetchResultSets.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        DBeaverUtils.savePreferenceStore(store);
    }

    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(PrefConstants.STATEMENT_TIMEOUT);

        store.setToDefault(PrefConstants.SCRIPT_COMMIT_TYPE);
        store.setToDefault(PrefConstants.SCRIPT_COMMIT_LINES);
        store.setToDefault(PrefConstants.SCRIPT_ERROR_HANDLING);
        store.setToDefault(PrefConstants.SCRIPT_FETCH_RESULT_SETS);
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
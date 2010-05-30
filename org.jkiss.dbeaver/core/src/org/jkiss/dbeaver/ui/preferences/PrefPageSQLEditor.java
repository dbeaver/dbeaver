/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.io.IOException;

/**
 * PrefPageSQL
 */
public class PrefPageSQLEditor extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqleditor";

    private Spinner resultSetSize;
    private Button autoCommitCheck;
    private Spinner executeTimeoutText;

    private Combo commitTypeCombo;
    private Combo errorHandlingCombo;
    private Spinner commitLinesText;
    private Button fetchResultSets;

    private Spinner maxTextContentSize;
    private Button editLongAsLobCheck;
    private Button commitOnEditApplyCheck;
    private Button commitOnContentApplyCheck;

    public PrefPageSQLEditor()
    {
        super();
    }

    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(PrefConstants.RESULT_SET_MAX_ROWS) ||
            store.contains(PrefConstants.DEFAULT_AUTO_COMMIT) ||
                store.contains(PrefConstants.STATEMENT_TIMEOUT) ||

                store.contains(PrefConstants.SCRIPT_COMMIT_TYPE) ||
                store.contains(PrefConstants.SCRIPT_ERROR_HANDLING) ||
                store.contains(PrefConstants.SCRIPT_COMMIT_LINES) ||
                store.contains(PrefConstants.SCRIPT_FETCH_RESULT_SETS) ||

                store.contains(PrefConstants.RS_EDIT_MAX_TEXT_SIZE) ||
                store.contains(PrefConstants.RS_EDIT_LONG_AS_LOB) ||
                store.contains(PrefConstants.RS_COMMIT_ON_EDIT_APPLY) ||
                store.contains(PrefConstants.RS_COMMIT_ON_CONTENT_APPLY)
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
                Label rsSizeLabel = new Label(commonGroup, SWT.NONE);
                rsSizeLabel.setText("ResultSet maximum size:");

                resultSetSize = new Spinner(commonGroup, SWT.BORDER);
                resultSetSize.setSelection(0);
                resultSetSize.setDigits(0);
                resultSetSize.setIncrement(1);
                resultSetSize.setMinimum(1);
                resultSetSize.setMaximum(1024 * 1024);
            }

            {
                Label acEnabledLabel = new Label(commonGroup, SWT.NONE);
                acEnabledLabel.setText("Auto-commit by default:");

                autoCommitCheck = new Button(commonGroup, SWT.CHECK);
                autoCommitCheck.setText("Enabled");
            }

            {
                Label executeTimeoutLabel = new Label(commonGroup, SWT.NONE);
                executeTimeoutLabel.setText("SQL statement timeout:");

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
                Label commitTypeLabel = new Label(scriptsGroup, SWT.NONE);
                commitTypeLabel.setText("Commit type:");

                commitTypeCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                commitTypeCombo.add("At script end", SQLScriptCommitType.AT_END.ordinal());
                commitTypeCombo.add("After each line (autocommit)", SQLScriptCommitType.AUTOCOMMIT.ordinal());
                commitTypeCombo.add("After each specified line", SQLScriptCommitType.NLINES.ordinal());
                commitTypeCombo.add("No commit", SQLScriptCommitType.NO_COMMIT.ordinal());
            }

            {
                Label commitLinesLabel = new Label(scriptsGroup, SWT.NONE);
                commitLinesLabel.setText("Commit after line:");
                commitLinesText = new Spinner(scriptsGroup, SWT.BORDER);
                commitLinesText.setSelection(0);
                commitLinesText.setDigits(0);
                commitLinesText.setIncrement(1);
                commitLinesText.setMinimum(1);
                commitLinesText.setMaximum(1024 * 1024);
            }

            {
                Label errorHandlingLabel = new Label(scriptsGroup, SWT.NONE);
                errorHandlingLabel.setText("Error handling:");

                errorHandlingCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                errorHandlingCombo.add("Stop + rollback", SQLScriptErrorHandling.STOP_ROLLBACK.ordinal());
                errorHandlingCombo.add("Stop + commit", SQLScriptErrorHandling.STOP_COMMIT.ordinal());
                errorHandlingCombo.add("Ignore", SQLScriptErrorHandling.IGNORE.ordinal());
            }

            {
                Label fetchLabel = new Label(scriptsGroup, SWT.NONE);
                fetchLabel.setText("Fetch resultsets:");

                fetchResultSets = new Button(scriptsGroup, SWT.CHECK);
            }
        }

        // Content
        {
            Group contentGroup = new Group(composite, SWT.NONE);
            contentGroup.setText("Content");
            contentGroup.setLayout(new GridLayout(2, false));

            Label label = new Label(contentGroup, SWT.NONE);
            label.setText("Maximum text editor content length:");

            maxTextContentSize = new Spinner(contentGroup, SWT.BORDER);
            maxTextContentSize.setSelection(0);
            maxTextContentSize.setDigits(0);
            maxTextContentSize.setIncrement(1000000);
            maxTextContentSize.setMinimum(0);
            maxTextContentSize.setMaximum(Integer.MAX_VALUE);

            label = new Label(contentGroup, SWT.NONE);
            label.setText("Edit LONG columns as LOBs:");
            editLongAsLobCheck = new Button(contentGroup, SWT.CHECK);

            label = new Label(contentGroup, SWT.NONE);
            label.setText("Commit session on value edit apply:");
            commitOnEditApplyCheck = new Button(contentGroup, SWT.CHECK);

            label = new Label(contentGroup, SWT.NONE);
            label.setText("Commit session on content edit apply:");
            commitOnContentApplyCheck = new Button(contentGroup, SWT.CHECK);
        }
        return composite;
    }

    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            resultSetSize.setSelection(store.getInt(PrefConstants.RESULT_SET_MAX_ROWS));
            autoCommitCheck.setSelection(store.getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT));
            executeTimeoutText.setSelection(store.getInt(PrefConstants.STATEMENT_TIMEOUT));

            commitTypeCombo.select(SQLScriptCommitType.valueOf(store.getString(PrefConstants.SCRIPT_COMMIT_TYPE)).ordinal());
            errorHandlingCombo.select(SQLScriptErrorHandling.valueOf(store.getString(PrefConstants.SCRIPT_ERROR_HANDLING)).ordinal());
            commitLinesText.setSelection(store.getInt(PrefConstants.SCRIPT_COMMIT_LINES));
            fetchResultSets.setSelection(store.getBoolean(PrefConstants.SCRIPT_FETCH_RESULT_SETS));

            maxTextContentSize.setSelection(store.getInt(PrefConstants.RS_EDIT_MAX_TEXT_SIZE));
            editLongAsLobCheck.setSelection(store.getBoolean(PrefConstants.RS_EDIT_LONG_AS_LOB));
            commitOnEditApplyCheck.setSelection(store.getBoolean(PrefConstants.RS_COMMIT_ON_EDIT_APPLY));
            commitOnContentApplyCheck.setSelection(store.getBoolean(PrefConstants.RS_COMMIT_ON_CONTENT_APPLY));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(PrefConstants.RESULT_SET_MAX_ROWS, resultSetSize.getSelection());
            store.setValue(PrefConstants.DEFAULT_AUTO_COMMIT, autoCommitCheck.getSelection());
            store.setValue(PrefConstants.STATEMENT_TIMEOUT, executeTimeoutText.getSelection());

            store.setValue(PrefConstants.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.fromOrdinal(commitTypeCombo.getSelectionIndex()).name());
            store.setValue(PrefConstants.SCRIPT_COMMIT_LINES, commitLinesText.getSelection());
            store.setValue(PrefConstants.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.fromOrdinal(errorHandlingCombo.getSelectionIndex()).name());
            store.setValue(PrefConstants.SCRIPT_FETCH_RESULT_SETS, fetchResultSets.getSelection());

            store.setValue(PrefConstants.RS_EDIT_MAX_TEXT_SIZE, maxTextContentSize.getSelection());
            store.setValue(PrefConstants.RS_EDIT_LONG_AS_LOB, editLongAsLobCheck.getSelection());
            store.setValue(PrefConstants.RS_COMMIT_ON_EDIT_APPLY, commitOnEditApplyCheck.getSelection());
            store.setValue(PrefConstants.RS_COMMIT_ON_CONTENT_APPLY, commitOnContentApplyCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        if (store instanceof IPersistentPreferenceStore) {
            try {
                ((IPersistentPreferenceStore)store).save();
            } catch (IOException e) {
                log.warn(e);
            }
        }
    }

    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(PrefConstants.RESULT_SET_MAX_ROWS);
        store.setToDefault(PrefConstants.DEFAULT_AUTO_COMMIT);
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
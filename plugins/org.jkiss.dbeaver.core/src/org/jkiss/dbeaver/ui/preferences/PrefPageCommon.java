/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

/**
 * PrefPageCommon
 */
public class PrefPageCommon extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.common";

    private Button autoCommitCheck;
    private Button keepStatementOpenCheck;
    private Button rollbackOnErrorCheck;
    private Spinner resultSetSize;
    private Spinner memoryContentSize;
    private Button readExpensiveCheck;
    private Button caseSensitiveNamesCheck;

    public PrefPageCommon()
    {
        super();
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
    }

    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(PrefConstants.RESULT_SET_MAX_ROWS) ||
            store.contains(PrefConstants.QUERY_ROLLBACK_ON_ERROR) ||
            store.contains(PrefConstants.DEFAULT_AUTO_COMMIT) ||
            store.contains(PrefConstants.KEEP_STATEMENT_OPEN) ||
            store.contains(PrefConstants.MEMORY_CONTENT_MAX_SIZE) ||
            store.contains(PrefConstants.READ_EXPENSIVE_PROPERTIES) ||
            store.contains(PrefConstants.META_CASE_SENSITIVE)
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
            Group txnGroup = new Group(composite, SWT.NONE);
            txnGroup.setText("Transactions");
            txnGroup.setLayout(new GridLayout(2, false));

            autoCommitCheck = UIUtils.createLabelCheckbox(txnGroup, "Auto-commit by default", false);
            keepStatementOpenCheck = UIUtils.createLabelCheckbox(txnGroup, "Keep open cursors in SQL editor", false);
            rollbackOnErrorCheck = UIUtils.createLabelCheckbox(txnGroup, "Rollback on error", false);
        }

        {
            Group queriesGroup = UIUtils.createControlGroup(composite, "Queries", 2, SWT.NONE, 0);
            UIUtils.createControlLabel(queriesGroup, "ResultSet maximum size");

            resultSetSize = new Spinner(queriesGroup, SWT.BORDER);
            resultSetSize.setSelection(0);
            resultSetSize.setDigits(0);
            resultSetSize.setIncrement(1);
            resultSetSize.setMinimum(1);
            resultSetSize.setMaximum(1024 * 1024);
        }

        {
            Group performanceGroup = UIUtils.createControlGroup(composite, "Performance", 2, SWT.NONE, 0);

            readExpensiveCheck = UIUtils.createLabelCheckbox(performanceGroup, "Show row count for tables", false);

            UIUtils.createControlLabel(performanceGroup, "Maximum LOB length to keep in memory");

            memoryContentSize = new Spinner(performanceGroup, SWT.BORDER);
            memoryContentSize.setSelection(0);
            memoryContentSize.setDigits(0);
            memoryContentSize.setIncrement(1);
            memoryContentSize.setMinimum(0);
            memoryContentSize.setMaximum(1024 * 1024 * 1024);
        }

        {
            Group metadataGroup = UIUtils.createControlGroup(composite, "Metadata", 2, SWT.NONE, 0);

            caseSensitiveNamesCheck = UIUtils.createLabelCheckbox(metadataGroup, "Use case-sensitive names in DDL statements", false);
        }
        return composite;
    }

    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            autoCommitCheck.setSelection(store.getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT));
            keepStatementOpenCheck.setSelection(store.getBoolean(PrefConstants.KEEP_STATEMENT_OPEN));
            rollbackOnErrorCheck.setSelection(store.getBoolean(PrefConstants.QUERY_ROLLBACK_ON_ERROR));
            resultSetSize.setSelection(store.getInt(PrefConstants.RESULT_SET_MAX_ROWS));
            memoryContentSize.setSelection(store.getInt(PrefConstants.MEMORY_CONTENT_MAX_SIZE));
            readExpensiveCheck.setSelection(store.getBoolean(PrefConstants.READ_EXPENSIVE_PROPERTIES));
            caseSensitiveNamesCheck.setSelection(store.getBoolean(PrefConstants.META_CASE_SENSITIVE));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(PrefConstants.DEFAULT_AUTO_COMMIT, autoCommitCheck.getSelection());
            store.setValue(PrefConstants.KEEP_STATEMENT_OPEN, keepStatementOpenCheck.getSelection());
            store.setValue(PrefConstants.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
            store.setValue(PrefConstants.RESULT_SET_MAX_ROWS, resultSetSize.getSelection());
            store.setValue(PrefConstants.MEMORY_CONTENT_MAX_SIZE, memoryContentSize.getSelection());
            store.setValue(PrefConstants.READ_EXPENSIVE_PROPERTIES, readExpensiveCheck.getSelection());
            store.setValue(PrefConstants.META_CASE_SENSITIVE, caseSensitiveNamesCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(PrefConstants.DEFAULT_AUTO_COMMIT);
        store.setToDefault(PrefConstants.KEEP_STATEMENT_OPEN);
        store.setToDefault(PrefConstants.QUERY_ROLLBACK_ON_ERROR);
        store.setToDefault(PrefConstants.RESULT_SET_MAX_ROWS);
        store.setToDefault(PrefConstants.MEMORY_CONTENT_MAX_SIZE);
        store.setToDefault(PrefConstants.READ_EXPENSIVE_PROPERTIES);
        store.setToDefault(PrefConstants.META_CASE_SENSITIVE);
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
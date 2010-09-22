/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * PrefPageSQL
 */
public class PrefPageCommon extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.common";

    private Button autoCommitCheck;
    private Button rollbackOnErrorCheck;
    private Spinner resultSetSize;

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
            store.contains(PrefConstants.DEFAULT_AUTO_COMMIT)
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

        return composite;
    }

    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            autoCommitCheck.setSelection(store.getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT));
            rollbackOnErrorCheck.setSelection(store.getBoolean(PrefConstants.QUERY_ROLLBACK_ON_ERROR));
            resultSetSize.setSelection(store.getInt(PrefConstants.RESULT_SET_MAX_ROWS));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(PrefConstants.DEFAULT_AUTO_COMMIT, autoCommitCheck.getSelection());
            store.setValue(PrefConstants.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
            store.setValue(PrefConstants.RESULT_SET_MAX_ROWS, resultSetSize.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        DBeaverUtils.savePreferenceStore(store);
    }

    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(PrefConstants.DEFAULT_AUTO_COMMIT);
        store.setToDefault(PrefConstants.QUERY_ROLLBACK_ON_ERROR);
        store.setToDefault(PrefConstants.RESULT_SET_MAX_ROWS);
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
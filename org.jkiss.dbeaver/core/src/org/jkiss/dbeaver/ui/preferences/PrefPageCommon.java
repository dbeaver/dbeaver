/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.io.IOException;

/**
 * PrefPageSQL
 */
public class PrefPageCommon extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.common";

    private Button autoCommitCheck;
    private Button rollbackOnErrorCheck;

    public PrefPageCommon()
    {
        super();
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
    }

    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
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
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        // General settings
        {
            Group txnGroup = new Group(composite, SWT.NONE);
            txnGroup.setText("Transactions");
            txnGroup.setLayout(new GridLayout(2, false));

            autoCommitCheck = UIUtils.createLabelCheckbox(txnGroup, "Auto-commit by default", false);
            rollbackOnErrorCheck = UIUtils.createLabelCheckbox(txnGroup, "Rollback on error", false);
        }


        return composite;
    }

    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            autoCommitCheck.setSelection(store.getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT));
            rollbackOnErrorCheck.setSelection(store.getBoolean(PrefConstants.QUERY_ROLLBACK_ON_ERROR));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(PrefConstants.DEFAULT_AUTO_COMMIT, autoCommitCheck.getSelection());
            store.setValue(PrefConstants.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
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
        store.setToDefault(PrefConstants.DEFAULT_AUTO_COMMIT);
        store.setToDefault(PrefConstants.QUERY_ROLLBACK_ON_ERROR);
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
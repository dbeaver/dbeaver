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
import org.jkiss.dbeaver.ui.controls.LocaleSelectorControl;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

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

        LocaleSelectorControl localeSelector = new LocaleSelectorControl(composite, null);
        // Format settings
        {
            Group formatGroup = new Group(composite, SWT.NONE);
            formatGroup.setText("Format");
            formatGroup.setLayout(new GridLayout(2, false));

            {
                UIUtils.createControlLabel(formatGroup, "Locale");
                Combo localeCombo = new Combo(formatGroup, SWT.DROP_DOWN);

                Locale[] locales = Locale.getAvailableLocales();
                Set<String> localeSet = new TreeSet<String>();
                String defLocale = Locale.getDefault().toString();
                for (Locale locale : locales) {
                    String localeString = locale.toString();
                    localeSet.add(localeString);
                }
                for (String locale : localeSet) {
                    localeCombo.add(locale);
                    if (locale.equals(defLocale)) {
                        localeCombo.select(localeCombo.getItemCount() - 1);
                    }
                }
            }

            UIUtils.createLabelText(formatGroup, "Date format", "");
            UIUtils.createLabelText(formatGroup, "Time format", "");
            UIUtils.createLabelText(formatGroup, "Timestamp format", "");
            UIUtils.createLabelText(formatGroup, "Number format", "");
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
        DBeaverUtils.savePreferenceStore(store);
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
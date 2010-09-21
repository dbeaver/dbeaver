/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
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
 * PrefPageDataFormat
 */
public class PrefPageDataFormat extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.dataformat";

    private Font boldFont;

    public PrefPageDataFormat()
    {
        super();
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
    }

    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return false
            //store.contains(PrefConstants.RESULT_SET_MAX_ROWS) ||
            //store.contains(PrefConstants.QUERY_ROLLBACK_ON_ERROR) ||
            //store.contains(PrefConstants.DEFAULT_AUTO_COMMIT)
            ;
    }

    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    protected Control createPreferenceContent(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        // Locale
        LocaleSelectorControl localeSelector = new LocaleSelectorControl(composite, null);

        // formats
        {
            Group formatGroup = new Group(composite, SWT.NONE);
            formatGroup.setText("Format");
            formatGroup.setLayout(new GridLayout(3, false));
            formatGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(formatGroup, "Type");
            UIUtils.createControlLabel(formatGroup, "Format");
            UIUtils.createControlLabel(formatGroup, "Sample");

            for (int i = 0; i < 5; i++) {
                Label typeLable = new Label(formatGroup, SWT.SHADOW_OUT);
                typeLable.setText("Type" + i + ":  ");
                typeLable.setFont(boldFont);

                Text typeFormat = new Text(formatGroup, SWT.BORDER);
                typeFormat.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                Text sampleText = new Text(formatGroup, SWT.BORDER | SWT.READ_ONLY);
                sampleText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }

        }


        return composite;
    }

    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            //autoCommitCheck.setSelection(store.getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT));
            //rollbackOnErrorCheck.setSelection(store.getBoolean(PrefConstants.QUERY_ROLLBACK_ON_ERROR));
            //resultSetSize.setSelection(store.getInt(PrefConstants.RESULT_SET_MAX_ROWS));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    protected void savePreferences(IPreferenceStore store)
    {
        try {
            //store.setValue(PrefConstants.DEFAULT_AUTO_COMMIT, autoCommitCheck.getSelection());
            //store.setValue(PrefConstants.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
            //store.setValue(PrefConstants.RESULT_SET_MAX_ROWS, resultSetSize.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        DBeaverUtils.savePreferenceStore(store);
    }

    protected void clearPreferences(IPreferenceStore store)
    {
        //store.setToDefault(PrefConstants.DEFAULT_AUTO_COMMIT);
        //store.setToDefault(PrefConstants.QUERY_ROLLBACK_ON_ERROR);
        //store.setToDefault(PrefConstants.RESULT_SET_MAX_ROWS);
    }

    public void applyData(Object data)
    {
        super.applyData(data);
    }

    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

    @Override
    public void dispose()
    {
        boldFont.dispose();
        super.dispose();
    }
}
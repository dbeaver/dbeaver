/*
 * Copyright (C) 2010-2012 Serge Rieder serge@jkiss.org
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
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

/**
 * PrefPageResultSet
 */
public class PrefPageResultSet extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset"; //$NON-NLS-1$

    private Spinner resultSetSize;
    private Button binaryShowStrings;
    private Spinner binaryStringMaxLength;

    public PrefPageResultSet()
    {
        super();
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(PrefConstants.RESULT_SET_MAX_ROWS) ||
            store.contains(PrefConstants.RESULT_SET_BINARY_SHOW_STRINGS) ||
            store.contains(PrefConstants.RESULT_SET_BINARY_STRING_MAX_LEN)
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

        {
            Group queriesGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_queries, 2, SWT.NONE, 0);
            UIUtils.createControlLabel(queriesGroup, CoreMessages.pref_page_database_general_label_result_set_max_size);

            resultSetSize = new Spinner(queriesGroup, SWT.BORDER);
            resultSetSize.setSelection(0);
            resultSetSize.setDigits(0);
            resultSetSize.setIncrement(1);
            resultSetSize.setMinimum(1);
            resultSetSize.setMaximum(1024 * 1024);
        }

        {
            Group performanceGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_resultsets_group_binary, 2, SWT.NONE, 0);

            binaryShowStrings = UIUtils.createLabelCheckbox(performanceGroup, CoreMessages.pref_page_database_resultsets_label_binary_use_strings, false);

            UIUtils.createControlLabel(performanceGroup, CoreMessages.pref_page_database_resultsets_label_binary_strings_max_length);

            binaryStringMaxLength = new Spinner(performanceGroup, SWT.BORDER);
            binaryStringMaxLength.setSelection(0);
            binaryStringMaxLength.setDigits(0);
            binaryStringMaxLength.setIncrement(1);
            binaryStringMaxLength.setMinimum(0);
            binaryStringMaxLength.setMaximum(10000);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            resultSetSize.setSelection(store.getInt(PrefConstants.RESULT_SET_MAX_ROWS));
            binaryShowStrings.setSelection(store.getBoolean(PrefConstants.RESULT_SET_BINARY_SHOW_STRINGS));
            binaryStringMaxLength.setSelection(store.getInt(PrefConstants.RESULT_SET_BINARY_STRING_MAX_LEN));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(PrefConstants.RESULT_SET_MAX_ROWS, resultSetSize.getSelection());
            store.setValue(PrefConstants.RESULT_SET_BINARY_SHOW_STRINGS, binaryShowStrings.getSelection());
            store.setValue(PrefConstants.RESULT_SET_BINARY_STRING_MAX_LEN, binaryStringMaxLength.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(PrefConstants.RESULT_SET_MAX_ROWS);
        store.setToDefault(PrefConstants.RESULT_SET_BINARY_SHOW_STRINGS);
        store.setToDefault(PrefConstants.RESULT_SET_BINARY_STRING_MAX_LEN);
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
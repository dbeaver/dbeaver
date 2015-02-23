/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

/**
 * PrefPageMetaData
 */
public class PrefPageMetaData extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.meta"; //$NON-NLS-1$

    private Button readExpensiveCheck;
    private Button separateMetaConnectionCheck;
    private Button caseSensitiveNamesCheck;

    public PrefPageMetaData()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES) ||
            store.contains(DBeaverPreferences.META_SEPARATE_CONNECTION) ||
            store.contains(DBeaverPreferences.META_CASE_SENSITIVE)
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
            Group metadataGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_metadata, 2, SWT.NONE, 0);

            separateMetaConnectionCheck = UIUtils.createLabelCheckbox(metadataGroup, CoreMessages.pref_page_database_general_separate_meta_connection, false);
            caseSensitiveNamesCheck = UIUtils.createLabelCheckbox(metadataGroup, CoreMessages.pref_page_database_general_checkbox_case_sensitive_names, false);
            readExpensiveCheck = UIUtils.createLabelCheckbox(metadataGroup, CoreMessages.pref_page_database_general_checkbox_show_row_count, false);
        }


        return composite;
    }

    @Override
    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            readExpensiveCheck.setSelection(store.getBoolean(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES));
            separateMetaConnectionCheck.setSelection(store.getBoolean(DBeaverPreferences.META_SEPARATE_CONNECTION));
            caseSensitiveNamesCheck.setSelection(store.getBoolean(DBeaverPreferences.META_CASE_SENSITIVE));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES, readExpensiveCheck.getSelection());
            store.setValue(DBeaverPreferences.META_SEPARATE_CONNECTION, separateMetaConnectionCheck.getSelection());
            store.setValue(DBeaverPreferences.META_CASE_SENSITIVE, caseSensitiveNamesCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES);
        store.setToDefault(DBeaverPreferences.META_SEPARATE_CONNECTION);
        store.setToDefault(DBeaverPreferences.META_CASE_SENSITIVE);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.views;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

/**
 * DB2 Preference Page
 * 
 * @author Denis Forveille
 */
public class DB2PreferencePage extends TargetPrefPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.db2.general"; //$NON-NLS-1$

    private Text explainTableSchemaNameText;

    public DB2PreferencePage()
    {
        super();
        setPreferenceStore(DBeaverCore.getGlobalPreferenceStore());
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return store.contains(DB2Constants.PREF_EXPLAIN_TABLE_SCHEMA_NAME);
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

        Group planGroup = UIUtils.createControlGroup(composite, "Explain", 2, GridData.FILL_HORIZONTAL, 0);
        explainTableSchemaNameText = UIUtils.createLabelText(planGroup, "Explain Table Schema", "");

        return composite;
    }

    @Override
    protected void loadPreferences(IPreferenceStore store)
    {
        explainTableSchemaNameText.setText(store.getString(DB2Constants.PREF_EXPLAIN_TABLE_SCHEMA_NAME));
    }

    @Override
    protected void savePreferences(IPreferenceStore store)
    {
        store.setValue(DB2Constants.PREF_EXPLAIN_TABLE_SCHEMA_NAME, explainTableSchemaNameText.getText());
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(DB2Constants.PREF_EXPLAIN_TABLE_SCHEMA_NAME);
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
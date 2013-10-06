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
package org.jkiss.dbeaver.ext.oracle.views;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

/**
 * PrefPageOracle
 */
public class PrefPageOracle extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.oracle.general"; //$NON-NLS-1$

    private Text explainTableText;

    public PrefPageOracle()
    {
        super();
        setPreferenceStore(DBeaverCore.getGlobalPreferenceStore());
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(OracleConstants.PREF_EXPLAIN_TABLE_NAME)
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
            Group planGroup = UIUtils.createControlGroup(composite, "Execution plan", 2, GridData.FILL_HORIZONTAL, 0);
            explainTableText = UIUtils.createLabelText(planGroup, "Plan table", "");
        }

        return composite;
    }

    @Override
    protected void loadPreferences(IPreferenceStore store)
    {
        explainTableText.setText(store.getString(OracleConstants.PREF_EXPLAIN_TABLE_NAME));
    }

    @Override
    protected void savePreferences(IPreferenceStore store)
    {
        store.setValue(OracleConstants.PREF_EXPLAIN_TABLE_NAME, explainTableText.getText());
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(OracleConstants.PREF_EXPLAIN_TABLE_NAME);
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
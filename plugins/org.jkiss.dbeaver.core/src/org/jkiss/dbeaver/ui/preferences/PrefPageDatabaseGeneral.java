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
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * PrefPageDatabaseGeneral
 */
public class PrefPageDatabaseGeneral extends PreferencePage implements IWorkbenchPreferencePage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.common"; //$NON-NLS-1$

    private Button longOperationsCheck;
    private Spinner longOperationsTimeout;

    private Button expandOnConnectCheck;
    private Button sortCaseInsensitiveCheck;
    private Button groupByDriverCheck;

    private Button caseSensitiveNamesCheck;

    public PrefPageDatabaseGeneral()
    {
        super();
        setPreferenceStore(DBeaverCore.getGlobalPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        // Agent settings
        {
            Group agentGroup = UIUtils.createControlGroup(composite, "Taskbar", 2, SWT.NONE, 0);

            longOperationsCheck = UIUtils.createLabelCheckbox(agentGroup, "Enable long-time operations notification", false);
            longOperationsTimeout = UIUtils.createLabelSpinner(agentGroup, "Long-time operation timeout", 0, 0, Integer.MAX_VALUE);
        }

        {
            Group navigatorGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_navigator, 2, SWT.NONE, 0);

            expandOnConnectCheck = UIUtils.createLabelCheckbox(navigatorGroup, "Expand navigator tree on connect", false);
            sortCaseInsensitiveCheck = UIUtils.createLabelCheckbox(navigatorGroup, "Order elements alphabetically", false);
            groupByDriverCheck = UIUtils.createLabelCheckbox(navigatorGroup, "Group databases by driver", false);
        }

        {
            Group metadataGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_metadata, 2, SWT.NONE, 0);

            caseSensitiveNamesCheck = UIUtils.createLabelCheckbox(metadataGroup, CoreMessages.pref_page_database_general_checkbox_case_sensitive_names, false);
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        longOperationsCheck.setSelection(store.getBoolean(PrefConstants.AGENT_LONG_OPERATION_NOTIFY));
        longOperationsTimeout.setSelection(store.getInt(PrefConstants.AGENT_LONG_OPERATION_TIMEOUT));
        expandOnConnectCheck.setSelection(store.getBoolean(PrefConstants.NAVIGATOR_EXPAND_ON_CONNECT));
        sortCaseInsensitiveCheck.setSelection(store.getBoolean(PrefConstants.NAVIGATOR_SORT_ALPHABETICALLY));
        groupByDriverCheck.setSelection(store.getBoolean(PrefConstants.NAVIGATOR_GROUP_BY_DRIVER));
        caseSensitiveNamesCheck.setSelection(store.getBoolean(PrefConstants.META_CASE_SENSITIVE));
    }

    @Override
    public boolean performOk()
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        //store.setValue(PrefConstants.AGENT_ENABLED, agentEnabledCheck.getSelection());
        store.setValue(PrefConstants.AGENT_LONG_OPERATION_NOTIFY, longOperationsCheck.getSelection());
        store.setValue(PrefConstants.AGENT_LONG_OPERATION_TIMEOUT, longOperationsTimeout.getSelection());
        store.setValue(PrefConstants.NAVIGATOR_EXPAND_ON_CONNECT, expandOnConnectCheck.getSelection());
        store.setValue(PrefConstants.NAVIGATOR_SORT_ALPHABETICALLY, sortCaseInsensitiveCheck.getSelection());
        store.setValue(PrefConstants.NAVIGATOR_GROUP_BY_DRIVER, groupByDriverCheck.getSelection());
        store.setValue(PrefConstants.META_CASE_SENSITIVE, caseSensitiveNamesCheck.getSelection());
        RuntimeUtils.savePreferenceStore(store);

        return true;
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

}
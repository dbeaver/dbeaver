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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * PrefPageDatabaseGeneral
 */
public class PrefPageDatabaseGeneral extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.common"; //$NON-NLS-1$

    private Button automaticUpdateCheck;

    private Button longOperationsCheck;
    private Spinner longOperationsTimeout;

    private Button expandOnConnectCheck;
    private Button sortCaseInsensitiveCheck;
    private Button groupByDriverCheck;
    private Button editorFullName;

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

        {
            Group groupObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_general, 1, GridData.VERTICAL_ALIGN_BEGINNING, 300);
            automaticUpdateCheck = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_ui_general_checkbox_automatic_updates, false);
        }

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
            editorFullName = UIUtils.createLabelCheckbox(navigatorGroup, "Use full object names in editors", false);
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        automaticUpdateCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK));
        longOperationsCheck.setSelection(store.getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY));
        longOperationsTimeout.setSelection(store.getInt(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT));
        expandOnConnectCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT));
        sortCaseInsensitiveCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY));
        groupByDriverCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_GROUP_BY_DRIVER));
        editorFullName.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME));
    }

    @Override
    public boolean performOk()
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        store.setValue(DBeaverPreferences.UI_AUTO_UPDATE_CHECK, automaticUpdateCheck.getSelection());
        //store.setValue(DBeaverPreferences.AGENT_ENABLED, agentEnabledCheck.getSelection());
        store.setValue(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY, longOperationsCheck.getSelection());
        store.setValue(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT, longOperationsTimeout.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT, expandOnConnectCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY, sortCaseInsensitiveCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_GROUP_BY_DRIVER, groupByDriverCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, editorFullName.getSelection());
        RuntimeUtils.savePreferenceStore(store);

        return true;
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {
    }

}
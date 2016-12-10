/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageDatabaseNavigator
 */
public class PrefPageDatabaseNavigator extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.navigator"; //$NON-NLS-1$

    private Button expandOnConnectCheck;
    private Button sortCaseInsensitiveCheck;
    private Button sortFoldersFirstCheck;
    private Button groupByDriverCheck;
    private Button editorFullName;
    private Combo doubleClickBehavior;

    public PrefPageDatabaseNavigator()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBeaverCore.getGlobalPreferenceStore()));
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
            Group navigatorGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_navigator, 2, SWT.NONE, 0);

            expandOnConnectCheck = UIUtils.createCheckbox(navigatorGroup, "Expand navigator tree on connect", false);
            expandOnConnectCheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));

            sortCaseInsensitiveCheck = UIUtils.createCheckbox(navigatorGroup, "Order elements alphabetically", false);
            sortCaseInsensitiveCheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));

            sortFoldersFirstCheck = UIUtils.createCheckbox(navigatorGroup, "Folders first", "Show folders before regular elements", false, 1);
            sortFoldersFirstCheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));

            groupByDriverCheck = UIUtils.createCheckbox(navigatorGroup, "Group databases by driver", false);
            groupByDriverCheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));
            groupByDriverCheck.setEnabled(false);

            editorFullName = UIUtils.createCheckbox(navigatorGroup, "Show full object names in editors", false);
            editorFullName.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));

            doubleClickBehavior = UIUtils.createLabelCombo(navigatorGroup, "Double-click on connection", SWT.DROP_DOWN | SWT.READ_ONLY);
            doubleClickBehavior.add("Open Properties", NavigatorViewBase.DoubleClickBehavior.EDIT.ordinal());
            doubleClickBehavior.add("Connect / Disconnect", NavigatorViewBase.DoubleClickBehavior.CONNECT.ordinal());
            doubleClickBehavior.add("Open SQL Editor", NavigatorViewBase.DoubleClickBehavior.SQL_EDITOR.ordinal());
            doubleClickBehavior.add("Expand / Collapse", NavigatorViewBase.DoubleClickBehavior.EXPAND.ordinal());
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        expandOnConnectCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT));
        sortCaseInsensitiveCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY));
        sortFoldersFirstCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SORT_FOLDERS_FIRST));
        groupByDriverCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_GROUP_BY_DRIVER));
        editorFullName.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME));
        doubleClickBehavior.select(
            NavigatorViewBase.DoubleClickBehavior.valueOf(store.getString(DBeaverPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK)).ordinal());
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        store.setValue(DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT, expandOnConnectCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY, sortCaseInsensitiveCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, sortFoldersFirstCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_GROUP_BY_DRIVER, groupByDriverCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, editorFullName.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK,
            CommonUtils.fromOrdinal(NavigatorViewBase.DoubleClickBehavior.class, doubleClickBehavior.getSelectionIndex()).name());

        PrefUtils.savePreferenceStore(store);

        return true;
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

    @Nullable
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
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
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
    private Button showObjectTipsCheck;
    private Button sortCaseInsensitiveCheck;
    private Button sortFoldersFirstCheck;
    private Button groupByDriverCheck;
    private Button syncEditorDataSourceWithNavigator;
    private Combo dsDoubleClickBehavior;
    private Combo objDoubleClickBehavior;

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

            expandOnConnectCheck = UIUtils.createCheckbox(navigatorGroup, CoreMessages.pref_page_database_general_label_expand_navigator_tree, "", false, 2);
            showObjectTipsCheck = UIUtils.createCheckbox(navigatorGroup, CoreMessages.pref_page_database_general_label_show_tips_in_tree, CoreMessages.pref_page_database_general_label_show_tips_in_tree_tip, false, 2);
            sortCaseInsensitiveCheck = UIUtils.createCheckbox(navigatorGroup, CoreMessages.pref_page_database_general_label_order_elements_alphabetically, "", false, 2);

            sortFoldersFirstCheck = UIUtils.createCheckbox(navigatorGroup, CoreMessages.pref_page_database_general_label_folders_first, CoreMessages.pref_page_database_general_label_folders_first_tip, false, 2);

            groupByDriverCheck = UIUtils.createCheckbox(navigatorGroup, CoreMessages.pref_page_database_general_label_group_database_by_driver, "", false, 2);
            groupByDriverCheck.setEnabled(false);

            syncEditorDataSourceWithNavigator = UIUtils.createCheckbox(navigatorGroup, CoreMessages.pref_page_database_general_label_sync_editor_connection_with_navigator, CoreMessages.pref_page_database_general_label_sync_editor_connection_with_navigator_tip, false, 2);

            objDoubleClickBehavior = UIUtils.createLabelCombo(navigatorGroup, CoreMessages.pref_page_database_general_label_double_click_node, SWT.DROP_DOWN | SWT.READ_ONLY);
            objDoubleClickBehavior.add(CoreMessages.pref_page_database_general_label_double_click_node_open_properties, 0);
            objDoubleClickBehavior.add(CoreMessages.pref_page_database_general_label_double_click_node_expand_collapse, 1);

            dsDoubleClickBehavior = UIUtils.createLabelCombo(navigatorGroup, CoreMessages.pref_page_database_general_label_double_click_connection, SWT.DROP_DOWN | SWT.READ_ONLY);
            dsDoubleClickBehavior.add(CoreMessages.pref_page_database_general_label_double_click_connection_open_properties, NavigatorViewBase.DoubleClickBehavior.EDIT.ordinal());
            dsDoubleClickBehavior.add(CoreMessages.pref_page_database_general_label_double_click_connection_conn_disconn, NavigatorViewBase.DoubleClickBehavior.CONNECT.ordinal());
            dsDoubleClickBehavior.add(CoreMessages.pref_page_database_general_label_double_click_connection_open_sqleditor, NavigatorViewBase.DoubleClickBehavior.SQL_EDITOR.ordinal());
            dsDoubleClickBehavior.add(CoreMessages.pref_page_database_general_label_double_click_connection_expand_collapse, NavigatorViewBase.DoubleClickBehavior.EXPAND.ordinal());

        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        expandOnConnectCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT));
        showObjectTipsCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SHOW_OBJECT_TIPS));
        sortCaseInsensitiveCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY));
        sortFoldersFirstCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SORT_FOLDERS_FIRST));
        groupByDriverCheck.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_GROUP_BY_DRIVER));
        syncEditorDataSourceWithNavigator.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE));
        NavigatorViewBase.DoubleClickBehavior objDCB = NavigatorViewBase.DoubleClickBehavior.valueOf(store.getString(DBeaverPreferences.NAVIGATOR_OBJECT_DOUBLE_CLICK));
        objDoubleClickBehavior.select(objDCB == NavigatorViewBase.DoubleClickBehavior.EXPAND ? 1 : 0);
        dsDoubleClickBehavior.select(
            NavigatorViewBase.DoubleClickBehavior.valueOf(store.getString(DBeaverPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK)).ordinal());
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        store.setValue(DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT, expandOnConnectCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_SHOW_OBJECT_TIPS, showObjectTipsCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY, sortCaseInsensitiveCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, sortFoldersFirstCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_GROUP_BY_DRIVER, groupByDriverCheck.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE, syncEditorDataSourceWithNavigator.getSelection());
        NavigatorViewBase.DoubleClickBehavior objDCB = NavigatorViewBase.DoubleClickBehavior.EXPAND;
        if (objDoubleClickBehavior.getSelectionIndex() == 0) {
            objDCB = NavigatorViewBase.DoubleClickBehavior.EDIT;
        }
        store.setValue(DBeaverPreferences.NAVIGATOR_OBJECT_DOUBLE_CLICK, objDCB.name());
        store.setValue(DBeaverPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK,
            CommonUtils.fromOrdinal(NavigatorViewBase.DoubleClickBehavior.class, dsDoubleClickBehavior.getSelectionIndex()).name());

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
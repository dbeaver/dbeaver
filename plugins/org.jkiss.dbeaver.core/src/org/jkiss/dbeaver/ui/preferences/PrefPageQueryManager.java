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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.QMConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PrefPageQueryManager
 */
public class PrefPageQueryManager extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.qm"; //$NON-NLS-1$
    private Button checkObjectTypeSessions;
    private Button checkObjectTypeTxn;
    private Button checkObjectTypeScripts;
    private Button checkObjectTypeQueries;
    private Button checkQueryTypeUser;
    private Button checkQueryTypeScript;
    private Button checkQueryTypeUtil;
    private Button checkQueryTypeMeta;
    private Button checkQueryTypeDDL;
    private Button checkQueryTypeOther;
    private Text textHistoryDays;
    private Text textEntriesPerPage;


    @Override
    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Composite filterSettings = UIUtils.createPlaceholder(composite, 2, 5);

        Group groupObjects = UIUtils.createControlGroup(filterSettings, CoreMessages.pref_page_query_manager_group_object_types, 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 170);
        checkObjectTypeSessions = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_query_manager_checkbox_sessions, false);
        checkObjectTypeTxn = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_query_manager_checkbox_transactions, false);
        checkObjectTypeScripts = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_query_manager_checkbox_scripts, false);
        checkObjectTypeQueries = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_query_manager_checkbox_queries, false);

        Group groupQueryTypes = UIUtils.createControlGroup(filterSettings, CoreMessages.pref_page_query_manager_group_query_types, 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 170);
        checkQueryTypeUser = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_user_queries, false);
        checkQueryTypeScript = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_user_scripts, false);
        checkQueryTypeUtil = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_utility_functions, false);
        checkQueryTypeMeta = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_metadata_read, false);
        checkQueryTypeDDL = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_ddl_executions, false);
        checkQueryTypeOther = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_other, false);

        Group settingsTypes = UIUtils.createControlGroup(composite, CoreMessages.pref_page_query_manager_group_settings, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
        textHistoryDays = UIUtils.createLabelText(settingsTypes, CoreMessages.pref_page_query_manager_label_days_to_store_log, "", SWT.BORDER, new GridData(50, SWT.DEFAULT)); //$NON-NLS-2$
        textEntriesPerPage = UIUtils.createLabelText(settingsTypes, CoreMessages.pref_page_query_manager_label_entries_per_page, "", SWT.BORDER, new GridData(50, SWT.DEFAULT)); //$NON-NLS-2$

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();
        List<String> objectTypes = CommonUtils.splitString(store.getString(QMConstants.PROP_OBJECT_TYPES), ',');
        List<String> queryTypes = CommonUtils.splitString(store.getString(QMConstants.PROP_QUERY_TYPES), ',');

        checkObjectTypeSessions.setSelection(objectTypes.contains(QMConstants.OBJECT_TYPE_SESSION));
        checkObjectTypeTxn.setSelection(objectTypes.contains(QMConstants.OBJECT_TYPE_TRANSACTION));
        checkObjectTypeScripts.setSelection(objectTypes.contains(QMConstants.OBJECT_TYPE_SCRIPT));
        checkObjectTypeQueries.setSelection(objectTypes.contains(QMConstants.OBJECT_TYPE_QUERY));

        checkQueryTypeUser.setSelection(queryTypes.contains(DBCExecutionPurpose.USER.name()));
        checkQueryTypeScript.setSelection(queryTypes.contains(DBCExecutionPurpose.USER_SCRIPT.name()));
        checkQueryTypeUtil.setSelection(queryTypes.contains(DBCExecutionPurpose.UTIL.name()));
        checkQueryTypeMeta.setSelection(queryTypes.contains(DBCExecutionPurpose.META.name()));
        checkQueryTypeDDL.setSelection(queryTypes.contains(DBCExecutionPurpose.DDL.name()));
        checkQueryTypeOther.setSelection(queryTypes.contains(DBCExecutionPurpose.OTHER.name()));

        textHistoryDays.setText(store.getString(QMConstants.PROP_HISTORY_DAYS));
        textEntriesPerPage.setText(store.getString(QMConstants.PROP_ENTRIES_PER_PAGE));

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        List<String> objectTypes = new ArrayList<String>();
        List<String> queryTypes = new ArrayList<String>();
        if (checkObjectTypeSessions.getSelection()) objectTypes.add(QMConstants.OBJECT_TYPE_SESSION);
        if (checkObjectTypeTxn.getSelection()) objectTypes.add(QMConstants.OBJECT_TYPE_TRANSACTION);
        if (checkObjectTypeScripts.getSelection()) objectTypes.add(QMConstants.OBJECT_TYPE_SCRIPT);
        if (checkObjectTypeQueries.getSelection()) objectTypes.add(QMConstants.OBJECT_TYPE_QUERY);

        if (checkQueryTypeUser.getSelection()) queryTypes.add(DBCExecutionPurpose.USER.name());
        if (checkQueryTypeScript.getSelection()) queryTypes.add(DBCExecutionPurpose.USER_SCRIPT.name());
        if (checkQueryTypeUtil.getSelection()) queryTypes.add(DBCExecutionPurpose.UTIL.name());
        if (checkQueryTypeMeta.getSelection()) queryTypes.add(DBCExecutionPurpose.META.name());
        if (checkQueryTypeDDL.getSelection()) queryTypes.add(DBCExecutionPurpose.DDL.name());
        if (checkQueryTypeOther.getSelection()) queryTypes.add(DBCExecutionPurpose.OTHER.name());

        Integer historyDays = UIUtils.getTextInteger(textHistoryDays);
        Integer entriesPerPage = UIUtils.getTextInteger(textEntriesPerPage);

        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();
        store.setValue(QMConstants.PROP_OBJECT_TYPES, CommonUtils.makeString(objectTypes, ','));
        store.setValue(QMConstants.PROP_QUERY_TYPES, CommonUtils.makeString(queryTypes, ','));
        if (historyDays != null) {
            store.setValue(QMConstants.PROP_HISTORY_DAYS, historyDays);
        }
        if (entriesPerPage != null) {
            store.setValue(QMConstants.PROP_ENTRIES_PER_PAGE, entriesPerPage);
        }
        RuntimeUtils.savePreferenceStore(store);

        return super.performOk();
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
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.jkiss.dbeaver.runtime.qm.QMObjectType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PrefPageQueryManager
 */
public class PrefPageQueryManager extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.qm"; //$NON-NLS-1$
    private Button checkObjectTypeSessions;
    private Button checkObjectTypeTxn;
    private Button checkObjectTypeQueries;
    private Button checkQueryTypeUser;
    private Button checkQueryTypeScript;
    private Button checkQueryTypeUtil;
    private Button checkQueryTypeMeta;
    private Text textHistoryDays;
    private Text textEntriesPerPage;
    private Button checkStoreLog;
    private Text textOutputFolder;


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
        //checkObjectTypeScripts = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_query_manager_checkbox_scripts, false);
        checkObjectTypeQueries = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_query_manager_checkbox_queries, false);

        Group groupQueryTypes = UIUtils.createControlGroup(filterSettings, CoreMessages.pref_page_query_manager_group_query_types, 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 170);
        checkQueryTypeUser = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_user_queries, false);
        checkQueryTypeScript = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_user_scripts, false);
        checkQueryTypeUtil = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_utility_functions, false);
        checkQueryTypeMeta = UIUtils.createCheckbox(groupQueryTypes, CoreMessages.pref_page_query_manager_checkbox_metadata_read, false);

        {
            Group viewSettings = UIUtils.createControlGroup(composite, CoreMessages.pref_page_query_manager_group_settings, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            textEntriesPerPage = UIUtils.createLabelText(viewSettings, CoreMessages.pref_page_query_manager_label_entries_per_page, "", SWT.BORDER, new GridData(50, SWT.DEFAULT)); //$NON-NLS-2$
        }

        {
            Group storageSettings = UIUtils.createControlGroup(composite, CoreMessages.pref_page_query_manager_group_storage, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            checkStoreLog = UIUtils.createCheckbox(storageSettings, CoreMessages.pref_page_query_manager_checkbox_store_log_file, false);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            checkStoreLog.setLayoutData(gd);
            checkStoreLog.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    UIUtils.enableWithChildren(textOutputFolder.getParent(), checkStoreLog.getSelection());
                }
            });
            textOutputFolder = UIUtils.createOutputFolderChooser(storageSettings, CoreMessages.pref_page_query_manager_logs_folder, null);
            textHistoryDays = UIUtils.createLabelText(storageSettings, CoreMessages.pref_page_query_manager_label_days_to_store_log, "", SWT.BORDER, new GridData(50, SWT.DEFAULT)); //$NON-NLS-2$
            textHistoryDays.setEnabled(false);
        }
        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        Collection<QMObjectType> objectTypes = QMObjectType.fromString(store.getString(QMConstants.PROP_OBJECT_TYPES));
        Collection<String> queryTypes = CommonUtils.splitString(store.getString(QMConstants.PROP_QUERY_TYPES), ',');

        checkObjectTypeSessions.setSelection(objectTypes.contains(QMObjectType.session));
        checkObjectTypeTxn.setSelection(objectTypes.contains(QMObjectType.txn));
        checkObjectTypeQueries.setSelection(objectTypes.contains(QMObjectType.query));

        checkQueryTypeUser.setSelection(queryTypes.contains(DBCExecutionPurpose.USER.name()));
        checkQueryTypeScript.setSelection(queryTypes.contains(DBCExecutionPurpose.USER_SCRIPT.name()));
        checkQueryTypeUtil.setSelection(queryTypes.contains(DBCExecutionPurpose.UTIL.name()));
        checkQueryTypeMeta.setSelection(queryTypes.contains(DBCExecutionPurpose.META.name()));

        textHistoryDays.setText(store.getString(QMConstants.PROP_HISTORY_DAYS));
        textEntriesPerPage.setText(store.getString(QMConstants.PROP_ENTRIES_PER_PAGE));

        checkStoreLog.setSelection(store.getBoolean(QMConstants.PROP_STORE_LOG_FILE));
        textOutputFolder.setText(store.getString(QMConstants.PROP_LOG_DIRECTORY));
        UIUtils.enableWithChildren(textOutputFolder.getParent(), checkStoreLog.getSelection());

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        List<QMObjectType> objectTypes = new ArrayList<QMObjectType>();
        List<String> queryTypes = new ArrayList<String>();
        if (checkObjectTypeSessions.getSelection()) objectTypes.add(QMObjectType.session);
        if (checkObjectTypeTxn.getSelection()) objectTypes.add(QMObjectType.txn);
        if (checkObjectTypeQueries.getSelection()) objectTypes.add(QMObjectType.query);

        if (checkQueryTypeUser.getSelection()) queryTypes.add(DBCExecutionPurpose.USER.name());
        if (checkQueryTypeScript.getSelection()) queryTypes.add(DBCExecutionPurpose.USER_SCRIPT.name());
        if (checkQueryTypeUtil.getSelection()) queryTypes.add(DBCExecutionPurpose.UTIL.name());
        if (checkQueryTypeMeta.getSelection()) queryTypes.add(DBCExecutionPurpose.META.name());

        Integer historyDays = UIUtils.getTextInteger(textHistoryDays);
        Integer entriesPerPage = UIUtils.getTextInteger(textEntriesPerPage);

        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.setValue(QMConstants.PROP_OBJECT_TYPES, QMObjectType.toString(objectTypes));
        store.setValue(QMConstants.PROP_QUERY_TYPES, CommonUtils.makeString(queryTypes, ','));
        if (historyDays != null) {
            store.setValue(QMConstants.PROP_HISTORY_DAYS, historyDays);
        }
        if (entriesPerPage != null) {
            store.setValue(QMConstants.PROP_ENTRIES_PER_PAGE, entriesPerPage);
        }
        store.setValue(QMConstants.PROP_STORE_LOG_FILE, checkStoreLog.getSelection());
        store.setValue(QMConstants.PROP_LOG_DIRECTORY, textOutputFolder.getText());
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
/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.QMConstants;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PrefPageQueryManager
 */
public class PrefPageQueryManager extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.qm";
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


    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Composite filterSettings = UIUtils.createPlaceholder(composite, 2, 5);

        Group groupObjects = UIUtils.createControlGroup(filterSettings, "Object Types", 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 150);
        checkObjectTypeSessions = UIUtils.createCheckbox(groupObjects, "Sessions", false);
        checkObjectTypeTxn = UIUtils.createCheckbox(groupObjects, "Transactions", false);
        checkObjectTypeScripts = UIUtils.createCheckbox(groupObjects, "Scripts", false);
        checkObjectTypeQueries = UIUtils.createCheckbox(groupObjects, "Queries", false);

        Group groupQueryTypes = UIUtils.createControlGroup(filterSettings, "Query Types", 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 150);
        checkQueryTypeUser = UIUtils.createCheckbox(groupQueryTypes, "User queries", false);
        checkQueryTypeScript = UIUtils.createCheckbox(groupQueryTypes, "User scripts", false);
        checkQueryTypeUtil = UIUtils.createCheckbox(groupQueryTypes, "Utility functions", false);
        checkQueryTypeMeta = UIUtils.createCheckbox(groupQueryTypes, "Metadata read", false);
        checkQueryTypeDDL = UIUtils.createCheckbox(groupQueryTypes, "DDL executions", false);
        checkQueryTypeOther = UIUtils.createCheckbox(groupQueryTypes, "Other", false);

        Group settingsTypes = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
        textHistoryDays = UIUtils.createLabelText(settingsTypes, "Days to store log", "", SWT.BORDER, new GridData(50, SWT.DEFAULT));
        textEntriesPerPage = UIUtils.createLabelText(settingsTypes, "Entries per page", "", SWT.BORDER, new GridData(50, SWT.DEFAULT));

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

    public IAdaptable getElement()
    {
        return null;
    }

    public void setElement(IAdaptable element)
    {

    }
}
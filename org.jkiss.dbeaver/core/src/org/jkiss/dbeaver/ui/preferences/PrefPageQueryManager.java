/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * PrefPageQueryManager
 */
public class PrefPageQueryManager extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.qm";


    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Composite filterSettings = UIUtils.createPlaceholder(composite, 2, 5);

        Group groupObjects = UIUtils.createControlGroup(filterSettings, "Object Types", 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 150);
        UIUtils.createLabelCheckbox(groupObjects, "Sessions", false);
        UIUtils.createLabelCheckbox(groupObjects, "Transactions", false);
        UIUtils.createLabelCheckbox(groupObjects, "Scripts", false);
        UIUtils.createLabelCheckbox(groupObjects, "Queries", true);

        Group groupQueryTypes = UIUtils.createControlGroup(filterSettings, "Query Types", 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 150);
        UIUtils.createLabelCheckbox(groupQueryTypes, "User queries", true);
        UIUtils.createLabelCheckbox(groupQueryTypes, "User scripts", true);
        UIUtils.createLabelCheckbox(groupQueryTypes, "Utility functions", false);
        UIUtils.createLabelCheckbox(groupQueryTypes, "Metadata read", false);
        UIUtils.createLabelCheckbox(groupQueryTypes, "DDL executions", false);
        UIUtils.createLabelCheckbox(groupQueryTypes, "Other", false);

        Group settingsTypes = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
        UIUtils.createLabelText(settingsTypes, "Days to store log", "30", SWT.BORDER, 0);

        return composite;
    }

    public IAdaptable getElement()
    {
        return null;
    }

    public void setElement(IAdaptable element)
    {

    }
}
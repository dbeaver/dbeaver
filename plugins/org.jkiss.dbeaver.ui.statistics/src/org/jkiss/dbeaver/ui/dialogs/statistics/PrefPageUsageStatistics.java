/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.statistics;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

/**
 * PrefPageQueryManager
 */
public class PrefPageUsageStatistics extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.usageStatistics"; //$NON-NLS-1$

    private Button checkSendUsageStatistics;
    private Button checkShowStatisticsDetails;

    @Override
    public void init(IWorkbench workbench) {

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Composite group = UIUtils.createControlGroup(composite, "Data sharing", 1, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        checkSendUsageStatistics = UIUtils.createCheckbox(group, "Send usage statistics", false);
        createDataShareComposite(group);

        UIUtils.createEmptyLabel(group, 1, 1);
        checkShowStatisticsDetails = UIUtils.createCheckbox(group, "Show statistics details before sending", false);
        UIUtils.createLabel(group, "Show the exact information we are going to send.\n");

        performDefaults();

        return composite;
    }

    public static void createDataShareComposite(Composite group) {
        UIUtils.createLink(group,
            "Help DBeaver to improve by sending anonymous data about features used,\n" +
                "hardware and software configuration.\n" +
                "\n" +
                "Please note that this will not include personal data or any sensitive information,\n" +
                "such as database connection configurations, executed queries, database information, etc.\n" +
                "The data sent complies with <a>DBeaver Corporation Privacy Policy</a>.",
            SelectionListener.widgetSelectedAdapter(selectionEvent ->
                ShellUtils.launchProgram("https://dbeaver.com/privacy/")));
    }

    @Override
    protected void performDefaults() {
        checkSendUsageStatistics.setSelection(DBRFeatureRegistry.isTrackingEnabled());
        checkShowStatisticsDetails.setSelection(DBRFeatureRegistry.isDetailsPreviewEnabled());

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        DBRFeatureRegistry.setTrackingEnabled(checkSendUsageStatistics.getSelection());
        DBRFeatureRegistry.setDetailsPreviewEnabled(checkShowStatisticsDetails.getSelection());

        return super.performOk();
    }

    @Override
    public IAdaptable getElement() {
        return null;
    }

    @Override
    public void setElement(IAdaptable element) {

    }
}
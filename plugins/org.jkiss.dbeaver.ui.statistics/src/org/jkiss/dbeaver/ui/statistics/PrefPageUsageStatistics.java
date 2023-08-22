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
package org.jkiss.dbeaver.ui.statistics;

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
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

/**
 * PrefPageUsageStatistics
 */
public class PrefPageUsageStatistics extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.usageStatistics"; //$NON-NLS-1$
    public static final String LINK_PRIVACY_INFO = "https://dbeaver.com/privacy/";
    public static final String LINK_GIHUB_REPO = "https://github.com/dbeaver/dbeaver";

    private Button checkSendUsageStatistics;
    // Disabled for now. It is too annoying UIX
    //private Button checkShowStatisticsDetails;

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
        //checkShowStatisticsDetails = UIUtils.createCheckbox(group, "Show statistics details before sending", false);
        UIUtils.createLabel(group, "We send statistics before application shutdown or during startup.\n" +
                "Information we send is:\n" +
            "  - Brief information about your OS and locale\n" +
            "  - List of actions you perform in UI to better understand users workflow\n" +
            "  - Type of databases you use to improve support of popular ones"
            );
        UIUtils.createEmptyLabel(group, 1, 1);
        UIUtils.createLink(group,
            "DBeaver is open source and you can always validate what exactly we send\n" +
                "in our source code <a>" + LINK_GIHUB_REPO + "</a>\n",
            SelectionListener.widgetSelectedAdapter(selectionEvent ->
                ShellUtils.launchProgram(LINK_GIHUB_REPO)));


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
                ShellUtils.launchProgram(LINK_PRIVACY_INFO)));
    }

    @Override
    protected void performDefaults() {
        checkSendUsageStatistics.setSelection(UIStatisticsActivator.isTrackingEnabled());
        //checkShowStatisticsDetails.setSelection(UIStatisticsActivator.isDetailsPreviewEnabled());

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        UIStatisticsActivator.setTrackingEnabled(checkSendUsageStatistics.getSelection());
        //UIStatisticsActivator.setDetailsPreviewEnabled(checkShowStatisticsDetails.getSelection());

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
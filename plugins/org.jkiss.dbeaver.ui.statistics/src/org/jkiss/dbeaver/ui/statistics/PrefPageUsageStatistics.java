/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.osgi.util.NLS;
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
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.statistics.StatisticCollectionMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

/**
 * PrefPageUsageStatistics
 */
public class PrefPageUsageStatistics extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.usageStatistics"; //$NON-NLS-1$
    public static final String LINK_PRIVACY_INFO = "https://dbeaver.com/privacy/";
    public static final String LINK_STATISTIC_DETAILS = "https://dbeaver.com/privacy/";
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
        Composite group = UIUtils.createControlGroup(
            composite,
            StatisticCollectionMessages.statistic_collection_pref_group_label,
            1,
            GridData.FILL_HORIZONTAL,
            SWT.DEFAULT);
        checkSendUsageStatistics = UIUtils.createCheckbox(
            group,
            StatisticCollectionMessages.statistic_collection_pref_send_btn_label,
            false);
        createDataShareComposite(group);

        UIUtils.createEmptyLabel(group, 1, 1);
        UIUtils.createLabel(group, StatisticCollectionMessages.statistic_collection_pref_content_main_msg);
        UIUtils.createLink(group, StatisticCollectionMessages.statistic_collection_pref_content_documentation_link,
            SelectionListener.widgetSelectedAdapter(selectionEvent -> ShellUtils.launchProgram(LINK_STATISTIC_DETAILS)));
        UIUtils.createEmptyLabel(group, 1, 1);
        UIUtils.createLink(group,
            NLS.bind(StatisticCollectionMessages.statistic_collection_pref_content_opensource_link, LINK_GIHUB_REPO),
            SelectionListener.widgetSelectedAdapter(selectionEvent -> ShellUtils.launchProgram(LINK_GIHUB_REPO)));

        if (DBWorkbench.getPlatform().getApplication().isStatisticsCollectionRequired()) {
            checkSendUsageStatistics.setEnabled(false);
            UIUtils.createEmptyLabel(composite, 1, 1);
            UIUtils.createInfoLabel(composite, "You cannot opt-out from data sharing in this version of DBeaver.");
        }

        performDefaults();
        return composite;
    }

    public static void createDataShareComposite(Composite group) {
        UIUtils.createLink(group, StatisticCollectionMessages.statistic_collection_pref_content_datashare_msg,
            SelectionListener.widgetSelectedAdapter(selectionEvent -> ShellUtils.launchProgram(LINK_PRIVACY_INFO)));
    }

    @Override
    protected void performDefaults() {
        checkSendUsageStatistics.setSelection(UIStatisticsActivator.isTrackingEnabled());
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        UIStatisticsActivator.setTrackingEnabled(checkSendUsageStatistics.getSelection());
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
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardItem;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardList;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardListViewer;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/**
 * Dashboard view dialog
 */
public class DashboardItemViewDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardItemViewDialog";//$NON-NLS-1$

    private final DashboardViewContainer parentPart;
    private final DashboardItem sourceItem;

    public DashboardItemViewDialog(DashboardViewContainer parentPart, DashboardItem sourceItem) {
        super(parentPart.getSite().getShell(), UIDashboardMessages.dialog_dashboard_item_view_title, null);

        this.parentPart = parentPart;
        this.sourceItem = sourceItem;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(UIDashboardActivator.getDefault().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        Composite chartGroup = UIUtils.createPlaceholder(dialogArea, 1);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 450;
        gd.heightHint = 300;
        chartGroup.setLayoutData(gd);
        chartGroup.setLayout(new FillLayout());

        DashboardListViewer dashboardListViewer = new DashboardListViewer(
            parentPart.getSite(),
            sourceItem.getDataSourceContainer(),
            parentPart.getViewConfiguration());
        dashboardListViewer.setSingleChartMode(true);
        dashboardListViewer.createControl(chartGroup);

        DashboardItem targetItem  = new DashboardItem(
            (DashboardList) dashboardListViewer.getDefaultGroup(),
            sourceItem.getDashboardId());
        targetItem.moveViewFrom(sourceItem, false);

        return dialogArea;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);

        UIUtils.asyncExec(() -> getButton(IDialogConstants.OK_ID).setFocus());
    }

}
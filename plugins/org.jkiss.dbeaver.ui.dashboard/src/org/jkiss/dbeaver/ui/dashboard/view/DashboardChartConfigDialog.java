/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.utils.CommonUtils;

import java.time.Duration;
import java.util.List;

public class DashboardChartConfigDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardChartConfigDialog";//$NON-NLS-1$

    private final DashboardItemViewConfiguration dashboardConfig;
    private DashboardViewConfiguration viewConfiguration;
    private DashboardContainer dashboardContainer;

    public DashboardChartConfigDialog(Shell shell, DashboardContainer dashboardContainer, DashboardViewConfiguration viewConfiguration)
    {
        super(shell, "Dashboard [" + dashboardContainer.getDashboardTitle() + "]", null);

        this.viewConfiguration = viewConfiguration;
        this.dashboardContainer = dashboardContainer;
        this.dashboardConfig = viewConfiguration.getDashboardConfig(dashboardContainer.getDashboardId());
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return null;//UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        {
            Group infoGroup = UIUtils.createControlGroup(composite, "Dashboard info", 4, GridData.FILL_HORIZONTAL, 0);

            //UIUtils.createLabelText(infoGroup, "ID", dashboardConfig.getDashboardDescriptor().getId(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(infoGroup, "Name", dashboardConfig.getDashboardDescriptor().getName(), SWT.BORDER | SWT.READ_ONLY)
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            UIUtils.createLabelText(infoGroup, "Group", CommonUtils.notEmpty(dashboardConfig.getDashboardDescriptor().getGroup()), SWT.BORDER | SWT.READ_ONLY)
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            Combo typeCombo = UIUtils.createLabelCombo(infoGroup, "Type", "Dashboard type", SWT.BORDER | SWT.READ_ONLY);
            typeCombo.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            typeCombo.add(dashboardConfig.getDashboardDescriptor().getType().getTitle());
            typeCombo.select(0);
            UIUtils.createLabelText(infoGroup, "Calc type", dashboardConfig.getDashboardDescriptor().getCalcType().name(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(infoGroup, "Fetch type", dashboardConfig.getDashboardDescriptor().getFetchType().name(), SWT.BORDER | SWT.READ_ONLY);

            Composite btnGroup = UIUtils.createComposite(infoGroup, 1);
            btnGroup.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 4, 1));
            Button queriesButton = new Button(btnGroup, SWT.PUSH);
            queriesButton.setText("SQL Queries ...");
            queriesButton.setImage(DBeaverIcons.getImage(UIIcon.SQL_SCRIPT));
            queriesButton.setLayoutData(new GridData(GridData.END, GridData.BEGINNING, true, false));
            queriesButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    StringBuilder sql = new StringBuilder();
                    for (DashboardDescriptor.QueryMapping query : dashboardConfig.getDashboardDescriptor().getQueries()) {
                        sql.append(query.getQueryText()).append(";\n");
                    }
                    UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                    if (serviceSQL != null) {
                        serviceSQL.openSQLViewer(
                            DBUtils.getDefaultContext(dashboardContainer.getDataSourceContainer().getDataSource(), true),
                            "Dashboard read queries",
                            UIIcon.SQL_SCRIPT,
                            sql.toString(),
                            false);
                    }
                }
            });
            queriesButton.setEnabled(dashboardContainer.getDataSourceContainer().isConnected());
        }

        {
            Group updateGroup = UIUtils.createControlGroup(composite, "Dashboard update", 2, GridData.FILL_HORIZONTAL, 0);

            Text updatePeriodText = UIUtils.createLabelText(updateGroup, "Update period (ms)", String.valueOf(dashboardConfig.getUpdatePeriod()), SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            Text maxItemsText = UIUtils.createLabelText(updateGroup, "Maximum items", String.valueOf(dashboardConfig.getMaxItems()), SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            Text maxAgeText = UIUtils.createLabelText(updateGroup, "Maximum age (ISO-8601)", Duration.ofMillis(dashboardConfig.getMaxAge()).toString().substring(2), SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
        }

        {
            Group updateGroup = UIUtils.createControlGroup(composite, "Dashboard view", 2, GridData.FILL_HORIZONTAL, 0);

            Text widthRatioText = UIUtils.createLabelText(updateGroup, "Width ratio", String.valueOf(dashboardConfig.getWidthRatio()), SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            Text descriptionText = UIUtils.createLabelText(updateGroup, "Description", CommonUtils.notEmpty(dashboardConfig.getDescription()), SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 200;
            gd.heightHint = 50;
            descriptionText.setLayoutData(gd);
        }

        return parent;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        return contents;
    }


    @Override
    protected void okPressed() {
        super.okPressed();
    }
}

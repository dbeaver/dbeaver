/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.osgi.util.NLS;
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
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewType;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DashboardItemConfigDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardItemConfigDialog";//$NON-NLS-1$
    private static final boolean SHOW_QUERIES_BUTTON = false;

    private final DashboardItemViewConfiguration dashboardConfig;
    private DashboardViewConfiguration viewConfiguration;
    private DashboardContainer dashboardContainer;

    public DashboardItemConfigDialog(Shell shell, DashboardContainer dashboardContainer, DashboardViewConfiguration viewConfiguration) {
        super(shell, NLS.bind(UIDashboardMessages.dialog_dashboard_item_config_title, dashboardContainer.getDashboardTitle()), null);

        this.viewConfiguration = viewConfiguration;
        this.dashboardContainer = dashboardContainer;
        this.dashboardConfig = new DashboardItemViewConfiguration(viewConfiguration.getDashboardConfig(dashboardContainer.getDashboardId()));
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
            Group infoGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_dashboard_item_config_dashboardinfo, 4, GridData.FILL_HORIZONTAL, 0);

            //UIUtils.createLabelText(infoGroup, "ID", dashboardConfig.getDashboardDescriptor().getId(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(infoGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardinfo_labels_name, dashboardConfig.getDashboardDescriptor().getName(), SWT.BORDER | SWT.READ_ONLY)
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));

            UIUtils.createControlLabel(infoGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardinfo_labels_description).setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            Text descriptionText = new Text(infoGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            descriptionText.setText(CommonUtils.notEmpty(dashboardConfig.getDescription()));
            descriptionText.addModifyListener(e -> {
                dashboardConfig.setDescription(descriptionText.getText());
            });
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 200;
            gd.heightHint = 50;
            descriptionText.setLayoutData(gd);

            if (SHOW_QUERIES_BUTTON) {
                Composite btnGroup = UIUtils.createComposite(infoGroup, 1);
                btnGroup.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 4, 1));
                Button queriesButton = new Button(btnGroup, SWT.PUSH);
                queriesButton.setText(UIDashboardMessages.dialog_dashboard_item_config_buttons_sqlqueries);
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
                                UIDashboardMessages.dialog_dashboard_item_config_buttons_sqlqueries_dash,
                                UIIcon.SQL_SCRIPT,
                                sql.toString(),
                                false, false);
                        }
                    }
                });
                queriesButton.setEnabled(dashboardContainer.getDataSourceContainer().isConnected());
            }
        }

        {
            Group updateGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_dashboard_item_config_dashboardupdate, 2, GridData.FILL_HORIZONTAL, 0);

            Text updatePeriodText = UIUtils.createLabelText(updateGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardupdate_labels_updateperiod, String.valueOf(dashboardConfig.getUpdatePeriod()), SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            updatePeriodText.addModifyListener(e -> {
                dashboardConfig.setUpdatePeriod(CommonUtils.toLong(updatePeriodText.getText(), dashboardConfig.getUpdatePeriod()));
            });
            Text maxItemsText = UIUtils.createLabelText(updateGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardupdate_labels_maxitems, String.valueOf(dashboardConfig.getMaxItems()), SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            maxItemsText.addModifyListener(e -> {
                dashboardConfig.setMaxItems(CommonUtils.toInt(maxItemsText.getText(), dashboardConfig.getMaxItems()));
            });
/*
            Text maxAgeText = UIUtils.createLabelText(updateGroup, "Maximum age (ISO-8601)", DashboardUtils.formatDuration(dashboardConfig.getMaxAge()), SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            maxAgeText.addModifyListener(e -> {
                dashboardConfig.setMaxAge(DashboardUtils.parseDuration(maxAgeText.getText(), dashboardConfig.getMaxAge()));
            });
*/
        }

        {
            Group viewGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_dashboard_item_config_dashboardview, 2, GridData.FILL_HORIZONTAL, 0);

            Combo typeCombo = UIUtils.createLabelCombo(viewGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_combos_view, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_combos_view_tooltip, SWT.BORDER | SWT.READ_ONLY);
            typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            {
                List<DashboardViewType> viewTypes = DashboardRegistry.getInstance().getSupportedViewTypes(dashboardConfig.getDashboardDescriptor().getDataType());
                for (DashboardViewType viewType : viewTypes) {
                    typeCombo.add(viewType.getTitle());
                }
                typeCombo.setText(dashboardConfig.getViewType().getTitle());
                typeCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        dashboardConfig.setViewType(viewTypes.get(typeCombo.getSelectionIndex()));
                    }
                });
            }

            UIUtils.createCheckbox(viewGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_legend, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_legend_tooltip, dashboardConfig.isLegendVisible(), 2)
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        dashboardConfig.setLegendVisible(((Button)e.widget).getSelection());
                    }
                });
            UIUtils.createCheckbox(viewGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_grid, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_grid_tooltip, dashboardConfig.isGridVisible(), 2)
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        dashboardConfig.setGridVisible(((Button)e.widget).getSelection());
                    }
                });
            UIUtils.createCheckbox(viewGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_domainaxis, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_domainaxis_tooltip, dashboardConfig.isDomainTicksVisible(), 2)
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        dashboardConfig.setDomainTicksVisible(((Button)e.widget).getSelection());
                    }
                });
            UIUtils.createCheckbox(viewGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_rangeaxis, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_rangeaxis_tooltip, dashboardConfig.isDomainTicksVisible(), 2)
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        dashboardConfig.setRangeTicksVisible(((Button)e.widget).getSelection());
                    }
                });
/*
            Text widthRatioText = UIUtils.createLabelText(viewGroup, "Width ratio", String.valueOf(dashboardConfig.getWidthRatio()), SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            widthRatioText.addModifyListener(e -> {
                dashboardConfig.setWidthRatio((float) CommonUtils.toDouble(widthRatioText.getText(), dashboardConfig.getWidthRatio()));
            });
*/
        }

        return parent;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        return contents;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, UIDashboardMessages.dialog_dashboard_item_config_buttons_configuration, false).addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DashboardEditDialog editDialog = new DashboardEditDialog(getShell(), dashboardConfig.getDashboardDescriptor());
                editDialog.open();
            }
        });
        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void okPressed() {
        super.okPressed();
        viewConfiguration.updateDashboardConfig(this.dashboardConfig);
        viewConfiguration.saveSettings();
    }
}
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
package org.jkiss.dbeaver.ui.dashboard.control;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemViewSettings;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardRendererType;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardUIRegistry;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DatabaseChartViewSettingsEditor implements IObjectPropertyConfigurator<DashboardItemViewSettings, DashboardItemViewSettings> {
    @Override
    public void createControl(@NotNull Composite composite, DashboardItemViewSettings dashboardConfig, @NotNull Runnable propertyChangeListener) {
        {
            Group updateGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_dashboard_item_config_dashboardupdate, 2, GridData.FILL_HORIZONTAL, 0);

            Text updatePeriodText = UIUtils.createLabelText(
                updateGroup,
                UIDashboardMessages.dialog_dashboard_item_config_dashboardupdate_labels_updateperiod,
                String.valueOf(dashboardConfig.getUpdatePeriod()),
                SWT.BORDER,
                new GridData(GridData.FILL_HORIZONTAL));
            updatePeriodText.addModifyListener(e ->
                dashboardConfig.setUpdatePeriod(
                    CommonUtils.toLong(updatePeriodText.getText(), dashboardConfig.getUpdatePeriod())));
            Text maxItemsText = UIUtils.createLabelText(
                updateGroup,
                UIDashboardMessages.dialog_dashboard_item_config_dashboardupdate_labels_maxitems,
                String.valueOf(dashboardConfig.getMaxItems()),
                SWT.BORDER,
                new GridData(GridData.FILL_HORIZONTAL));
            maxItemsText.addModifyListener(e ->
                dashboardConfig.setMaxItems(CommonUtils.toInt(maxItemsText.getText(), dashboardConfig.getMaxItems())));
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
                List<DashboardRendererType> viewTypes = DashboardUIRegistry.getInstance().getSupportedViewTypes(dashboardConfig.getItemConfiguration().getDataType());
                for (DashboardRendererType viewType : viewTypes) {
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
            UIUtils.createCheckbox(viewGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_rangeaxis, UIDashboardMessages.dialog_dashboard_item_config_dashboardview_checkboxes_rangeaxis_tooltip, dashboardConfig.isRangeTicksVisible(), 2)
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
    }

    @Override
    public void loadSettings(@NotNull DashboardItemViewSettings viewSettings) {

    }

    @Override
    public void saveSettings(@NotNull DashboardItemViewSettings viewSettings) {

    }

    @Override
    public void resetSettings(@NotNull DashboardItemViewSettings viewSettings) {

    }

    @Override
    public boolean isComplete() {
        return true;
    }
}

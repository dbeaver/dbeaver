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

package org.jkiss.dbeaver.ui.dashboard.control;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.jfree.chart.JFreeChart;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.charts.BaseChartComposite;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConstants;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewType;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardItemConfigDialog;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardItemViewDialog;

import java.util.List;

/**
 * Dashboard chart composite
 */
public class DashboardChartComposite extends BaseChartComposite {

    private final DashboardViewContainer viewContainer;
    private final DashboardContainer dashboardContainer;

    public DashboardChartComposite(DashboardContainer dashboardContainer, DashboardViewContainer viewContainer, Composite parent, int style, Point preferredSize) {
        super(parent, style, preferredSize);
        this.dashboardContainer = dashboardContainer;
        this.viewContainer = viewContainer;
    }

    public DashboardViewContainer getViewContainer() {
        return viewContainer;
    }

    // It is a hack. As context menu create is called from base lcass constructor we can't use any constructor parameters in fillContextMenu.
    // Lets give caller a chance to use them in overloaded member
    protected boolean isSingleChartMode() {
        return viewContainer.isSingleChartMode();
    }

    @Override
    protected void fillContextMenu(IMenuManager manager) {
        if (!isSingleChartMode()) {
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), DashboardConstants.CMD_VIEW_DASHBOARD));
            manager.add(new Separator());
        }
        super.fillContextMenu(manager);
        if (!UIUtils.isInDialog(this)) {
            manager.add(new Separator());
            MenuManager viewMenu = new MenuManager("View as");
            List<DashboardViewType> viewTypes = DashboardRegistry.getInstance().getSupportedViewTypes(dashboardContainer.getDashboardDataType());
            for (DashboardViewType viewType : viewTypes) {
                Action changeViewAction = new Action(viewType.getTitle(), Action.AS_RADIO_BUTTON) {
                    @Override
                    public boolean isChecked() {
                        return dashboardContainer.getDashboardViewType() == viewType;
                    }

                    @Override
                    public void runWithEvent(Event event) {
                        ((DashboardItem) dashboardContainer).getDashboardConfig().setViewType(viewType);
                        dashboardContainer.getGroup().getView().getViewConfiguration().saveSettings();
                        dashboardContainer.updateDashboardView();
                    }
                };
                if (viewType.getIcon() != null) {
                    changeViewAction.setImageDescriptor(DBeaverIcons.getImageDescriptor(viewType.getIcon()));
                }
                viewMenu.add(changeViewAction);
            }
            manager.add(viewMenu);
        }
        if (!isSingleChartMode()) {
            manager.add(new Separator());
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), DashboardConstants.CMD_ADD_DASHBOARD));
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), DashboardConstants.CMD_REMOVE_DASHBOARD));
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), DashboardConstants.CMD_RESET_DASHBOARD));
        }
    }

    @Override
    public void setChart(JFreeChart chart) {
        super.setChart(chart);
        if (chart != null && !isSingleChartMode()) {
            this.setDomainZoomable(false);
            this.setRangeZoomable(false);
        }
    }

    protected boolean showChartConfigDialog() {
        DashboardItemConfigDialog dialog = new DashboardItemConfigDialog(
            this.getShell(),
            dashboardContainer,
            viewContainer.getViewConfiguration());
        boolean changed = dialog.open() == IDialogConstants.OK_ID;
        if (changed) {
            dashboardContainer.updateDashboardView();
        }
        return changed;
    }

    @Override
    public void mouseDoubleClick(MouseEvent event) {
        if (viewContainer.isSingleChartMode()) {
            restoreAutoBounds();
        } else {
            DashboardItemViewDialog viewDialog = new DashboardItemViewDialog(viewContainer, (DashboardItem) dashboardContainer);
            viewDialog.open();
        }
    }

}

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

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.JFreeChart;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.charts.BaseChartComposite;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConstants;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardChartConfigDialog;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardViewDialog;

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
    protected void fillContextMenu(MenuManager manager) {
        if (!isSingleChartMode()) {
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), DashboardConstants.CMD_VIEW_DASHBOARD));
            manager.add(new Separator());
        }
        super.fillContextMenu(manager);
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
        if (!isSingleChartMode()) {
            this.setDomainZoomable(false);
            this.setRangeZoomable(false);
        }
    }

    protected boolean showChartConfigDialog() {
        DashboardChartConfigDialog dialog = new DashboardChartConfigDialog(
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
        DashboardViewDialog viewDialog = new DashboardViewDialog(viewContainer, (DashboardItem) dashboardContainer);
        viewDialog.open();
    }

}

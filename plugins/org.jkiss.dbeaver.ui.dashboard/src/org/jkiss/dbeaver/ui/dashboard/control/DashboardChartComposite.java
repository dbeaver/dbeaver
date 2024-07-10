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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jfree.chart.JFreeChart;
import org.jkiss.dbeaver.ui.charts.BaseChartComposite;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemContainer;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardItemViewDialog;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardItemViewSettingsDialog;

/**
 * Dashboard chart composite
 */
public class DashboardChartComposite extends BaseChartComposite implements DashboardViewCompositeControl {

    private final DashboardContainer viewContainer;
    private final DashboardItemContainer dashboardContainer;

    public DashboardChartComposite(
        DashboardItemContainer dashboardContainer,
        DashboardContainer viewContainer,
        Composite parent,
        int style,
        Point preferredSize
    ) {
        super(parent, style, preferredSize);
        this.dashboardContainer = dashboardContainer;
        this.viewContainer = viewContainer;
    }

    public DashboardContainer getViewContainer() {
        return viewContainer;
    }

    // It is a hack. As context menu create is called from base class constructor we can't use any constructor parameters in fillContextMenu.
    // Let's give caller a chance to use them in overloaded member
    protected boolean isSingleChartMode() {
        return viewContainer.isSingleChartMode();
    }

    @Override
    protected void fillContextMenu(IMenuManager manager) {
        dashboardContainer.fillDashboardContextMenu(manager, isSingleChartMode());
        super.fillContextMenu(manager);
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
        DashboardItemViewSettingsDialog dialog = new DashboardItemViewSettingsDialog(
            this.getShell(),
            dashboardContainer,
            viewContainer.getViewConfiguration());
        boolean changed = dialog.open() == IDialogConstants.OK_ID;
        if (changed) {
            dashboardContainer.updateDashboardView();
            viewContainer.saveChanges();
        }
        return changed;
    }

    @Override
    public void mouseDoubleClick(MouseEvent event) {
        if (viewContainer.isSingleChartMode()) {
            restoreAutoBounds();
        } else {
            DashboardItemViewDialog viewDialog = new DashboardItemViewDialog(
                viewContainer,
                viewContainer.getConfiguration(),
                (DashboardViewItem) dashboardContainer);
            viewDialog.open();
        }
    }

    @Override
    public Control getDashboardControl() {
        return getChartCanvas();
    }
}

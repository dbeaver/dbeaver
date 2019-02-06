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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.jfree.chart.swt.ChartComposite;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;

/**
 * Dashboard chart composite
 */
public class DashboardChartComposite extends ChartComposite {

    public static final String CHART_CONFIG_COMMAND = "CHART_CONFIG";
    private DashboardContainer dashboardContainer;

    public DashboardChartComposite(DashboardContainer dashboardContainer, Composite parent, int style, Point preferredSize) {
        super(parent, style, null,
            preferredSize.x, preferredSize.y,
            30, 20,
            10000, 10000,
            true, false, true, true, true, true);
        this.dashboardContainer = dashboardContainer;
    }

    public Canvas getChartCanvas() {
        Control[] children = getChildren();
        return children.length == 0 ? null : (Canvas) children[0];
    }

    @Override
    public void mouseDoubleClick(MouseEvent event) {
        if (showChartConfigDialog()) {
            forceRedraw();
        }
    }

    @Override
    protected Menu createPopupMenu(boolean properties, boolean save, boolean print, boolean zoom) {
        Menu popupMenu = super.createPopupMenu(properties, save, print, zoom);

        new MenuItem(popupMenu, SWT.SEPARATOR, 0);

        MenuItem printItem = new MenuItem(popupMenu, SWT.PUSH, 0);
        printItem.setText("Settings ...");
        printItem.setImage(DBeaverIcons.getImage(UIIcon.CONFIGURATION));
        printItem.setData(CHART_CONFIG_COMMAND);
        printItem.addSelectionListener(this);

        return popupMenu;
    }

    public void widgetSelected(SelectionEvent e) {
        if (CHART_CONFIG_COMMAND.equals(((MenuItem) e.getSource()).getData())) {
            if (showChartConfigDialog()) {
                forceRedraw();
            }
        } else {
            super.widgetSelected(e);
        }

    }

    boolean showChartConfigDialog() {
        DashboardChartConfigDialog dialog = new DashboardChartConfigDialog(this, dashboardContainer);
        return dialog.open() == IDialogConstants.OK_ID;
    }

}

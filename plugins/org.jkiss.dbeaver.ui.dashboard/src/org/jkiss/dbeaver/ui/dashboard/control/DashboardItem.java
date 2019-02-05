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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;

public class DashboardItem extends Composite implements DashboardContainer {

    public static final int DEFAULT_HEIGHT = 200;
    private DashboardDescriptor dashboardDescriptor;

    public DashboardItem(DashboardList parent, DashboardDescriptor dashboardDescriptor) {
        super(parent, SWT.BORDER);
        parent.addItem(this);
        addDisposeListener(e -> parent.removeItem(this));

        GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        this.dashboardDescriptor = dashboardDescriptor;

        try {
            Composite chartComposite = new Composite(this, SWT.NONE);
            chartComposite.setLayout(new FillLayout());
            chartComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
            DashboardRenderer renderer = dashboardDescriptor.getType().createRenderer();
            Control dashboardControl = renderer.createDashboard(chartComposite, this, computeSize(-1, -1));
        } catch (DBException e) {
            // Something went wrong
            Text errorLabel = new Text(this, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
            errorLabel.setText("Error creating " + dashboardDescriptor.getLabel() + " renderer: " + e.getMessage());
            errorLabel.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));
        }
    }

    public DashboardList getParent() {
        return (DashboardList) super.getParent();
    }

    public int getDefaultHeight() {
        return DEFAULT_HEIGHT;
    }

    public int getDefaultWidth() {
        return (int) (dashboardDescriptor.getWidthRatio() * getDefaultHeight());
    }
    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        DashboardList list = getParent();

        int defHeight = getDefaultHeight();
        int defWidth = getDefaultWidth();
        Point areaSize = list.getSize();
        if (areaSize.x <= defWidth || areaSize.y <= defHeight) {
            return new Point(defWidth, defHeight);
        }
        // Use some insets
        areaSize.x -= 10;
        areaSize.y -= 10;

        int extraWidthSpace = 0;
        int extraHeightSpace = 0;
        int totalWidth = 0;
        int totalHeight = 0;

        if (areaSize.x > areaSize.y) {
            // Horizontal
            totalHeight = defHeight;
            for (DashboardItem item : list.getItems()) {
                if (totalWidth > 0) totalWidth += list.getItemSpacing();
                totalWidth += item.getDefaultWidth();
            }
            if (totalWidth < areaSize.x) {
                // Stretch to fit height
                extraWidthSpace = areaSize.x - totalWidth;
                extraHeightSpace = areaSize.y - defHeight;
            }
        } else {
            // Vertical
            totalWidth = defWidth;
            for (DashboardItem item : list.getItems()) {
                if (totalHeight > 0) totalHeight += list.getItemSpacing();
                totalHeight += item.getDefaultHeight();
            }
            if (totalHeight < areaSize.y) {
                // Stretch to fit width
                // Stretch to fit height
                extraWidthSpace = areaSize.x - defWidth;
                extraHeightSpace = areaSize.y - totalHeight;
            }
        }
        if (extraHeightSpace > 0 && extraWidthSpace > 0) {
            // Stretch
            int widthIncreasePercent = 100 * areaSize.x / totalWidth;
            int heightIncreasePercent = 100 * areaSize.y / totalHeight;
            int increasePercent = Math.min(widthIncreasePercent, heightIncreasePercent);
            return new Point(
                (defWidth * increasePercent / 100),
                (defHeight * increasePercent / 100));
        } else {
            return new Point(defWidth, defHeight);
        }
    }

    @Override
    public String getDashboardTitle() {
        return dashboardDescriptor.getLabel();
    }

    @Override
    public String getDashboardDescription() {
        return dashboardDescriptor.getDescription();
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return getParent().getDataSourceContainer();
    }

}

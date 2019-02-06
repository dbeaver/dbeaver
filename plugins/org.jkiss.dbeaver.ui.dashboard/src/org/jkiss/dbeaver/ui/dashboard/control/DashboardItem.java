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
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDataset;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;

import java.util.Date;
import java.util.List;

public class DashboardItem extends Composite implements DashboardContainer {

    public static final int DEFAULT_HEIGHT = 200;
    private DashboardList groupContainer;
    private DashboardDescriptor dashboardDescriptor;

    private Date lastUpdateTime;
    private DashboardRenderer renderer;
    private DashboardChartComposite dashboardControl;

    public DashboardItem(DashboardList parent, DashboardDescriptor dashboardDescriptor) {
        super(parent, SWT.DOUBLE_BUFFERED);
        this.groupContainer = parent;
        groupContainer.addItem(this);

        addDisposeListener(e -> groupContainer.removeItem(this));

        this.addPaintListener(e -> paintItem(e));

        GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        this.setLayout(layout);

        this.dashboardDescriptor = dashboardDescriptor;

        {
            Composite titleComposite = new Composite(this, SWT.NONE);
            titleComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            FillLayout fillLayout = new FillLayout();
            fillLayout.marginHeight = 3;
            fillLayout.marginWidth = 3;
            titleComposite.setLayout(fillLayout);
            Label titleLabel = new Label(titleComposite, SWT.NONE);
            titleLabel.setFont(parent.getTitleFont());
            //GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            //titleLabel.setLayoutData(gd);
            //titleLabel.setForeground(titleLabel.getDisplay().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));
            //titleLabel.setBackground(titleLabel.getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND));
            titleLabel.setText("  " + dashboardDescriptor.getLabel());
        }

        try {
            Composite chartComposite = new Composite(this, SWT.NONE);
            chartComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
            chartComposite.setLayout(new FillLayout());
            renderer = dashboardDescriptor.getType().createRenderer();
            dashboardControl = renderer.createDashboard(chartComposite, this, computeSize(-1, -1));

        } catch (DBException e) {
            // Something went wrong
            Text errorLabel = new Text(this, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
            errorLabel.setText("Error creating " + dashboardDescriptor.getLabel() + " renderer: " + e.getMessage());
            errorLabel.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));
        }

        if (dashboardControl != null) {
            Canvas chartCanvas = dashboardControl.getChartCanvas();
            chartCanvas.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    chartCanvas.setFocus();
                }
            });
            chartCanvas.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    groupContainer.setSelection(DashboardItem.this);
                    redraw();
                }

                @Override
                public void focusLost(FocusEvent e) {

                }
            });
        }

    }

    private void paintItem(PaintEvent e) {
        Point itemSize = getSize();
        e.gc.setLineWidth(groupContainer.getSelectedItem() == this ? 2 : 1);
        e.gc.drawRoundRectangle(1, 1, itemSize.x - 2, itemSize.y - 2, 3, 3);
//        if (groupContainer.getSelectedItem() == this) {
//            e.gc.drawRoundRectangle(1, 1, itemSize.x - 4, itemSize.y - 4, 3, 3);
//        }
    }

    public int getDefaultHeight() {
        return DEFAULT_HEIGHT;
    }

    public int getDefaultWidth() {
        return (int) (dashboardDescriptor.getWidthRatio() * getDefaultHeight());
    }
    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        Point currentSize = getSize();

        int defHeight = getDefaultHeight();
        int defWidth = getDefaultWidth();
        Point areaSize = groupContainer.getSize();
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
            for (DashboardItem item : groupContainer.getItems()) {
                if (totalWidth > 0) totalWidth += groupContainer.getItemSpacing();
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
            for (DashboardItem item : groupContainer.getItems()) {
                if (totalHeight > 0) totalHeight += groupContainer.getItemSpacing();
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

            Point compSize = new Point(
                (defWidth * increasePercent / 100),
                (defHeight * increasePercent / 100));

            if (currentSize.x > 0 && currentSize.y > 0) {
                // Grab all extra space if possible
                //System.out.println("NEw size: " + compSize);
            }
            return compSize;
        } else {
            return new Point(defWidth, defHeight);
        }
    }

    @Override
    public String getDashboardId() {
        return dashboardDescriptor.getId();
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
    public DashboardType getDashboardType() {
        return dashboardDescriptor.getType();
    }

    @Override
    public DashboardCalcType getDashboardCalcType() {
        return dashboardDescriptor.getCalcType();
    }

    @Override
    public DashboardFetchType getDashboardFetchType() {
        return dashboardDescriptor.getFetchType();
    }

    @Override
    public int getDashboardMaxItems() {
        return dashboardDescriptor.getMaxItems();
    }

    @Override
    public long getDashboardMaxAge() {
        return dashboardDescriptor.getMaxAge();
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return groupContainer.getDataSourceContainer();
    }

    @Override
    public DashboardGroupContainer getGroup() {
        return groupContainer;
    }

    @Override
    public List<? extends DashboardQuery> getQueryList() {
        return dashboardDescriptor.getQueries();
    }

    @Override
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public void updateDashboardData(DashboardDataset dataset) {
        UIUtils.asyncExec(() -> {
            if (renderer != null) {
                renderer.updateDashboardData(this, lastUpdateTime, dataset);
            }
        });
        lastUpdateTime = new Date();
    }

    @Override
    public long getUpdatePeriod() {
        return dashboardDescriptor.getUpdatePeriod();
    }

    @Override
    public DashboardChartComposite getDashboardControl() {
        return dashboardControl;
    }


}

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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDataset;

import java.util.Date;
import java.util.List;

public class DashboardItem extends Composite implements DashboardContainer {

    public static final int DEFAULT_HEIGHT = 200;
    private DashboardList groupContainer;
    private final DashboardItemViewConfiguration dashboardConfig;

    private Date lastUpdateTime;
    private DashboardRenderer renderer;
    private DashboardChartComposite dashboardControl;
    private final Label titleLabel;

    public DashboardItem(DashboardList parent, String dashboardId) {
        super(parent, SWT.DOUBLE_BUFFERED);
        this.groupContainer = parent;
        this.dashboardConfig = groupContainer.getView().getViewConfiguration().getDashboardConfig(dashboardId);

        GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        this.setLayout(layout);

        Color defFG = UIStyles.getDefaultTextForeground();
        Color defBG = UIStyles.getDefaultTextBackground();
        this.setForeground(defFG);
        this.setBackground(defBG);

        {
            Composite titleComposite = new Composite(this, SWT.NONE);
            titleComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            titleComposite.setBackground(defBG);
            titleComposite.setForeground(defFG);
            FillLayout fillLayout = new FillLayout();
            fillLayout.marginHeight = 3;
            fillLayout.marginWidth = 3;
            titleComposite.setLayout(fillLayout);
            titleLabel = new Label(titleComposite, SWT.NONE);
            titleLabel.setFont(parent.getTitleFont());
            titleLabel.setText("  " + dashboardConfig.getDashboardDescriptor().getName());
        }

        try {
            Composite chartComposite = new Composite(this, SWT.NONE);
            chartComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
            chartComposite.setLayout(new FillLayout());
            renderer = dashboardConfig.getDashboardDescriptor().getDefaultViewType().createRenderer();
            dashboardControl = renderer.createDashboard(chartComposite, this, groupContainer.getView(), computeSize(-1, -1));

        } catch (DBException e) {
            // Something went wrong
            Text errorLabel = new Text(this, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
            errorLabel.setText("Error creating " + dashboardConfig.getDashboardDescriptor().getName() + " renderer: " + e.getMessage());
            errorLabel.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));
        }

        if (dashboardControl != null) {
            Canvas chartCanvas = dashboardControl.getChartCanvas();
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    chartCanvas.setFocus();
                }
            };
            this.addMouseListener(mouseAdapter);
            chartCanvas.addMouseListener(mouseAdapter);
            titleLabel.addMouseListener(mouseAdapter);

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

            chartCanvas.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    groupContainer.handleKeyEvent(e);
                }
            });

            dashboardControl.addDisposeListener(e -> renderer.disposeDashboard(DashboardItem.this));
        }

        groupContainer.addItem(this);
        addDisposeListener(e -> groupContainer.removeItem(this));

        this.addPaintListener(this::paintItem);
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    private void paintItem(PaintEvent e) {
        Point itemSize = getSize();
        e.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
        if (groupContainer.getSelectedItem() == this) {
            e.gc.setLineWidth(2);
            e.gc.setLineStyle(SWT.LINE_SOLID);
        } else {
            e.gc.setLineWidth(1);
            e.gc.setLineStyle(SWT.LINE_CUSTOM);
            e.gc.setLineDash(new int[] {10, 10});
        }
        e.gc.drawRectangle(1, 1, itemSize.x - 2, itemSize.y - 2);
//        if (groupContainer.getSelectedItem() == this) {
//            e.gc.drawRoundRectangle(1, 1, itemSize.x - 4, itemSize.y - 4, 3, 3);
//        }
    }

    public int getDefaultHeight() {
        return DEFAULT_HEIGHT;
    }

    public int getDefaultWidth() {
        return (int) (dashboardConfig.getWidthRatio() * getDefaultHeight());
    }
    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        if (groupContainer.getView().isSingleChartMode()) {
            return super.computeSize(wHint, hHint, changed);
        }
        Point currentSize = getSize();

        int defHeight = getDefaultHeight();
        int defWidth = getDefaultWidth();
        Point areaSize = groupContainer.getParent().getSize();
        if (areaSize.x <= 0 || areaSize.y <= 0) {
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
            totalHeight = Math.min(defHeight, areaSize.y);
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
            totalWidth = Math.min(defWidth, areaSize.x);
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
        if (extraHeightSpace > 0 && extraWidthSpace > 0 && totalWidth > 0 && totalHeight > 0) {
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
            return new Point(Math.min(defWidth, areaSize.x), Math.min(defHeight, areaSize.y));
        }
    }

    @Override
    public String getDashboardId() {
        return dashboardConfig.getDashboardDescriptor().getId();
    }

    @Override
    public String getDashboardTitle() {
        return dashboardConfig.getDashboardDescriptor().getName();
    }

    @Override
    public String getDashboardDescription() {
        return dashboardConfig.getDescription();
    }

    @Override
    public DashboardViewType getDashboardType() {
        return dashboardConfig.getDashboardDescriptor().getDefaultViewType();
    }

    @Override
    public DashboardCalcType getDashboardCalcType() {
        return dashboardConfig.getDashboardDescriptor().getCalcType();
    }

    @Override
    public DashboardValueType getDashboardValueType() {
        return dashboardConfig.getDashboardDescriptor().getValueType();
    }

    @Override
    public DashboardFetchType getDashboardFetchType() {
        return dashboardConfig.getDashboardDescriptor().getFetchType();
    }

    @Override
    public int getDashboardMaxItems() {
        return dashboardConfig.getMaxItems();
    }

    @Override
    public long getDashboardMaxAge() {
        return dashboardConfig.getMaxAge();
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
        return dashboardConfig.getDashboardDescriptor().getQueries();
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
                lastUpdateTime = new Date();
            }
        });
    }

    @Override
    public void resetDashboardData() {
        UIUtils.asyncExec(() -> {
            if (renderer != null) {
                renderer.resetDashboardData(this, lastUpdateTime);
            }
        });
    }

    @Override
    public void updateDashboardView() {
        UIUtils.asyncExec(() -> {
            if (renderer != null) {
                renderer.updateDashboardView(this);
            }
        });
    }

    @Override
    public long getUpdatePeriod() {
        return dashboardConfig.getUpdatePeriod();
    }

    @Override
    public DashboardChartComposite getDashboardControl() {
        return dashboardControl;
    }


    public void moveViewFrom(DashboardItem item, boolean clearOriginal) {
        renderer.moveDashboardView(this, item, clearOriginal);
    }
}

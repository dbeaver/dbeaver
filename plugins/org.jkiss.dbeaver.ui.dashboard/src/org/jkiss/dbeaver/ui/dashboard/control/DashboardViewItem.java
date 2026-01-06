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

import org.apache.commons.jexl3.JexlExpression;
import org.eclipse.jface.action.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dashboard.DBDashboardMapQuery;
import org.jkiss.dbeaver.model.dashboard.DBDashboardQuery;
import org.jkiss.dbeaver.model.dashboard.DashboardIcons;
import org.jkiss.dbeaver.model.dashboard.data.DashboardDataset;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.DashboardUIConstants;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardUIRegistry;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jkiss.dbeaver.ui.dashboard.DashboardUIConstants.PARAM_CATALOG_PANEL_TOGGLE;

public class DashboardViewItem extends Composite implements DashboardItemContainer {

    public static final int DEFAULT_HEIGHT = 200;
    private final DashboardListControl groupContainer;
    private final DashboardItemViewSettings viewItemConfig;

    private Date lastUpdateTime;
    private DashboardRendererType curViewType;
    private DashboardItemRenderer renderer;
    private Composite dashboardControl;
    private final Label titleLabel;
    private final ToolBarManager titleToolbarManager;
    private final Composite chartComposite;
    private boolean autoUpdateEnabled;

    public DashboardViewItem(@NotNull DashboardListControl parent, @NotNull DashboardItemConfiguration item) {
        super(parent, SWT.DOUBLE_BUFFERED);
        this.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.groupContainer = parent;
        this.viewItemConfig = groupContainer.getView().getViewConfiguration().getItemConfig(item.getId());
        if (this.viewItemConfig == null) {
            throw new IllegalStateException("View item configuration not found for '" + item.getId() + "'");
        }

        GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        this.setLayout(layout);

        Color defFG = UIStyles.getDefaultTextForeground();
        Color defBG = UIStyles.getDefaultTextBackground();
        //this.setForeground(defFG);
        this.setBackground(defBG);

        {
            Composite titleComposite = new Composite(this, SWT.NONE);
            titleComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            titleComposite.setBackground(defBG);
            titleComposite.setForeground(defFG);
            GridLayout gridLayout = new GridLayout(3, false);
            gridLayout.marginHeight = 3;
            gridLayout.marginWidth = 3;
            titleComposite.setLayout(gridLayout);

            DBPImage icon = DashboardIcons.DASHBOARD;
            DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
            if (item.getDashboardProvider().isDatabaseRequired() && dataSourceContainer != null) {
                icon = dataSourceContainer.getDriver().getIcon();
            } else {
                icon = item.getDashboardProvider().getIcon();
            }
            Label titleIcon = new Label(titleComposite, SWT.NONE);
            titleIcon.setImage(DBeaverIcons.getImage(icon));

            titleLabel = new Label(titleComposite, SWT.NONE);
            titleLabel.setFont(parent.getTitleFont());

            updateChartLabel(item);
            titleLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            titleLabel.setBackground(defBG);

            titleToolbarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
            ToolBar toolBar = titleToolbarManager.createControl(titleComposite);
            toolBar.setBackground(defBG);

            this.createContextMenu(titleLabel);
        }

        chartComposite = new Composite(this, SWT.NONE);
        chartComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        chartComposite.setLayout(new FillLayout());

        createDashboardRenderer();

        groupContainer.addItem(this);
        addDisposeListener(e -> groupContainer.removeItem(this));

        this.addPaintListener(this::paintItem);

        this.autoUpdateEnabled = true;
    }

    private void updateChartLabel(@NotNull DashboardItemConfiguration item) {
        if (viewItemConfig == null) {
            titleLabel.setText(item.getId());
        } else {
            titleLabel.setText(evaluateChartLabel(item.getTitle()));
        }
        titleLabel.setToolTipText(CommonUtils.notEmpty(item.getDescription()));
    }

    private String evaluateChartLabel(String label) {
        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            return GeneralUtils.replaceVariables(label, dataSourceContainer.getVariablesResolver(true));
        }
        return label;
    }

    public DashboardListControl getGroupContainer() {
        return groupContainer;
    }

    private void createContextMenu(Control control) {
        MenuManager menuMgr = new MenuManager(null, getItemDescriptor().getId() + "_context_menu");
        menuMgr.addMenuListener(manager -> {
            fillDashboardContextMenu(menuMgr, false);
        });
        Menu menu = menuMgr.createContextMenu(this);
        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);
    }

    private void createDashboardRenderer() {
        try {
            curViewType = viewItemConfig.getViewType();
            renderer = curViewType.createRenderer();
            dashboardControl = renderer.createDashboard(chartComposite, this, groupContainer.getView(), computeSize(-1, -1));
            updateToolBarActions();
        } catch (DBException e) {
            // Something went wrong
            Text errorLabel = new Text(this, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
            errorLabel.setText(NLS.bind(UIDashboardMessages.dashboard_item_errorlabel_text, viewItemConfig.getItemId(), e.getMessage()));
            errorLabel.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));
        }

        initChartRenderer();
    }

    private void updateToolBarActions() {
        titleToolbarManager.removeAll();
        renderer.fillDashboardToolbar(this, titleToolbarManager, dashboardControl, viewItemConfig);
        titleToolbarManager.update(false);
    }

    private void initChartRenderer() {
        Control dbCanvas = dashboardControl instanceof DashboardViewCompositeControl chart ?
            chart.getDashboardControl() : null;
        if (dbCanvas == null) {
            return;
        }
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                groupContainer.setSelection(DashboardViewItem.this);
                //dbCanvas.forceFocus();
            }
        };
        dbCanvas.addMouseListener(mouseAdapter);
        this.addMouseListener(mouseAdapter);
        titleLabel.addMouseListener(mouseAdapter);

        dbCanvas.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                groupContainer.setSelection(DashboardViewItem.this);
                redraw();
            }

            @Override
            public void focusLost(FocusEvent e) {

            }
        });

        dbCanvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                groupContainer.handleKeyEvent(e);
            }
        });

        dashboardControl.addDisposeListener(e -> renderer.disposeDashboard(DashboardViewItem.this));
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    private void paintItem(PaintEvent e) {
        if (UIUtils.isInDialog(this)) {
            return;
        }
        Point itemSize = getSize();
        e.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
        if (groupContainer.getSelectedItem() == this) {
            e.gc.setLineWidth(2);
            e.gc.setLineStyle(SWT.LINE_SOLID);
        } else {
            e.gc.setLineWidth(1);
            e.gc.setLineStyle(SWT.LINE_SOLID);
            //e.gc.setLineDash(new int[] {10, 10});
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
        return (int) (viewItemConfig.getWidthRatio() * getDefaultHeight());
    }
    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        Point listAreaSize = groupContainer.getSize();
        if (groupContainer.getView().isSingleChartMode() || listAreaSize.x <= 0 || listAreaSize.y <= 0) {
            return super.computeSize(wHint, hHint, changed);
        }
        GridLayout listLayout = (GridLayout) groupContainer.getLayout();
        int listRowCount = groupContainer.getListRowCount();
        int listColumnCount = groupContainer.getListColumnCount();
        int width = (listAreaSize.x - listLayout.marginWidth * 2 - listLayout.horizontalSpacing * (listColumnCount - 1)) / listColumnCount;
        int height = (listAreaSize.y - listLayout.marginHeight * 2 - listLayout.verticalSpacing * (listRowCount - 1)) / listRowCount;
        return new Point(width, height);
/*
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
*/
    }

    public DashboardItemViewSettings getViewItemConfig() {
        return viewItemConfig;
    }

    @Override
    public DashboardItemConfiguration getItemDescriptor() {
        return viewItemConfig.getItemConfiguration();
    }

    @Override
    public DashboardItemViewSettings getItemConfiguration() {
        return viewItemConfig;
    }

    @Override
    public int getDashboardMaxItems() {
        return viewItemConfig.getMaxItems();
    }

    @Override
    public long getDashboardMaxAge() {
        return viewItemConfig.getMaxAge();
    }

    @Override
    public DBPProject getProject() {
        return groupContainer.getProject();
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
    public DBDashboardMapQuery getMapQuery() {
        DashboardItemConfiguration dashboard = viewItemConfig.getItemConfiguration();
        return dashboard == null ? null : dashboard.getMapQuery();
    }

    @Override
    public String[] getMapKeys() {
        DashboardItemConfiguration dashboard = viewItemConfig.getItemConfiguration();
        return dashboard == null ? null : dashboard.getMapKeys();
    }

    @Override
    public String[] getMapLabels() {
        DashboardItemConfiguration dashboard = viewItemConfig.getItemConfiguration();
        return dashboard == null ? null : dashboard.getMapLabels();
    }

    @Override
    public JexlExpression getMapFormula() {
        DashboardItemConfiguration dashboard = viewItemConfig.getItemConfiguration();
        return dashboard == null ? null : dashboard.getMapFormulaExpr();
    }

    @Override
    public List<? extends DBDashboardQuery> getQueryList() {
        DashboardItemConfiguration dashboard = viewItemConfig.getItemConfiguration();
        return dashboard == null ? null : dashboard.getQueries();
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
            boolean forceLayout = false;
            if (viewItemConfig.getViewType() != curViewType) {
                // Change view!
                if (dashboardControl != null) {
                    dashboardControl.dispose();
                    forceLayout = true;
                }
                createDashboardRenderer();
            } else if (renderer != null) {
                renderer.updateDashboardView(this);
            }
            if (forceLayout) {
                layout(true, true);
            }
        });
    }

    @Override
    public boolean isAutoUpdateEnabled() {
        return autoUpdateEnabled;
    }

    @Override
    public void disableAutoUpdate() {
        this.autoUpdateEnabled = false;
    }

    @Override
    public long getUpdatePeriod() {
        return viewItemConfig.getUpdatePeriod();
    }

    @Override
    public Composite getDashboardControl() {
        return dashboardControl;
    }


    public void moveViewFrom(DashboardViewItem item, boolean clearOriginal) {
        renderer.moveDashboardView(this, item, clearOriginal);
    }

    @Override
    public void fillDashboardContextMenu(
        @NotNull IMenuManager manager,
        boolean singleChartMode
    ) {
        if (!singleChartMode) {
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), DashboardUIConstants.CMD_VIEW_DASHBOARD));
            manager.add(new Separator());
        }
        List<DashboardRendererType> viewTypes = DashboardUIRegistry.getInstance().getSupportedViewTypes(getItemDescriptor().getDataType());
        if (!UIUtils.isInDialog(dashboardControl) && viewTypes.size() > 1) {
            MenuManager viewMenu = new MenuManager(UIDashboardMessages.dashboard_chart_composite_menu_manager_text);
            for (DashboardRendererType viewType : viewTypes) {
                Action changeViewAction = new Action(viewType.getTitle(), Action.AS_RADIO_BUTTON) {
                    @Override
                    public boolean isChecked() {
                        return getItemConfiguration().getViewType() == viewType;
                    }

                    @Override
                    public void runWithEvent(Event event) {
                        getViewItemConfig().setViewType(viewType);
                        getGroup().getView().saveChanges();
                        updateDashboardView();
                    }
                };
                if (viewType.getIcon() != null) {
                    changeViewAction.setImageDescriptor(DBeaverIcons.getImageDescriptor(viewType.getIcon()));
                }
                viewMenu.add(changeViewAction);
            }
            manager.add(viewMenu);
        }
        if (!singleChartMode) {
            manager.add(new Separator());
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), DashboardUIConstants.CMD_ADD_DASHBOARD));
            Map<String, Object> params = new HashMap<>();
            params.put(PARAM_CATALOG_PANEL_TOGGLE, "true");
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(),
                DashboardUIConstants.CMD_CATALOG_SHOW_DASHBOARD, params));
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), DashboardUIConstants.CMD_REMOVE_DASHBOARD));
            manager.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), DashboardUIConstants.CMD_REFRESH_CHART));
        }
        manager.add(new Separator());
    }

    @Override
    public void refreshInfo() {
        updateChartLabel(getItemDescriptor());
    }

    @Override
    public String toString() {
        DashboardItemConfiguration dashboard = viewItemConfig.getItemConfiguration();
        return dashboard == null ? viewItemConfig.getItemId() : dashboard.getName();
    }
}

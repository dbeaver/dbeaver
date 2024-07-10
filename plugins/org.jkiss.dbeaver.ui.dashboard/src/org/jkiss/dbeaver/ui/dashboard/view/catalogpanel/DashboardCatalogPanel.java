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
package org.jkiss.dbeaver.ui.dashboard.view.catalogpanel;

import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dashboard.DBDashboardFolder;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardProviderDescriptor;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistryListener;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRendererDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardUIRegistry;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardItemConfigurationTransfer;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.function.Function;

/**
 * Dashboard add dialog
 */
public abstract class DashboardCatalogPanel extends Composite implements DashboardRegistryListener {

    private static final Log log = Log.getLog(DashboardCatalogPanel.class);
    private final TreeViewer dashboardTable;

    private DashboardItemConfiguration selectedDashboard;

    @NotNull
    private final DBPProject project;
    @Nullable
    private final DBPDataSourceContainer dataSourceContainer;

    public DashboardCatalogPanel(
        @NotNull Composite parent,
        @NotNull DBPProject project,
        @Nullable DBPDataSourceContainer dataSourceContainer,
        @Nullable Function<DashboardItemConfiguration, Boolean> itemFilter,
        boolean isFlat) {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        if (isFlat) {
            layout.marginWidth = 0;
            layout.marginHeight = 0;
        }
        setLayout(layout);
        this.project = project;
        this.dataSourceContainer = dataSourceContainer;

        int style = SWT.FULL_SELECTION;
        if (!isFlat) {
            style |= SWT.BORDER;
        }
        dashboardTable = new TreeViewer(this, style);

        dashboardTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        Tree table = dashboardTable.getTree();
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 200;
        table.setLayoutData(gd);
        table.setHeaderVisible(true);
        UIUtils.createTreeColumn(table, SWT.LEFT, UIDashboardMessages.dialog_add_dashboard_column_name);
        UIUtils.createTreeColumn(table, SWT.LEFT, UIDashboardMessages.dialog_add_dashboard_column_description);

        dashboardTable.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                if (cell.getElement() instanceof DashboardProviderDescriptor dpd) {
                    if (cell.getColumnIndex() == 0) {
                        cell.setText(dpd.getName());
                        cell.setImage(DBeaverIcons.getImage(dpd.getIcon()));
                    } else {
                        cell.setText(CommonUtils.notEmpty(dpd.getDescription()));
                    }
                } else if (cell.getElement() instanceof DBDashboardFolder folder) {
                    if (cell.getColumnIndex() == 0) {
                        cell.setText(folder.getName());
                        DBPImage icon = folder.getIcon();
                        if (icon == null) {
                            icon = DBIcon.TREE_FOLDER;
                        }
                        cell.setImage(DBeaverIcons.getImage(icon));
                    } else {
                        cell.setText(CommonUtils.notEmpty(folder.getDescription()));
                    }
                } else if (cell.getElement() instanceof DashboardItemConfiguration dashboardDescriptor) {
                    if (cell.getColumnIndex() == 0) {
                        cell.setText(dashboardDescriptor.getName());
                        DBPImage icon = null;
                        if (dashboardDescriptor.isCustom()) {
                            icon = DBIcon.TYPE_OBJECT;
                        } else {
                            DashboardRendererDescriptor viewType = DashboardUIRegistry.getInstance().getViewType(dashboardDescriptor.getDashboardRenderer());
                            if (viewType != null) {
                                icon = viewType.getIcon();
                            }
                        }
                        if (icon != null) {
                            cell.setImage(DBeaverIcons.getImage(icon));
                        }

                    } else {
                        cell.setText(CommonUtils.notEmpty(dashboardDescriptor.getDescription()));
                    }
                }
            }
        });
        dashboardTable.addDoubleClickListener(event -> {
            if ((dashboardTable.getStructuredSelection().getFirstElement() instanceof DashboardItemConfiguration)) {
                handleChartSelectedFinal();
            }
        });
        dashboardTable.addSelectionChangedListener(event -> {
            ISelection selection = dashboardTable.getSelection();
            if (selection instanceof IStructuredSelection ss) {
                if (ss.getFirstElement() instanceof DashboardItemConfiguration dd) {
                    selectedDashboard = dd;
                } else {
                    selectedDashboard = null;
                }
            }
            handleChartSelected();
        });
        table.addPaintListener(e -> {
            if (table.getItemCount() == 0) {
                final String dbmsName = dataSourceContainer == null ?
                    project.getName() : dataSourceContainer.getDriver().getName();
                final String msg = NLS.bind(UIDashboardMessages.dialog_add_dashboard_message_no_more_dashboards_for, dbmsName);
                UIUtils.drawMessageOverControl(table, e, msg, 0);
            }
        });
        dashboardTable.setContentProvider(new DashboardCatalogPanelTreeContentProvider(dataSourceContainer, project,
            itemFilter));

        refreshInput();

        if (isFlat) {
            addDragAndDropSupport(table);
        }


        UIUtils.asyncExec(() -> UIUtils.packColumns(table, true, null));

        // Add listeners
        DashboardRegistry.getInstance().addListener(this);
        addDisposeListener(e -> DashboardRegistry.getInstance().removeListener(this));
    }

    private void refreshInput() {
        List<DashboardProviderDescriptor> dbProviders = DashboardRegistry.getInstance().getDashboardProviders(
            dataSourceContainer);

        dashboardTable.setInput(dbProviders);
        dashboardTable.expandToLevel(2);
    }

    private static void addDragAndDropSupport(Tree table) {
        final DragSource source = new DragSource(table, DND.DROP_MOVE);
        source.setTransfer(TextTransfer.getInstance(), DashboardItemConfigurationTransfer.INSTANCE);
        source.addDragListener (new DragSourceListener() {
            private TreeItem dragItem;
            private Image dragImage;
            @Override
            public void dragStart(DragSourceEvent event) {
                Point point = table.toControl(table.getDisplay().getCursorLocation());
                dragItem = table.getItem(point);

                if (dragItem == null || !(dragItem.getData() instanceof DashboardItemConfiguration)) {
                    return;
                }
                Rectangle columnBounds = dragItem.getBounds();
                if (dragImage != null) {
                    dragImage.dispose();
                    dragImage = null;
                }
                GC gc = new GC(table);
                dragImage = new Image(Display.getCurrent(), columnBounds.width, columnBounds.height);
                gc.copyArea(
                    dragImage,
                    columnBounds.x,
                    columnBounds.y);
                event.image = dragImage;
                gc.dispose();
            }

            @Override
            public void dragSetData (DragSourceEvent event) {
                if (dragItem.getData() instanceof DashboardItemConfiguration dashboardDescriptor &&
                    DashboardItemConfigurationTransfer.INSTANCE.isSupportedType(event.dataType)
                ) {
                    event.data = dashboardDescriptor;
                } else {
                    event.data = dragItem.getText();
                }
            }
            @Override
            public void dragFinished(DragSourceEvent event) {
                if (dragImage != null) {
                    dragImage.dispose();
                    dragImage = null;
                }
            }
        });
    }

    @NotNull
    public DBPProject getProject() {
        return project;
    }

    @Nullable
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public DashboardItemConfiguration getSelectedDashboard() {
        return selectedDashboard;
    }

    protected abstract void handleChartSelected();

    protected abstract void handleChartSelectedFinal();

    @Override
    public void handleItemCreate(@NotNull DashboardItemConfiguration item) {
        refreshInput();
    }

    @Override
    public void handleItemDelete(@NotNull DashboardItemConfiguration item) {
        refreshInput();
    }
}
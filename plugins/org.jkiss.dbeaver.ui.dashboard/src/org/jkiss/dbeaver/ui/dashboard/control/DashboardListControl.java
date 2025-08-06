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

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.DashboardUIConstants;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardItemConfigurationTransfer;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardItemTransfer;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardManagerDialog;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jkiss.dbeaver.ui.dashboard.DashboardUIConstants.PARAM_CATALOG_PANEL_TOGGLE;

public class DashboardListControl extends Composite implements DashboardGroupContainer {

    private static final int ITEM_SPACING = 5;

    private final IWorkbenchSite site;
    private final DashboardContainer viewContainer;
    private final List<DashboardViewItem> items = new ArrayList<>();
    private final Font boldFont;
    private DashboardViewItem selectedItem;
    private int listRowCount = 1;
    private int listColumnCount = 1;

    public DashboardListControl(IWorkbenchSite site, Composite parent, DashboardContainer viewContainer) {
        super(parent, SWT.DOUBLE_BUFFERED);

        this.site = site;
        this.viewContainer = viewContainer;

        Font normalFont = getFont();
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setHeight(fontData[0].getHeight() + 1);
        fontData[0].setStyle(SWT.BOLD);
        boldFont = new Font(normalFont.getDevice(), fontData[0]);

        addDisposeListener(e -> boldFont.dispose());

        this.setForeground(UIStyles.getDefaultTextForeground());
        this.setBackground(UIStyles.getDefaultTextBackground());

        GridLayout layout = new GridLayout(1, true);
        this.setLayout(layout);

        if (!UIUtils.isInDialog(parent)) {
            createIntroItem();
        }

        registerContextMenu();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                setSelection(null);
                setFocus();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyEvent(e);
            }
        });

        addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                computeGridSize();
                layout(true, true);
            }
        });

        addItemDropSupport();
    }

    private void addItemDropSupport() {
        final DropTarget target = new DropTarget(this, DND.DROP_MOVE);
        target.setTransfer(DashboardItemTransfer.INSTANCE, DashboardItemConfigurationTransfer.INSTANCE);
        target.addDropListener(new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragLeave(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void drop(DropTargetEvent event) {
                handleDragEvent(event);
                if (event.detail == DND.DROP_MOVE || event.detail == DND.DROP_COPY) {
                    if (event.data instanceof DashboardItemConfiguration itemConfiguration) {
                        addItem(itemConfiguration);
                    } else if (event.data instanceof DashboardViewItem item) {
                        addItem(item.getItemConfiguration().getDashboardItem());
                    }
                }
            }

            @Override
            public void dropAccept(DropTargetEvent event) {
                handleDragEvent(event);
            }

            private void handleDragEvent(DropTargetEvent event) {
                if (!isDropSupported(event)) {
                    event.detail = DND.DROP_NONE;
                } else {
                    if (event.detail == DND.DROP_NONE) {
                        event.detail = DND.DROP_MOVE;
                    }
                }
                event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
            }

            private boolean isDropSupported(DropTargetEvent event) {
                if (event.dataTypes == null) {
                    return false;
                }
                for (TransferData td : event.dataTypes) {
                    if (td.type == DashboardItemConfigurationTransfer.INSTANCE.getSupportedTypes()[0].type ||
                        td.type == DashboardItemTransfer.INSTANCE.getSupportedTypes()[0].type) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void createIntroItem() {
        Composite intro = UIUtils.createComposite(this, 1);
        Label titleLabel = new Label(intro, SWT.NONE);
        titleLabel.setFont(this.getTitleFont());
        titleLabel.setText("Customize your dashboard");
        String addCommandName = ActionUtils.findCommandName(DashboardUIConstants.CMD_ADD_DASHBOARD);
        UIUtils.createLink(intro,
            "<a>" + addCommandName + "</a> to this dashboard by drag-n-drop or double-click from the <a>catalog</a> or another dashboard.\n" +
                "You can also create new charts in the <a>Configuration</a> dialog.\n" +
                "For further information, please refer to the <a>documentation</a>.",
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (CommonUtils.equalObjects(addCommandName, e.text)) {
                        ActionUtils.runCommand(DashboardUIConstants.CMD_ADD_DASHBOARD, site);
                    } else if (CommonUtils.equalObjects("catalog", e.text)) {
                        viewContainer.showChartCatalog();
                    }  else if (CommonUtils.equalObjects("documentation", e.text)) {
                        ShellUtils.launchProgram(HelpUtils.getHelpExternalReference(DashboardUIConstants.DASHBOARD_DOCUMENTATION_URL_SUFFIX));
                    }else {
                        new DashboardManagerDialog(UIUtils.getActiveWorkbenchShell()).open();
                    }
                }
            });
    }

    public int getListRowCount() {
        return listRowCount;
    }

    public int getListColumnCount() {
        return listColumnCount;
    }

    private void computeGridSize() {
        int totalItems = items.size();
        Point listAreaSize = getSize();
        if (listAreaSize.x <= 0 || listAreaSize.y <= 0 || items.isEmpty()) {
            return;
        }

        listRowCount = 1;
        // Calculate pack efficiency for different number of rows
        for (int rowCount = 1; rowCount < 50; rowCount++) {
            int itemsPerRow = (int) Math.ceil((float) totalItems / rowCount);

            int itemWidth = listAreaSize.x / itemsPerRow;
            int itemHeight = itemWidth / 3;

            int totalHeight = itemHeight * rowCount;
            if (totalHeight > listAreaSize.y) {
                // Too many
                if (rowCount > 1) {
                    listRowCount = rowCount - 1;
                }
                break;
            }
        }
        listColumnCount = (int) Math.ceil((float) totalItems / listRowCount);
        ((GridLayout) getLayout()).numColumns = listColumnCount;
    }

    void handleKeyEvent(KeyEvent e) {
        switch (e.keyCode) {
            case SWT.CR:
                ActionUtils.runCommand(DashboardUIConstants.CMD_VIEW_DASHBOARD, DashboardListControl.this.site);
                break;
            case SWT.DEL:
                ActionUtils.runCommand(DashboardUIConstants.CMD_REMOVE_DASHBOARD, DashboardListControl.this.site);
                break;
            case SWT.INSERT:
                ActionUtils.runCommand(DashboardUIConstants.CMD_ADD_DASHBOARD, DashboardListControl.this.site);
                break;
            case SWT.ARROW_LEFT:
            case SWT.ARROW_UP:
                moveSelection(-1);
                break;
            case SWT.ARROW_RIGHT:
            case SWT.ARROW_DOWN:
                moveSelection(1);
                break;
        }
    }

    private void moveSelection(int delta) {
        if (items.isEmpty()) {
            return;
        }
        if (selectedItem == null) {
            setSelection(items.get(0));
        } else {
            int curIndex = items.indexOf(selectedItem);
            curIndex += delta;
            if (curIndex < 0) {
                curIndex = items.size() - 1;
            } else if (curIndex >= items.size()) {
                curIndex = 0;
            }
            DashboardViewItem newSelection = items.get(curIndex);
            newSelection.getDashboardControl().setFocus();
            setSelection(newSelection);
            newSelection.redraw();
        }
    }

    private void registerContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.add(ActionUtils.makeCommandContribution(site, DashboardUIConstants.CMD_ADD_DASHBOARD));
        menuMgr.add(ActionUtils.makeCommandContribution(site, DashboardUIConstants.CMD_REFRESH_CHART));
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_CATALOG_PANEL_TOGGLE, "true");
        menuMgr.add(ActionUtils.makeCommandContribution(site, DashboardUIConstants.CMD_CATALOG_SHOW_DASHBOARD, params));
        setMenu(menuMgr.createContextMenu(this));

        addDisposeListener(e -> menuMgr.dispose());
    }

    DBPProject getProject() {
        return viewContainer.getViewConfiguration().getProject();
    }

    DBPDataSourceContainer getDataSourceContainer() {
        return viewContainer.getDataSourceContainer();
    }

    @NotNull
    @Override
    public DashboardContainer getView() {
        return viewContainer;
    }

    @NotNull
    public List<DashboardViewItem> getItems() {
        return items;
    }

    @Override
    public void removeItem(@NotNull DashboardItemContainer container) {
        DashboardViewItem item = (DashboardViewItem) container;
        item.dispose();
        computeGridSize();

        if (this.items.isEmpty()) {
            createIntroItem();
            viewContainer.showChartCatalog();
        }

        layout(true, true);
        viewContainer.getViewConfiguration().removeItem(item.getItemDescriptor().getId());
        viewContainer.saveChanges();
    }

    @Override
    public void addItem(@NotNull DashboardItemConfiguration dashboard) {
        if (viewContainer.getViewConfiguration().getItemConfig(dashboard.getId()) != null) {
            // Duplicate
            return;
        }

        if (this.items.isEmpty()) {
            UIUtils.disposeChildControls(this);
        }

        viewContainer.getViewConfiguration().readDashboardItemConfiguration(dashboard);
        new DashboardViewItem(this, dashboard);
        viewContainer.saveChanges();
        computeGridSize();
        layout(true, true);
    }

    @Override
    public void selectItem(DashboardItemContainer item) {
        setSelection((DashboardViewItem) item);
        if (item != null) {
            item.getDashboardControl().setFocus();
        }
    }

    void createDefaultDashboards() {
        if (viewContainer.getViewConfiguration().isInitDefaultCharts() && viewContainer.getDataSourceContainer() != null) {
            List<DashboardItemConfiguration> dashboards = DashboardRegistry.getInstance().getDashboardItems(
                null, viewContainer.getDataSourceContainer(), true);
            for (DashboardItemConfiguration dd : dashboards) {
                loadItem(dd);
            }
        }
    }

    public void createDashboardsFromConfiguration() {
        for (DashboardItemViewSettings itemConfig : new ArrayList<>(viewContainer.getViewConfiguration().getDashboardItemConfigs())) {
            DashboardItemConfiguration dashboard = itemConfig.getItemConfiguration();
            if (dashboard != null) {
                loadItem(dashboard);
            } else {
                // Bad item - remove it
                viewContainer.getViewConfiguration().removeItemConfiguration(itemConfig);
            }
        }
    }


    private void loadItem(DashboardItemConfiguration dashboard) {
        if (this.items.isEmpty()) {
            UIUtils.disposeChildControls(this);
        }
        viewContainer.getViewConfiguration().readDashboardItemConfiguration(dashboard);
        DashboardViewItem item = new DashboardViewItem(this, dashboard);
    }

    void addItem(DashboardViewItem item) {
        addItemMoveSupport(item);
        this.items.add(item);
    }

    void removeItem(DashboardViewItem item) {
        this.items.remove(item);
    }

    public int getItemSpacing() {
        return ITEM_SPACING;
    }

    public Font getTitleFont() {
        return boldFont;
    }

    public DashboardViewItem getSelectedItem() {
        return selectedItem;
    }

    public void setSelection(DashboardViewItem selection) {
        DashboardViewItem oldSelection = this.selectedItem;
        if (oldSelection == selection) {
            return;
        }
        this.selectedItem = selection;
        if (selection != null && !selection.isDisposed()) {
            selection.redraw();
        }
        if (oldSelection != null && !oldSelection.isDisposed()) {
            oldSelection.redraw();
        }
        viewContainer.updateSelection();
    }

    /**
     * Clear dashboards view
     */
    public void clear() {
        selectedItem = null;

        for (DashboardViewItem item : List.copyOf(items)) {
            item.dispose();
        }

        items.clear();

        getView().getViewConfiguration().clearItems();
    }

    /////////////////////////////////////////////////////////////////////////////////
    // DnD
    /////////////////////////////////////////////////////////////////////////////////

    private void addItemMoveSupport(DashboardViewItem item) {
        Label dndControl = item.getTitleLabel();
        final int operations = DND.DROP_MOVE | DND.DROP_COPY;// | DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_DEFAULT;

        final DragSource source = new DragSource(dndControl, operations);
        source.setTransfer(DashboardItemTransfer.INSTANCE);
        source.addDragListener(new DragSourceListener() {

            private Image dragImage;
            private long lastDragEndTime;

            @Override
            public void dragStart(DragSourceEvent event) {
                if (selectedItem == null || lastDragEndTime > 0 && System.currentTimeMillis() - lastDragEndTime < 100) {
                    event.doit = false;
                } else {
                    Rectangle columnBounds = selectedItem.getBounds();
                    if (dragImage != null) {
                        dragImage.dispose();
                        dragImage = null;
                    }
                    GC gc = new GC(DashboardListControl.this);
                    dragImage = new Image(Display.getCurrent(), columnBounds.width, columnBounds.height);
                    gc.copyArea(
                        dragImage,
                        columnBounds.x,
                        columnBounds.y);
                    event.image = dragImage;
                    gc.dispose();
                }
            }

            @Override
            public void dragSetData(DragSourceEvent event) {
                if (selectedItem != null) {
                    if (DashboardItemTransfer.INSTANCE.isSupportedType(event.dataType)) {
                        event.data = selectedItem;
                    }
                }
            }

            @Override
            public void dragFinished(DragSourceEvent event) {
                if (dragImage != null) {
                    UIUtils.dispose(dragImage);
                    dragImage = null;
                }
                lastDragEndTime = System.currentTimeMillis();
            }
        });

        addItemViewDropSupport(dndControl, operations);
        addItemViewDropSupport(item.getDashboardControl(), operations);
    }

    private void addItemViewDropSupport(Control dndControl, int operations) {
        DropTarget dropTarget = new DropTarget(dndControl, operations);
        dropTarget.setTransfer(DashboardItemTransfer.INSTANCE, DashboardItemConfigurationTransfer.INSTANCE);
        dropTarget.addDropListener(new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragLeave(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void drop(DropTargetEvent event) {
                handleDragEvent(event);
                if (event.detail == DND.DROP_MOVE) {
                    if (event.data instanceof DashboardViewItem) {
                        UIUtils.asyncExec(() -> moveDashboard(event));
                    } else if (event.data instanceof DashboardItemConfiguration item) {
                        UIUtils.asyncExec(() -> addItem(item));
                    }
                }
            }

            @Override
            public void dropAccept(DropTargetEvent event) {
                handleDragEvent(event);
            }

            private void handleDragEvent(DropTargetEvent event) {
                if (!isDropSupported(event)) {
                    event.detail = DND.DROP_NONE;
                } else {
                    event.detail = DND.DROP_MOVE;
                }
                event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
            }

            private boolean isDropSupported(DropTargetEvent event) {
                DashboardViewItem overItem = getOverItem(event);
                if (selectedItem == null || overItem == null) {
                    return false;
                }
                return true;
            }

            private void moveDashboard(DropTargetEvent event) {
                DashboardViewItem overItem = getOverItem(event);
                if (selectedItem == null || overItem == null || selectedItem == overItem) {
                    return;
                }

                List<DashboardViewItem> newList = new ArrayList<>(items);
                int newIndex = newList.indexOf(overItem);
                newList.remove(selectedItem);
                newList.add(newIndex, selectedItem);

                DashboardConfiguration viewConfiguration = viewContainer.getViewConfiguration();

                // Re-create  items
                DashboardListControl.this.setRedraw(false);
                try {
                    selectedItem = null;
                    items.clear();

                    for (int i = 0; i < newList.size(); i++) {
                        DashboardViewItem oldItem = newList.get(i);
                        DashboardViewItem newItem = new DashboardViewItem(DashboardListControl.this, oldItem.getItemDescriptor());
                        DashboardItemViewSettings dashboardConfig = viewConfiguration.getItemConfig(newItem.getItemDescriptor().getId());
                        dashboardConfig.setIndex(i);
                        newItem.moveViewFrom(oldItem, true);
                    }

                    // Dispose old items
                    for (DashboardViewItem item : newList) {
                        item.dispose();
                    }
                } finally {
                    DashboardListControl.this.layout(true, true);
                    DashboardListControl.this.setRedraw(true);
                }

                viewContainer.saveChanges();
            }

            private DashboardViewItem getOverItem(DropTargetEvent event) {
                Object source = event.getSource();
                if (source instanceof DropTarget) {
                    Control control = ((DropTarget) source).getControl();
                    for (Composite parent = control.getParent(); parent != null; parent = parent.getParent()) {
                        if (parent instanceof DashboardViewItem) {
                            return (DashboardViewItem) parent;
                        }
                    }
                }
                return null;
            }

        });
    }

    public void showItem(DashboardItemContainer item) {

    }

}

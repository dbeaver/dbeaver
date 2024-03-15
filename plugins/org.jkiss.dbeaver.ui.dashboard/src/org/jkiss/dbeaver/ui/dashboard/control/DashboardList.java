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
import org.jkiss.dbeaver.model.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dnd.LocalObjectTransfer;

import java.util.ArrayList;
import java.util.List;

public class DashboardList extends Composite implements DashboardGroupContainer {

    private static final int ITEM_SPACING = 5;

    private final IWorkbenchSite site;
    private final DashboardViewContainer viewContainer;
    private final List<DBDashboardItem> items = new ArrayList<>();
    private final Font boldFont;
    private DBDashboardItem selectedItem;
    private int listRowCount = 1;
    private int listColumnCount = 1;

    public DashboardList(IWorkbenchSite site, Composite parent, DashboardViewContainer viewContainer) {
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
            int itemsPerRow = (int) Math.ceil((float)totalItems / rowCount);

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
        listColumnCount = (int)Math.ceil((float)totalItems / listRowCount);
        ((GridLayout)getLayout()).numColumns = listColumnCount;
    }

    void handleKeyEvent(KeyEvent e) {
        switch (e.keyCode) {
            case SWT.CR:
                ActionUtils.runCommand(DashboardUIConstants.CMD_VIEW_DASHBOARD, DashboardList.this.site);
                break;
            case SWT.DEL:
                ActionUtils.runCommand(DashboardUIConstants.CMD_REMOVE_DASHBOARD, DashboardList.this.site);
                break;
            case SWT.INSERT:
                ActionUtils.runCommand(DashboardUIConstants.CMD_ADD_DASHBOARD, DashboardList.this.site);
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
            DBDashboardItem newSelection = items.get(curIndex);
            newSelection.getDashboardControl().setFocus();
            setSelection(newSelection);
            newSelection.redraw();
        }
    }

    private void registerContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.add(ActionUtils.makeCommandContribution(site, DashboardUIConstants.CMD_ADD_DASHBOARD));
        menuMgr.add(ActionUtils.makeCommandContribution(site, DashboardUIConstants.CMD_RESET_DASHBOARD));
        setMenu(menuMgr.createContextMenu(this));

        addDisposeListener(e -> menuMgr.dispose());
    }

    DBPDataSourceContainer getDataSourceContainer() {
        return viewContainer.getDataSourceContainer();
    }

    @NotNull
    @Override
    public DashboardViewContainer getView() {
        return viewContainer;
    }

    @NotNull
    public List<DBDashboardItem> getItems() {
        return items;
    }

    @Override
    public void removeItem(@NotNull DBDashboardContainer container) {
        DBDashboardItem item = (DBDashboardItem) container;
        item.dispose();
        layout(true, true);
        viewContainer.getViewConfiguration().removeDashboard(item.getDashboard().getId());
        viewContainer.getViewConfiguration().saveSettings();
    }

    @Override
    public void addItem(@NotNull DashboardDescriptor dashboard) {
        viewContainer.getViewConfiguration().readDashboardConfiguration(dashboard);
        new DBDashboardItem(this, dashboard);
        viewContainer.getViewConfiguration().saveSettings();
        layout(true, true);
    }

    @Override
    public void selectItem(DBDashboardContainer item) {
        setSelection((DBDashboardItem) item);
        item.getDashboardControl().setFocus();
    }

    void createDefaultDashboards() {
        List<DashboardDescriptor> dashboards = DashboardRegistry.getInstance().getDashboards(
            null, viewContainer.getDataSourceContainer(), true);
        for (DashboardDescriptor dd : dashboards) {
            addDashboard(dd);
        }
    }

    public void createDashboardsFromConfiguration() {
        for (DashboardItemViewConfiguration itemConfig : new ArrayList<>(viewContainer.getViewConfiguration().getDashboardItemConfigs())) {
            DashboardDescriptor dashboard = itemConfig.getDashboardDescriptor();
            if (dashboard != null) {
                addDashboard(dashboard);
            } else {
                viewContainer.getViewConfiguration().readDashboardConfiguration(itemConfig);
            }
        }
    }


    private void addDashboard(DashboardDescriptor dashboard) {
        viewContainer.getViewConfiguration().readDashboardConfiguration(dashboard);
        DBDashboardItem item = new DBDashboardItem(this, dashboard);
    }

    void addItem(DBDashboardItem item) {
        addDragAndDropSupport(item);

        this.items.add(item);
    }

    void removeItem(DBDashboardItem item) {
        this.items.remove(item);
    }

    public int getItemSpacing() {
        return ITEM_SPACING;
    }

    public Font getTitleFont() {
        return boldFont;
    }

    public DBDashboardItem getSelectedItem() {
        return selectedItem;
    }

    public void setSelection(DBDashboardItem selection) {
        DBDashboardItem oldSelection = this.selectedItem;
        if (oldSelection == selection) {
            return;
        }
        this.selectedItem = selection;
        if (oldSelection != null && !oldSelection.isDisposed()) {
            oldSelection.redraw();
        }
    }

    /**
     * Clear dashboards view
     */
    public void clear() {
        selectedItem = null;
                
        for (DBDashboardItem item : List.copyOf(items)) {
            item.dispose();
        }
        
        items.clear();

        getView().getViewConfiguration().clearDashboards();
    }

    /////////////////////////////////////////////////////////////////////////////////
    // DnD
    /////////////////////////////////////////////////////////////////////////////////

    private void addDragAndDropSupport(DBDashboardItem item)
    {
        Label dndControl = item.getTitleLabel();
        final int operations = DND.DROP_MOVE | DND.DROP_COPY;// | DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_DEFAULT;

        final DragSource source = new DragSource(dndControl, operations);
        source.setTransfer(DashboardTransfer.INSTANCE);
        source.addDragListener (new DragSourceListener() {

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
                    GC gc = new GC(DashboardList.this);
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
            public void dragSetData (DragSourceEvent event) {
                if (selectedItem != null) {
                    if (DashboardTransfer.INSTANCE.isSupportedType(event.dataType)) {
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

        addControlDropTarget(dndControl, operations);
        addControlDropTarget(item.getDashboardControl(), operations);
    }

    private void addControlDropTarget(Control dndControl, int operations) {
        DropTarget dropTarget = new DropTarget(dndControl, operations);
        dropTarget.setTransfer(DashboardTransfer.INSTANCE, TextTransfer.getInstance());
        dropTarget.addDropListener(new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragLeave(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void drop(DropTargetEvent event)
            {
                handleDragEvent(event);
                if (event.detail == DND.DROP_MOVE) {
                    UIUtils.asyncExec(() -> moveDashboard(event));
                }
            }

            @Override
            public void dropAccept(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            private void handleDragEvent(DropTargetEvent event)
            {
                if (!isDropSupported(event)) {
                    event.detail = DND.DROP_NONE;
                } else {
                    event.detail = DND.DROP_MOVE;
                }
                event.feedback = DND.FEEDBACK_SELECT;
            }

            private boolean isDropSupported(DropTargetEvent event)
            {
                DBDashboardItem overItem = getOverItem(event);
                if (selectedItem == null || overItem == null) {
                    return false;
                }
                return overItem != selectedItem;
            }

            private void moveDashboard(DropTargetEvent event)
            {
                DBDashboardItem overItem = getOverItem(event);
                if (selectedItem == null || overItem == null || selectedItem == overItem) {
                    return;
                }

                List<DBDashboardItem> newList = new ArrayList<>(items);
                int newIndex = newList.indexOf(overItem);
                newList.remove(selectedItem);
                newList.add(newIndex, selectedItem);

                DashboardViewConfiguration viewConfiguration = viewContainer.getViewConfiguration();

                // Re-create  items
                DashboardList.this.setRedraw(false);
                try {
                    selectedItem = null;
                    items.clear();

                    for (int i = 0; i < newList.size(); i++) {
                        DBDashboardItem oldItem = newList.get(i);
                        DBDashboardItem newItem = new DBDashboardItem(DashboardList.this, oldItem.getDashboard());
                        DashboardItemViewConfiguration dashboardConfig = viewConfiguration.getDashboardConfig(newItem.getDashboard().getId());
                        dashboardConfig.setIndex(i);
                        newItem.moveViewFrom(oldItem, true);
                    }

                    // Dispose old items
                    for (DBDashboardItem item : newList) {
                        item.dispose();
                    }
                } finally {
                    DashboardList.this.layout(true, true);
                    DashboardList.this.setRedraw(true);
                }

                viewConfiguration.saveSettings();
            }

            private DBDashboardItem getOverItem(DropTargetEvent event) {
                Object source = event.getSource();
                if (source instanceof DropTarget) {
                    Control control = ((DropTarget) source).getControl();
                    for (Composite parent = control.getParent(); parent != null; parent = parent.getParent()) {
                        if (parent instanceof DBDashboardItem) {
                            return (DBDashboardItem) parent;
                        }
                    }
                }
                return null;
            }

        });
    }

    public void showItem(DBDashboardContainer item) {

    }

    public final static class DashboardTransfer extends LocalObjectTransfer<List<Object>> {

        public static final DashboardTransfer INSTANCE = new DashboardTransfer();
        private static final String TYPE_NAME = "DashboardTransfer.Item Transfer" + System.currentTimeMillis() + ":" + INSTANCE.hashCode();//$NON-NLS-1$
        private static final int TYPEID = registerType(TYPE_NAME);

        private DashboardTransfer() {
        }

        @Override
        protected int[] getTypeIds() {
            return new int[] { TYPEID };
        }

        @Override
        protected String[] getTypeNames() {
            return new String[] { TYPE_NAME };
        }

    }

}

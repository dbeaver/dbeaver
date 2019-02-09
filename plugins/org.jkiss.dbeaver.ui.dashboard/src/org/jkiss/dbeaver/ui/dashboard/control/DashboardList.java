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
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.dnd.LocalObjectTransfer;

import java.util.ArrayList;
import java.util.List;

public class DashboardList extends Composite implements DashboardGroupContainer {

    private static final int ITEM_SPACING = 5;

    private IWorkbenchSite site;
    private DashboardViewContainer viewContainer;
    private List<DashboardItem> items = new ArrayList<>();
    private final Font boldFont;
    private DashboardItem selectedItem;

    public DashboardList(IWorkbenchSite site, Composite parent, DashboardViewContainer viewContainer) {
        super(parent, SWT.DOUBLE_BUFFERED);

        this.site = site;
        this.viewContainer = viewContainer;

        Font normalFont = getFont();
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setHeight(fontData[0].getHeight() + 1);
        fontData[0].setStyle(SWT.BOLD);
        boldFont = new Font(normalFont.getDevice(), fontData[0]);

        addDisposeListener(e -> {
            boldFont.dispose();
        });

        RowLayout layout = new RowLayout();
        layout.spacing = getItemSpacing();
        layout.pack = false;
        layout.wrap = true;
        layout.justify = false;
        this.setLayout(layout);

        registerContextMenu();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                setSelection(null);
                setFocus();
            }
        });
    }

    private void registerContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.add(ActionUtils.makeCommandContribution(site, DashboardConstants.CMD_ADD_DASHBOARD));
        menuMgr.add(ActionUtils.makeCommandContribution(site, DashboardConstants.CMD_RESET_DASHBOARD));
        setMenu(menuMgr.createContextMenu(this));

        addDisposeListener(e -> menuMgr.dispose());
    }

    DBPDataSourceContainer getDataSourceContainer() {
        return viewContainer.getDataSourceContainer();
    }

    @Override
    public DashboardViewContainer getView() {
        return viewContainer;
    }

    public List<DashboardItem> getItems() {
        return items;
    }

    void createDefaultDashboards() {

        List<DashboardDescriptor> dashboards = DashboardRegistry.getInstance().getDashboards(
            viewContainer.getDataSourceContainer(), true);
        for (DashboardDescriptor dd : dashboards) {
            addDashboard(dd);
        }
    }

    private void addDashboard(DashboardDescriptor dashboard) {
        viewContainer.getViewConfiguration().readDashboardConfiguration(dashboard);
        DashboardItem item = new DashboardItem(this, dashboard.getId());
    }

    void addItem(DashboardItem item) {
        addDragAndDropSupport(item);

        this.items.add(item);
    }

    void removeItem(DashboardItem item) {
        this.items.remove(item);
    }

    public int getItemSpacing() {
        return ITEM_SPACING;
    }

    public Font getTitleFont() {
        return boldFont;
    }

    public DashboardItem getSelectedItem() {
        return selectedItem;
    }

    public void setSelection(DashboardItem selection) {
        DashboardItem oldSelection = this.selectedItem;
        this.selectedItem = selection;
        if (oldSelection != null) {
            oldSelection.redraw();
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    // DnD
    /////////////////////////////////////////////////////////////////////////////////

    private void addDragAndDropSupport(DashboardItem item)
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
                    moveDashboard(event);
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
                DashboardItem overItem = getOverItem(event);
                if (selectedItem == null || overItem == null) {
                    return false;
                }
                return overItem != selectedItem;
            }

            private void moveDashboard(DropTargetEvent event)
            {
                DashboardItem overItem = getOverItem(event);
                if (selectedItem == null || overItem == null || selectedItem == overItem) {
                    return;
                }

                List<DashboardItem> newList = new ArrayList<>(items);
                int newIndex = newList.indexOf(overItem);
                newList.remove(selectedItem);
                newList.add(newIndex, selectedItem);

                DashboardViewConfiguration viewConfiguration = viewContainer.getViewConfiguration();
                //viewConfiguration.

                // Re-create  items
                DashboardList.this.setRedraw(false);
                try {
                    for (DashboardItem item : items.toArray(new DashboardItem[0])) {
                        item.dispose();
                    }

                    for (DashboardItem oldItem : newList) {
                        DashboardItem newItem = new DashboardItem(DashboardList.this, oldItem.getDashboardId());
                        newItem.copyFrom(oldItem);
                    }
                } finally {
                    DashboardList.this.layout(true, true);
                    DashboardList.this.setRedraw(true);
                }

                viewConfiguration.saveSettings();

/*
                GridColumn overColumn = getOverItem(event);
                if (draggingColumn == null || draggingColumn == overColumn) {
                    return;
                }
                IGridController gridController = getGridController();
                if (gridController != null) {
                    IGridController.DropLocation location;// = IGridController.DropLocation.SWAP;

                    Point dropPoint = getDisplay().map(null, LightGrid.this, new Point(event.x, event.y));
                    Rectangle columnBounds = overColumn.getBounds();
                    if (dropPoint.x > columnBounds.x + columnBounds.width / 2) {
                        location = IGridController.DropLocation.DROP_AFTER;
                    } else {
                        location = IGridController.DropLocation.DROP_BEFORE;
                    }
                    gridController.moveColumn(draggingColumn.getElement(), overColumn.getElement(), location);
                }
                draggingColumn = null;
*/
            }

            private DashboardItem getOverItem(DropTargetEvent event) {
                Object source = event.getSource();
                if (source instanceof DropTarget) {
                    Control control = ((DropTarget) source).getControl();
                    for (Composite parent = control.getParent(); parent != null; parent = parent.getParent()) {
                        if (parent instanceof DashboardItem) {
                            return (DashboardItem) parent;
                        }
                    }
                }
                return null;
            }

        });
    }

    public void showItem(DashboardContainer item) {

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

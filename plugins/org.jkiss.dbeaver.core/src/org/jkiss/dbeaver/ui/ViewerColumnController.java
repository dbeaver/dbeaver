/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.text.Collator;
import java.util.*;
import java.util.List;

/**
 * Tree/table viewer column controller
 */
public class ViewerColumnController {

    static final Log log = Log.getLog(ViewerColumnController.class);

    private static final String DATA_KEY = ViewerColumnController.class.getSimpleName();

    private final String configId;
    private final ColumnViewer viewer;
    private final List<ColumnInfo> columns = new ArrayList<>();
    private boolean clickOnHeader;

    public static ViewerColumnController getFromControl(Control control)
    {
        return (ViewerColumnController)control.getData(DATA_KEY);
    }

    public ViewerColumnController(String id, ColumnViewer viewer)
    {
        this.configId = id + ".columns";
        this.viewer = viewer;
        final Control control = this.viewer.getControl();
        control.setData(DATA_KEY, this);
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                saveColumnConfig();
            }
        });

        if (control instanceof Tree || control instanceof Table) {
            control.addListener(SWT.MenuDetect, new Listener() {
                @Override
                public void handleEvent(Event event)
                {
                    Point pt = control.getDisplay().map(null, control, new Point(event.x, event.y));
                    Rectangle clientArea = ((Composite)control).getClientArea();
                    if (control instanceof Tree) {
                        clickOnHeader = clientArea.y <= pt.y && pt.y < (clientArea.y + ((Tree) control).getHeaderHeight());
                    } else {
                        clickOnHeader = clientArea.y <= pt.y && pt.y < (clientArea.y + ((Table) control).getHeaderHeight());
                    }
                }
            });
        }
    }

    public boolean isClickOnHeader()
    {
        return clickOnHeader;
    }

    public void fillConfigMenu(IMenuManager menuManager)
    {
        menuManager.add(new Action("Configure columns ...") {
            @Override
            public void run()
            {
                configureColumns();
            }
        });
    }

    public void addColumn(String name, String description, int style, boolean defaultVisible, boolean required, CellLabelProvider labelProvider)
    {
        columns.add(
            new ColumnInfo(name, description, style, defaultVisible, required, labelProvider, columns.size()));
    }

    public void createColumns()
    {
        try {
            readColumnsConfiguration();
        } catch (Exception e) {
            // Possibly incompatible format from previous version
            log.warn("Failed to load configuration for '" + this.configId + "'", e);
        }
        recreateColumns();
    }

    private void recreateColumns()
    {
        boolean needRefresh = false;
        for (ColumnInfo columnInfo : columns) {
            if (columnInfo.column != null) {
                columnInfo.column.dispose();
                columnInfo.column = null;
                needRefresh = true;
            }
        }
        createVisibleColumns();
        boolean allSized = true;
        for (ColumnInfo columnInfo : getVisibleColumns()) {
            if (columnInfo.width <= 0) {
                allSized = false;
                break;
            }
        }
        if (!allSized) {
            repackColumns();
            viewer.getControl().addControlListener(new ControlAdapter() {
                @Override
                public void controlResized(ControlEvent e)
                {
                    viewer.getControl().removeControlListener(this);
                    repackColumns();
                }
            });
        }
        if (needRefresh) {
            viewer.refresh();
        }
    }

    public void repackColumns()
    {
        if (viewer instanceof TreeViewer) {
            float[] ratios = null;
            if (((TreeViewer) viewer).getTree().getColumnCount() == 2) {
                ratios = new float[]{0.6f, 0.4f};
            }
            UIUtils.packColumns(((TreeViewer) viewer).getTree(), true, ratios);
        } else if (viewer instanceof TableViewer) {
            UIUtils.packColumns(((TableViewer)viewer).getTable());
        }
    }

    public void sortByColumn(int index, int direction) {
        final ColumnInfo columnInfo = columns.get(index);
        columnInfo.sortListener.sortViewer(columnInfo.column, direction);
    }

    private void createVisibleColumns()
    {
        boolean hasLazyColumns = false;
        for (final ColumnInfo columnInfo : getVisibleColumns()) {
            final Item colItem;
            if (viewer instanceof TreeViewer) {
                final TreeViewerColumn item = new TreeViewerColumn((TreeViewer) viewer, columnInfo.style);
                final TreeColumn column = item.getColumn();
                colItem = column;
                column.setText(columnInfo.name);
                column.setMoveable(true);
                column.setWidth(columnInfo.width);
                if (!CommonUtils.isEmpty(columnInfo.description)) {
                    column.setToolTipText(columnInfo.description);
                }
                item.setLabelProvider(columnInfo.labelProvider);
                column.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e)
                    {
                        columnInfo.width = column.getWidth();
                        saveColumnConfig();
                    }
                });
                columnInfo.column = column;
            } else if (viewer instanceof TableViewer) {
                final TableViewerColumn item = new TableViewerColumn((TableViewer) viewer, columnInfo.style);
                final TableColumn column = item.getColumn();
                colItem = column;
                column.setText(columnInfo.name);
                column.setMoveable(true);
                column.setWidth(columnInfo.width);
                if (!CommonUtils.isEmpty(columnInfo.description)) {
                    column.setToolTipText(columnInfo.description);
                }
                item.setLabelProvider(columnInfo.labelProvider);
                column.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e)
                    {
                        columnInfo.width = column.getWidth();
                        saveColumnConfig();
                    }
                });
                columnInfo.column = column;
            } else {
                continue;
            }
            colItem.setData(columnInfo);
            if (columnInfo.labelProvider instanceof ILazyLabelProvider) {
                hasLazyColumns = true;
            } else if (columnInfo.labelProvider instanceof ILabelProvider) {
                columnInfo.sortListener = new SortListener(columnInfo);
                columnInfo.column.addListener(SWT.Selection, columnInfo.sortListener);
            }
        }
        if (hasLazyColumns) {
            viewer.getControl().addListener(SWT.PaintItem, new Listener() {
                public void handleEvent(Event event) {
                    if (viewer instanceof TreeViewer) {
                        TreeColumn column = ((TreeViewer) viewer).getTree().getColumn(event.index);
                        if (((ColumnInfo)column.getData()).labelProvider instanceof ILazyLabelProvider &&
                            CommonUtils.isEmpty(((TreeItem) event.item).getText(event.index)))
                        {
                            final String lazyText = ((ILazyLabelProvider) ((ColumnInfo) column.getData()).labelProvider).getLazyText(event.item.getData());
                            if (!CommonUtils.isEmpty(lazyText)) {
                                ((TreeItem) event.item).setText(event.index, lazyText);
                            }
                        }
                    } else {
                        TableColumn column = ((TableViewer) viewer).getTable().getColumn(event.index);
                        if (((ColumnInfo)column.getData()).labelProvider instanceof ILazyLabelProvider &&
                            CommonUtils.isEmpty(((TableItem) event.item).getText(event.index)))
                        {
                            final String lazyText = ((ILazyLabelProvider) ((ColumnInfo) column.getData()).labelProvider).getLazyText(event.item.getData());
                            if (!CommonUtils.isEmpty(lazyText)) {
                                ((TableItem) event.item).setText(event.index, lazyText);
                            }
                        }
                    }
                }
            });
        }

    }

    private Collection<ColumnInfo> getVisibleColumns()
    {
        Set<ColumnInfo> visibleList = new TreeSet<>(new ColumnInfoComparator());
        for (ColumnInfo column : columns) {
            if (column.visible) {
                visibleList.add(column);
            }
        }
        return visibleList;
    }

    // Read config from dialog settings
    private void readColumnsConfiguration()
    {
        IDialogSettings settings = UIUtils.getDialogSettings(configId);
        for (int i = 0;; i++) {
            String columnDesc = settings.get(String.valueOf(i));
            if (columnDesc == null) {
                break;
            }
            StringTokenizer st = new StringTokenizer(columnDesc, ":");
            boolean visible = Boolean.valueOf(st.nextToken());
            int order = Integer.parseInt(st.nextToken());
            int width = Integer.parseInt(st.nextToken());
            String name = st.nextToken();
            for (ColumnInfo columnInfo : columns) {
                if (columnInfo.name.equals(name)) {
                    columnInfo.visible = visible;
                    columnInfo.order = order;
                    columnInfo.width = width;
                    break;
                } else if (columnInfo.order == order) {
                    // Order conflict
                    columnInfo.order = columns.size() - 1;
                }
            }
        }
    }

    public void configureColumns()
    {
        ConfigDialog configDialog = new ConfigDialog();
        if (configDialog.open() != IDialogConstants.OK_ID) {
            return;
        }
        saveColumnConfig();
    }

    private void saveColumnConfig()
    {
        IDialogSettings settings = UIUtils.getDialogSettings(configId);
        for (ColumnInfo columnInfo : columns) {
            settings.put(String.valueOf(columnInfo.order), columnInfo.visible + ":" + columnInfo.order + ":" + columnInfo.width + ":" + columnInfo.name);
        }
    }

    private static class ColumnInfo {
        final String name;
        final String description;
        final int style;
        final boolean defaultVisible;
        final boolean required;
        final CellLabelProvider labelProvider;

        boolean visible;
        int order;
        int width;
        Item column;
        SortListener sortListener;

        private ColumnInfo(String name, String description, int style, boolean defaultVisible, boolean required, CellLabelProvider labelProvider, int order)
        {
            this.name = name;
            this.description = description;
            this.style = style;
            this.defaultVisible = defaultVisible;
            this.required = required;
            this.visible = defaultVisible;
            this.labelProvider = labelProvider;
            this.order = order;
        }

        @Override
        public String toString() {
            return name + ":" + order;
        }
    }

    private class ConfigDialog extends BaseDialog {

        private final Map<ColumnInfo, Button> buttonMap = new HashMap<>();
        protected ConfigDialog()
        {
            super(viewer.getControl().getShell(), "Configure columns", UIIcon.CONFIGURATION);
        }

        protected void setShellStyle(int newShellStyle) {
            super.setShellStyle(newShellStyle & ~SWT.MAX);
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Composite createDialogArea(Composite parent)
        {
            Composite composite = super.createDialogArea(parent);

            UIUtils.createControlLabel(composite, "Select columns you want to display");

            Set<ColumnInfo> orderedList = new TreeSet<>(new ColumnInfoComparator());
            orderedList.addAll(columns);
            for (ColumnInfo columnInfo : orderedList) {
                Button check = new Button(composite, SWT.CHECK);
                check.setText(columnInfo.name);
                check.setSelection(columnInfo.visible);
                if (!CommonUtils.isEmpty(columnInfo.description)) {
                    check.setToolTipText(columnInfo.description);
                }
                if (columnInfo.required) {
                    check.setEnabled(false);
                }
                buttonMap.put(columnInfo, check);
            }

            return parent;
        }

        @Override
        protected void okPressed()
        {
            boolean recreateColumns = false;
            for (Map.Entry<ColumnInfo, Button> cbEntry : buttonMap.entrySet()) {
                if (cbEntry.getValue().getSelection() != cbEntry.getKey().visible) {
                    cbEntry.getKey().visible = cbEntry.getValue().getSelection();
                    recreateColumns = true;
                }
            }
            if (recreateColumns) {
                recreateColumns();
            }
            super.okPressed();
        }

    }

    private static class ColumnInfoComparator implements Comparator<ColumnInfo> {
        @Override
        public int compare(ColumnInfo o1, ColumnInfo o2)
        {
            return o1.order - o2.order;
        }
    }

    private class SortListener implements Listener
    {
        ColumnInfo columnInfo;
        int sortDirection = SWT.DOWN;
        Item prevColumn = null;

        public SortListener(ColumnInfo columnInfo) {
            this.columnInfo = columnInfo;
        }

        @Override
        public void handleEvent(Event e) {
            Item column = (Item)e.widget;
            if (prevColumn == column) {
                // Set reverse order
                sortDirection = sortDirection == SWT.UP ? SWT.DOWN : SWT.UP;
            }
            prevColumn = column;

            sortViewer(column, sortDirection);
        }

        private void sortViewer(Item column, final int sortDirection) {
            Collator collator = Collator.getInstance();
            if (viewer instanceof TreeViewer) {
                ((TreeViewer)viewer).getTree().setSortColumn((TreeColumn) column);
                ((TreeViewer)viewer).getTree().setSortDirection(sortDirection);
            } else {
                ((TableViewer)viewer).getTable().setSortColumn((TableColumn) column);
                ((TableViewer)viewer).getTable().setSortDirection(sortDirection);
            }
            final ILabelProvider labelProvider = (ILabelProvider)columnInfo.labelProvider;

            viewer.setSorter(new ViewerSorter(collator) {
                @Override
                public int compare(Viewer v, Object e1, Object e2)
                {
                    int result;
                    String value1 = labelProvider.getText(e1);
                    String value2 = labelProvider.getText(e2);
                    if (value1 == null && value2 == null) {
                        result = 0;
                    } else if (value1 == null) {
                        result = -1;
                    } else if (value2 == null) {
                        result = 1;
                    } else {
                        try {
                            return (int)(Long.parseLong(value1) - Long.parseLong(value2));
                        } catch (NumberFormatException e) {
                            // not numbers
                        }
                        result = value1.compareToIgnoreCase(value2);
                    }
                    return sortDirection == SWT.DOWN ? result : -result;
                }
            });
        }
    }

}
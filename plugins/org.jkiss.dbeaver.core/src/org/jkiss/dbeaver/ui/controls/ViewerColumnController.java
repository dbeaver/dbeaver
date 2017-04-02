/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.ILabelProviderEx;
import org.jkiss.dbeaver.ui.ILazyLabelProvider;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Array;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * Tree/table viewer column controller
 */
public class ViewerColumnController {

    private static final Log log = Log.getLog(ViewerColumnController.class);

    private static final String DATA_KEY = ViewerColumnController.class.getSimpleName();

    private final String configId;
    private final ColumnViewer viewer;
    private final List<ColumnInfo> columns = new ArrayList<>();
    private boolean clickOnHeader;
    private boolean isPacking, isInitializing;

    private transient Listener menuListener;

    public static ViewerColumnController getFromControl(Control control)
    {
        return (ViewerColumnController)control.getData(DATA_KEY);
    }

    public ViewerColumnController(String id, ColumnViewer viewer)
    {
        this.configId = id;
        this.viewer = viewer;
        final Control control = this.viewer.getControl();
        control.setData(DATA_KEY, this);

        if (control instanceof Tree || control instanceof Table) {
            menuListener = new Listener() {
                @Override
                public void handleEvent(Event event) {
                    Point pt = control.getDisplay().map(null, control, new Point(event.x, event.y));
                    Rectangle clientArea = ((Composite) control).getClientArea();
                    if (RuntimeUtils.isPlatformMacOS()) {
                        clickOnHeader = pt.y < 0;
                    } else {
                        if (control instanceof Tree) {
                            clickOnHeader = clientArea.y <= pt.y && pt.y < (clientArea.y + ((Tree) control).getHeaderHeight());
                        } else {
                            clickOnHeader = clientArea.y <= pt.y && pt.y < (clientArea.y + ((Table) control).getHeaderHeight());
                        }
                    }
                }
            };
            control.addListener(SWT.MenuDetect, menuListener);
        }
    }

    public void dispose() {
        clearColumns();
        final Control control = this.viewer.getControl();
        if (!control.isDisposed()) {
            if (menuListener != null) {
                control.removeListener(SWT.MenuDetect, menuListener);
                menuListener = null;
            }
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
        addColumn(name, description, style, defaultVisible, required, false, null, labelProvider, null);
    }

    public void addColumn(String name, String description, int style, boolean defaultVisible, boolean required, boolean isNumeric, Object userData, CellLabelProvider labelProvider, EditingSupport editingSupport)
    {
        columns.add(
            new ColumnInfo(
                name,
                description,
                style,
                defaultVisible,
                required,
                isNumeric,
                userData,
                labelProvider,
                editingSupport,
                columns.size()));
    }

    private void clearColumns() {
        for (ColumnInfo columnInfo : columns) {
            if (columnInfo.column != null) {
                columnInfo.column.dispose();
                columnInfo.column = null;
            }
        }
        columns.clear();
    }

    public void createColumns() {
        this.createColumns(true);
    }

    public void createColumns(boolean pack)
    {
        try {
            readColumnsConfiguration();
        } catch (Exception e) {
            // Possibly incompatible format from previous version
            log.warn("Failed to load configuration for '" + this.configId + "'", e);
        }
        recreateColumns(pack);
    }

    private void recreateColumns(boolean pack)
    {
        final Control control = viewer.getControl();
        control.setRedraw(false);
        isInitializing = true;
        try {
            boolean needRefresh = false;
            for (ColumnInfo columnInfo : columns) {
                if (columnInfo.column != null) {
                    columnInfo.column.dispose();
                    columnInfo.column = null;
                    needRefresh = true;
                }
            }
            createVisibleColumns();
            if (pack && !isAllSized()) {
                repackColumns();
                control.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e) {
                        control.removeControlListener(this);
                        repackColumns();
                    }
                });
            }
            if (needRefresh) {
                viewer.refresh();
                for (ColumnInfo columnInfo : getVisibleColumns()) {
                    if (columnInfo.column instanceof TreeColumn) {
                        ((TreeColumn) columnInfo.column).pack();
                    } else {
                        ((TableColumn) columnInfo.column).pack();
                    }
                }
            }
        } finally {
            control.setRedraw(true);
            isInitializing = false;
        }
    }

    private boolean isAllSized() {
        for (ColumnInfo columnInfo : columns) {
            if (columnInfo.visible && columnInfo.width <= 0) {
                return false;
            }
        }
        return true;
    }

    public void repackColumns()
    {
        if (isAllSized()) {
            return;
        }
        isPacking = true;
        try {
            if (viewer instanceof TreeViewer) {
                float[] ratios = null;
                if (((TreeViewer) viewer).getTree().getColumnCount() == 2) {
                    ratios = new float[]{0.6f, 0.4f};
                }
                UIUtils.packColumns(((TreeViewer) viewer).getTree(), false, ratios);
            } else if (viewer instanceof TableViewer) {
                UIUtils.packColumns(((TableViewer)viewer).getTable());
            }
        } finally {
            isPacking = false;
        }
    }

    public void sortByColumn(int index, int direction) {
        final ColumnInfo columnInfo = columns.get(index);
        columnInfo.sortListener.sortViewer(columnInfo.column, direction);
    }

    private void createVisibleColumns()
    {
        boolean hasLazyColumns = false;
        List<ColumnInfo> visibleColumns = getVisibleColumns();
        for (int i = 0; i < visibleColumns.size(); i++) {
            final ColumnInfo columnInfo = visibleColumns.get(i);
            columnInfo.order = i;
            final Item colItem;
            ViewerColumn viewerColumn;
            if (viewer instanceof TreeViewer) {
                final TreeViewerColumn item = new TreeViewerColumn((TreeViewer) viewer, columnInfo.style);
                viewerColumn = item;
                final TreeColumn column = item.getColumn();
                colItem = column;
                column.setText(columnInfo.name);
                column.setMoveable(true);
                column.setWidth(columnInfo.width);
                if (!CommonUtils.isEmpty(columnInfo.description)) {
                    column.setToolTipText(columnInfo.description);
                }
                column.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e) {
                        columnInfo.width = column.getWidth();
                        saveColumnConfig();
                    }

                    @Override
                    public void controlMoved(ControlEvent e) {
                        if (!isInitializing && e.getSource() instanceof TreeColumn) {
                            updateColumnOrder(column, column.getParent().getColumnOrder());
                        }
                    }
                });
                columnInfo.column = column;
            } else if (viewer instanceof TableViewer) {
                final TableViewerColumn item = new TableViewerColumn((TableViewer) viewer, columnInfo.style);
                viewerColumn = item;
                final TableColumn column = item.getColumn();
                colItem = column;
                column.setText(columnInfo.name);
                column.setMoveable(true);
                column.setWidth(columnInfo.width);
                if (!CommonUtils.isEmpty(columnInfo.description)) {
                    column.setToolTipText(columnInfo.description);
                }
                column.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e) {
                        columnInfo.width = column.getWidth();
                        saveColumnConfig();
                    }

                    @Override
                    public void controlMoved(ControlEvent e) {
                        if (!isInitializing && e.getSource() instanceof TableColumn) {
                            updateColumnOrder(column, column.getParent().getColumnOrder());
                        }
                    }
                });
                columnInfo.column = column;
            } else {
                continue;
            }
            viewerColumn.setLabelProvider(columnInfo.labelProvider);
            viewerColumn.setEditingSupport(columnInfo.editingSupport);
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
                        if (((ColumnInfo) column.getData()).labelProvider instanceof ILazyLabelProvider &&
                            CommonUtils.isEmpty(((TreeItem) event.item).getText(event.index))) {
                            final String lazyText = ((ILazyLabelProvider) ((ColumnInfo) column.getData()).labelProvider).getLazyText(event.item.getData());
                            if (!CommonUtils.isEmpty(lazyText)) {
                                ((TreeItem) event.item).setText(event.index, lazyText);
                            }
                        }
                    } else {
                        TableColumn column = ((TableViewer) viewer).getTable().getColumn(event.index);
                        if (((ColumnInfo) column.getData()).labelProvider instanceof ILazyLabelProvider &&
                            CommonUtils.isEmpty(((TableItem) event.item).getText(event.index))) {
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

    private List<ColumnInfo> getVisibleColumns()
    {
        List<ColumnInfo> visibleList = new ArrayList<>();
        for (ColumnInfo column : columns) {
            if (column.visible) {
                visibleList.add(column);
            }
        }
        Collections.sort(visibleList, new ColumnInfoComparator());
        return visibleList;
    }

    // Read config from dialog settings
    private void readColumnsConfiguration()
    {
        final Collection<ViewerColumnRegistry.ColumnState> savedConfig = ViewerColumnRegistry.getInstance().getSavedConfig(configId);
        if (savedConfig == null) {
            return;
        }
        for (ColumnInfo columnInfo : columns) {
            for (ViewerColumnRegistry.ColumnState savedState : savedConfig) {
                if (columnInfo.name.equals(savedState.name)) {
                    columnInfo.visible = savedState.visible;
                    columnInfo.order = savedState.order;
                    columnInfo.width = savedState.width;
                    break;
                }
            }
        }
    }

    public Object getColumnData(int columnIndex) {
        final Control control = viewer.getControl();
        ColumnInfo columnInfo;
        if (control instanceof Tree) {
            columnInfo = (ColumnInfo) ((Tree) control).getColumn(columnIndex).getData();
        } else {
            columnInfo = (ColumnInfo) ((Table) control).getColumn(columnIndex).getData();
        }
        return columnInfo.userData;
    }

    public <T> T[] getColumnsData(Class<T> type) {
        T[] newArray = (T[]) Array.newInstance(type, columns.size());
        for (int i = 0; i < columns.size(); i++) {
            newArray[i] = type.cast(columns.get(i).userData);
        }
        return newArray;
    }

    public void configureColumns()
    {
        ConfigDialog configDialog = new ConfigDialog();
        if (configDialog.open() != IDialogConstants.OK_ID) {
            return;
        }
        saveColumnConfig();
    }

    private void updateColumnOrder(Item column, int[] order) {
        if (isPacking) {
            return;
        }
        ColumnInfo columnInfo = (ColumnInfo) column.getData();
        boolean updated = false;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == columnInfo.order) {
                columnInfo.order = i;
                updated = true;
                break;
            }
        }
        if (updated) {
            saveColumnConfig();
        }
    }

    private void saveColumnConfig()
    {
        // Save settings
        ViewerColumnRegistry.getInstance().updateConfig(configId, columns);
    }

    public int getColumnsCount() {
        final Control control = viewer.getControl();
        return control instanceof Tree ?
            ((Tree) control).getColumnCount() : ((Table) control).getColumnCount();
    }

    private static class ColumnInfo extends ViewerColumnRegistry.ColumnState {
        final String description;
        final int style;
        final boolean defaultVisible;
        final boolean required;
        final boolean numeric;
        final Object userData;
        final CellLabelProvider labelProvider;
        final EditingSupport editingSupport;

        Item column;
        SortListener sortListener;

        private ColumnInfo(String name, String description, int style, boolean defaultVisible, boolean required, boolean numeric, Object userData, CellLabelProvider labelProvider, EditingSupport editingSupport, int order)
        {
            this.name = name;
            this.description = description;
            this.style = style;
            this.defaultVisible = defaultVisible;
            this.required = required;
            this.numeric = numeric;
            this.userData = userData;
            this.visible = defaultVisible;
            this.labelProvider = labelProvider;
            this.editingSupport = editingSupport;
            this.order = order;
        }
    }

    private class ConfigDialog extends BaseDialog {

        private Table colTable;

        //private final Map<ColumnInfo, Button> buttonMap = new HashMap<>();
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

            List<ColumnInfo> orderedList = new ArrayList<>(columns);
            Collections.sort(orderedList, new ColumnInfoComparator());
            colTable = new Table(composite, SWT.BORDER | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
            colTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            colTable.setLinesVisible(true);
            colTable.addListener(SWT.Selection,new Listener() {
                public void handleEvent(Event event) {
                    if( event.detail == SWT.CHECK ) {
                        if (((TableItem)event.item).getGrayed()) {
                            ((TableItem)event.item).setChecked(true);
                            event.doit = false;
                        }
                    }
                }
            });
            final TableColumn nameColumn = new TableColumn(colTable, SWT.LEFT);
            nameColumn.setText("Name");
            final TableColumn descColumn = new TableColumn(colTable, SWT.LEFT);
            descColumn.setText("Description");

            for (ColumnInfo columnInfo : orderedList) {
                TableItem colItem = new TableItem(colTable, SWT.NONE);
                colItem.setData(columnInfo);
                colItem.setText(0, columnInfo.name);
                if (!CommonUtils.isEmpty(columnInfo.description)) {
                    colItem.setText(1, columnInfo.description);
                }
                colItem.setChecked(columnInfo.visible);
                if (columnInfo.required) {
                    colItem.setGrayed(true);
                }
            }
            nameColumn.pack();
            if (nameColumn.getWidth() > 300) {
                nameColumn.setWidth(300);
            }
            descColumn.pack();
            if (descColumn.getWidth() > 400) {
                descColumn.setWidth(400);
            }

            return parent;
        }

        @Override
        protected void okPressed()
        {
            boolean recreateColumns = false;
            for (TableItem item : colTable.getItems()) {
                ColumnInfo ci = (ColumnInfo) item.getData();
                if (item.getChecked() != ci.visible) {
                    ci.visible = item.getChecked();
                    recreateColumns = true;
                }
            }
            if (recreateColumns) {
                recreateColumns(true);
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

        private void sortViewer(final Item column, final int sortDirection) {
            Collator collator = Collator.getInstance();
            if (viewer instanceof TreeViewer) {
                ((TreeViewer)viewer).getTree().setSortColumn((TreeColumn) column);
                ((TreeViewer)viewer).getTree().setSortDirection(sortDirection);
            } else {
                ((TableViewer)viewer).getTable().setSortColumn((TableColumn) column);
                ((TableViewer)viewer).getTable().setSortDirection(sortDirection);
            }
            final ILabelProvider labelProvider = (ILabelProvider)columnInfo.labelProvider;
            final ILabelProviderEx exLabelProvider = labelProvider instanceof ILabelProviderEx ? (ILabelProviderEx)labelProvider : null;

            viewer.setComparator(new ViewerComparator(collator) {
                private final NumberFormat numberFormat = NumberFormat.getInstance();
                @Override
                public int compare(Viewer v, Object e1, Object e2)
                {
                    int result;
                    String value1;
                    String value2;
                    if (exLabelProvider != null) {
                        value1 = exLabelProvider.getText(e1, false);
                        value2 = exLabelProvider.getText(e2, false);
                    } else {
                        value1 = labelProvider.getText(e1);
                        value2 = labelProvider.getText(e2);
                    }
                    if (value1 == null && value2 == null) {
                        result = 0;
                    } else if (value1 == null) {
                        result = -1;
                    } else if (value2 == null) {
                        result = 1;
                    } else {
                        if (columnInfo.numeric) {
                            try {
                                final Number num1 = numberFormat.parse(value1);
                                final Number num2 = numberFormat.parse(value2);
                                if (num1.getClass() == num2.getClass() && num1 instanceof Comparable) {
                                    result = ((Comparable) num1).compareTo(num2);
                                } else {
                                    // Dunno how to compare
                                    result = 0;
                                }
                            } catch (Exception e) {
                                // not numbers
                                result = value1.compareToIgnoreCase(value2);
                            }
                        } else {
                            result = value1.compareToIgnoreCase(value2);
                        }
                    }
                    return sortDirection == SWT.DOWN ? result : -result;
                }
            });
        }
    }

}
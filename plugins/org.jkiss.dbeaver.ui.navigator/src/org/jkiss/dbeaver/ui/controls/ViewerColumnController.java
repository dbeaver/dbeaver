/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Array;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Tree/table viewer column controller
 */
public class ViewerColumnController<COLUMN, ELEMENT> {
    private static final Log log = Log.getLog(ViewerColumnController.class);

    private static final String DATA_KEY = ViewerColumnController.class.getSimpleName();

    private static final int MIN_COLUMN_AUTO_WIDTH = 100;

    private final String configId;
    private final ColumnViewer viewer;
    private final List<ColumnInfo> columns = new ArrayList<>();
    private boolean clickOnHeader;
    private boolean isPacking, isInitializing;
    private DBIcon defaultIcon;
    private boolean forceAutoSize;

    private transient ObjectViewerRenderer cellRenderer;
    private transient Listener menuListener;

    private int selectedColumnNumber;

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
            menuListener = event -> {
                Point pt = control.getDisplay().map(null, control, new Point(event.x, event.y));
                Rectangle clientArea = ((Composite) control).getClientArea();
                if (RuntimeUtils.isMacOS()) {
                    clickOnHeader = pt.y < 0;
                } else {
                    if (control instanceof Tree) {
                        clickOnHeader = clientArea.y <= pt.y && pt.y < (clientArea.y + ((Tree) control).getHeaderHeight());
                    } else {
                        clickOnHeader = clientArea.y <= pt.y && pt.y < (clientArea.y + ((Table) control).getHeaderHeight());
                    }
                }
                if (clickOnHeader) {
                    int pointYWithHeader;
                    // We can't get column number, if we use click on the header, but we can add header height to the y
                    if (viewer instanceof TableViewer && control instanceof Table) {
                        pointYWithHeader = pt.y + ((Table) control).getHeaderHeight();
                        TableItem selectedItem = ((TableViewer) this.viewer).getTable().getItem(new Point(pt.x, pointYWithHeader));
                        if (selectedItem != null) {
                            selectedColumnNumber = UIUtils.getColumnAtPos(selectedItem, pt.x, pointYWithHeader);
                        }
                    } else if (viewer instanceof TreeViewer && control instanceof Tree) {
                        pointYWithHeader = pt.y + ((Tree) control).getHeaderHeight();
                        TreeItem selectedItem = ((TreeViewer) viewer).getTree().getItem(new Point(pt.x, pointYWithHeader));
                        if (selectedItem != null) {
                            selectedColumnNumber = UIUtils.getColumnAtPos(selectedItem, pt.x, pointYWithHeader);
                        }
                    }
                }
            };
            control.addListener(SWT.MenuDetect, menuListener);
        }

        cellRenderer = new ObjectViewerRenderer(viewer, false) {
            @Nullable
            @Override
            public Object getCellValue(Object element, int columnIndex) {
                List<ColumnInfo> visibleColumns = getVisibleColumns();
                if (!visibleColumns.isEmpty()) {
                    ColumnInfo columnInfo = getVisibleColumns().get(columnIndex);
                    if (columnInfo.labelProvider instanceof ColumnBooleanLabelProvider) {
                        return ((ColumnBooleanLabelProvider) columnInfo.labelProvider).getValueProvider().getValue(element);
                    }
                }
                return null;
            }
        };

        viewer.setComparator(new DefaultComparator(Collator.getInstance()));
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

    public void setIsColumnVisible(int index, boolean visible) {
        columns.get(index).visible = visible;
        ViewerColumnRegistry.getInstance().updateConfig(configId, columns);
    }


    public boolean isClickOnHeader()
    {
        return clickOnHeader;
    }

    public void setForceAutoSize(boolean forceAutoSize) {
        this.forceAutoSize = forceAutoSize;
    }

    public void setDefaultIcon(DBIcon defaultIcon) {
        this.defaultIcon = defaultIcon;
    }

    public void setComparator(@NotNull DefaultComparator comparator) {
        viewer.setComparator(comparator);
    }

    public int getSelectedColumnNumber() {
        return selectedColumnNumber;
    }

    public void fillConfigMenu(IContributionManager menuManager) {
        menuManager.add(new Action(UINavigatorMessages.obj_editor_properties_control_action_configure_columns, DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)) {
            {
                setDescription(UINavigatorMessages.obj_editor_properties_control_action_configure_columns_description);
            }
            @Override
            public void run()
            {
                configureColumns();
            }
        });
        menuManager.add(new Action(UINavigatorMessages.obj_editor_properties_control_action_columns_fit_width) {
            {
                setDescription(UINavigatorMessages.obj_editor_properties_control_action_columns_fit_width_description);
            }
            @Override
            public void run()
            {
                repackColumns(true);
            }
        });
    }

    public void addColumn(
        String name,
        String description,
        int style,
        boolean defaultVisible,
        boolean required,
        IColumnTextProvider<ELEMENT> labelProvider,
        EditingSupport editingSupport)
    {
        addColumn(name, description, style, defaultVisible, required, false, null, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return labelProvider.getText((ELEMENT) element);
            }

            @Override
            public void update(ViewerCell cell) {
                if (cell.getColumnIndex() == 0) {
                    if (defaultIcon != null) {
                        cell.setImage(DBeaverIcons.getImage(defaultIcon));
                    }
                }
                cell.setText(labelProvider.getText((ELEMENT) cell.getElement()));
            }
        }, editingSupport);
    }

    public void addBooleanColumn(
        String name,
        String description,
        int style,
        boolean defaultVisible,
        boolean required,
        IColumnValueProvider<ELEMENT, Boolean> valueProvider,
        EditingSupport editingSupport)
    {
        addColumn(name, description, style, defaultVisible, required, false, null, new ColumnBooleanLabelProvider<>(valueProvider), editingSupport);
    }

    public void addColumn(String name, String description, int style, boolean defaultVisible, boolean required, CellLabelProvider labelProvider)
    {
        addColumn(name, description, style, defaultVisible, required, false, null, labelProvider, null);
    }

    public void addColumn(
        String name,
        String description,
        int style,
        boolean defaultVisible,
        boolean required,
        boolean numeric,
        Object userData,
        CellLabelProvider labelProvider,
        EditingSupport editingSupport)
    {
        columns.add(
            new ColumnInfo(
                name,
                description,
                style,
                defaultVisible,
                required,
                numeric,
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
            log.warn("Failed to load configuration for '" + this.configId + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
        recreateColumns(pack);
    }

    private void recreateColumns(boolean pack)
    {
        final Control control = viewer.getControl();
        if (control == null || control.isDisposed()) {
        	return;
        }
        control.setRedraw(false);
        isInitializing = true;
        try {
            boolean needRefresh = false;
            for (ColumnInfo columnInfo : columns) {
                boolean columnExists = (columnInfo.column != null);
                if (columnExists != columnInfo.visible) {
                    needRefresh = true;
                }
                if (columnInfo.column != null) {
                    columnInfo.column.dispose();
                    columnInfo.column = null;
                }
            }
            createVisibleColumns();

            if (needRefresh) {
                viewer.refresh();
            }
            boolean allSized = isAllSized();
            if (pack && !allSized) {
                repackColumns();
                control.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e) {
                        control.removeControlListener(this);
                        if (getRowCount() > 0) {
                            repackColumns();
                        }
                    }
                });
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

    public void repackColumns() {
        repackColumns(false);
    }

    private void repackColumns(boolean forceRepack) {
        if (!forceRepack && isAllSized()) {
            return;
        }
        isPacking = true;
        try {
            int itemCount = 0;
            if (viewer instanceof TreeViewer) {
                itemCount = ((TreeViewer) viewer).getTree().getItemCount();
                float[] ratios = null;
                if (((TreeViewer) viewer).getTree().getColumnCount() == 2) {
                    ratios = new float[]{0.6f, 0.4f};
                }
                UIUtils.packColumns(((TreeViewer) viewer).getTree(), forceAutoSize, ratios);
            } else if (viewer instanceof TableViewer) {
                itemCount = ((TableViewer) viewer).getTable().getItemCount();
                UIUtils.packColumns(((TableViewer) viewer).getTable(), forceAutoSize);
            }

            /*if (itemCount == 0) */{
                // Fix too narrow width for empty lists
                for (ColumnInfo columnInfo : getVisibleColumns()) {
                    if (columnInfo.column instanceof TreeColumn) {
                        int realWidth = ((TreeColumn) columnInfo.column).getWidth();
                        if (realWidth < MIN_COLUMN_AUTO_WIDTH) {
                            ((TreeColumn) columnInfo.column).setWidth(MIN_COLUMN_AUTO_WIDTH);
                            realWidth = MIN_COLUMN_AUTO_WIDTH;
                        }
                        columnInfo.width = realWidth;
                    } else if (columnInfo.column instanceof TableColumn) {
                        int realWidth = ((TableColumn) columnInfo.column).getWidth();
                        if (realWidth < MIN_COLUMN_AUTO_WIDTH) {
                            ((TableColumn) columnInfo.column).setWidth(MIN_COLUMN_AUTO_WIDTH);
                            realWidth = MIN_COLUMN_AUTO_WIDTH;
                        }
                        columnInfo.width = realWidth;
                    }
                }
            }

        } finally {
            isPacking = false;
        }
    }

    public void autoSizeColumns() {
        UIUtils.asyncExec(() -> {
            Control control = this.viewer.getControl();
            if (control instanceof Tree) {
                UIUtils.packColumns((Tree) control, true, null);
            } else if (control instanceof Table) {
                UIUtils.packColumns((Table) control, true);
            }
        });
    }

    public void sortByColumn(int index, int direction) {
        final ColumnInfo columnInfo = columns.get(index);
        columnInfo.sortListener.sortViewer(columnInfo.column, direction);
    }

    private void createVisibleColumns()
    {
        boolean hasCustomDraw = false;
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
                if (columnInfo.width > 0) {
                    column.setWidth(columnInfo.width);
                }
                if (!CommonUtils.isEmpty(columnInfo.description)) {
                    column.setToolTipText(columnInfo.description);
                }
                column.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e) {
                        if (!isInitializing && !isPacking) {
                            columnInfo.width = column.getWidth();
                            if (getRowCount() > 0) {
                                saveColumnConfig();
                            }
                        }
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
                        if (getRowCount() > 0) {
                            saveColumnConfig();
                        }
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
            if (columnInfo.labelProvider instanceof ILazyLabelProvider || columnInfo.labelProvider instanceof ColumnBooleanLabelProvider) {
                hasCustomDraw = true;
            } else if (columnInfo.labelProvider instanceof ILabelProvider) {
                columnInfo.sortListener = new SortListener(viewer, columnInfo);
                columnInfo.column.addListener(SWT.Selection, columnInfo.sortListener);
            }
        }
        if (hasCustomDraw) {
            viewer.getControl().addListener(SWT.PaintItem, event -> {
                ColumnInfo columnInfo;
                if (viewer instanceof TreeViewer) {
                    TreeColumn column = ((TreeViewer) viewer).getTree().getColumn(event.index);
                    columnInfo = (ColumnInfo) column.getData();
                    if (columnInfo.labelProvider instanceof ILazyLabelProvider &&
                        CommonUtils.isEmpty(((TreeItem) event.item).getText(event.index))) {
                        final String lazyText = ((ILazyLabelProvider) columnInfo.labelProvider).getLazyText(event.item.getData());
                        if (!CommonUtils.isEmpty(lazyText)) {
                            ((TreeItem) event.item).setText(event.index, lazyText);
                        }
                    }
                } else {
                    TableColumn column = ((TableViewer) viewer).getTable().getColumn(event.index);
                    columnInfo = (ColumnInfo) column.getData();
                    if (columnInfo.labelProvider instanceof ILazyLabelProvider &&
                        CommonUtils.isEmpty(((TableItem) event.item).getText(event.index))) {
                        final String lazyText = ((ILazyLabelProvider) columnInfo.labelProvider).getLazyText(event.item.getData());
                        if (!CommonUtils.isEmpty(lazyText)) {
                            ((TableItem) event.item).setText(event.index, lazyText);
                        }
                    }
                }
                if (columnInfo.labelProvider instanceof ColumnBooleanLabelProvider<?, ?>) {
                    Object element = event.item.getData();
                    Object cellValue = ((ColumnBooleanLabelProvider) columnInfo.labelProvider).getValueProvider().getValue(element);
                    cellRenderer.paintCell(event, element, cellValue, event.item, Boolean.class, event.index, true, (event.detail & SWT.SELECTED) == SWT.SELECTED);
                }
            });
        }

    }

    public List<ColumnInfo> getVisibleColumns()
    {
        List<ColumnInfo> visibleList = new ArrayList<>();
        for (ColumnInfo column : columns) {
            if (column.visible) {
                visibleList.add(column);
            }
        }
        visibleList.sort(new ColumnInfoComparator());
        return visibleList;
    }

    // Read config from dialog settings
    private void readColumnsConfiguration()
    {
        final Collection<ViewerColumnRegistry.ColumnState> savedConfig = ViewerColumnRegistry.getInstance().getSavedConfig(configId);
        if (savedConfig == null || savedConfig.isEmpty()) {
            return;
        }
        boolean hasVisible = false;
        for (ViewerColumnRegistry.ColumnState savedState : savedConfig) {
            if (savedState.visible) {
                hasVisible = true;
                break;
            }
        }
        if (!hasVisible) {
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

    private ColumnInfo getColumnByIndex(int columnIndex) {
        final Control control = viewer.getControl();
        ColumnInfo columnInfo;
        if (control instanceof Tree) {
            columnInfo = (ColumnInfo) ((Tree) control).getColumn(columnIndex).getData();
        } else {
            columnInfo = (ColumnInfo) ((Table) control).getColumn(columnIndex).getData();
        }
        return columnInfo;
    }

    @Nullable
    private ColumnInfo getSortColumn() {
        final Control control = viewer.getControl();

        if (control instanceof Tree) {
            final Tree tree = (Tree) control;
            final TreeColumn column = tree.getSortColumn();
            if (column != null) {
                return getColumnByIndex(tree.indexOf(column));
            }
        } else {
            final Table table = (Table) control;
            final TableColumn column = table.getSortColumn();
            if (column != null) {
                return getColumnByIndex(table.indexOf(column));
            }
        }

        return null;
    }

    private int getSortDirection() {
        final Control control = viewer.getControl();

        if (control instanceof Tree) {
            return ((Tree) control).getSortDirection();
        } else {
            return ((Table) control).getSortDirection();
        }
    }

    public COLUMN getColumnData(int columnIndex) {
        return (COLUMN) getColumnByIndex(columnIndex).userData;
    }

    public String getColumnName(int columnIndex) {
        return getColumnByIndex(columnIndex).name;
    }

    public COLUMN[] getColumnsData(Class<COLUMN> type) {
        COLUMN[] newArray = (COLUMN[]) Array.newInstance(type, columns.size());
        for (int i = 0; i < columns.size(); i++) {
            newArray[i] = type.cast(columns.get(i).userData);
        }
        return newArray;
    }

    public boolean configureColumns()
    {
        ConfigDialog configDialog = new ConfigDialog();
        if (configDialog.open() != IDialogConstants.OK_ID) {
            return false;
        }
        saveColumnConfig();
        return true;
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
        // Save settings only if we have at least one rows. Otherwise
        ViewerColumnRegistry.getInstance().updateConfig(configId, columns);
    }

    public int getColumnsCount() {
        final Control control = viewer.getControl();
        return control instanceof Tree ?
            ((Tree) control).getColumnCount() : ((Table) control).getColumnCount();
    }

    public int getRowCount() {
        final Control control = viewer.getControl();
        return control instanceof Tree ?
            ((Tree) control).getItemCount() : ((Table) control).getItemCount();
    }

    public int getEditableColumnIndex(Object element) {
        for (ColumnInfo info : getVisibleColumns()) {
            if (info.editingSupport != null) {
                return info.order;
            }
        }
        return -1;
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
            super(viewer.getControl().getShell(), UINavigatorMessages.label_configure_columns, UIIcon.CONFIGURATION);
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

            UIUtils.createControlLabel(composite, UINavigatorMessages.label_select_columns);

            List<ColumnInfo> orderedList = new ArrayList<>(columns);
            orderedList.sort(new ColumnInfoComparator());
            colTable = new Table(composite, SWT.BORDER | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
            colTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            colTable.setLinesVisible(true);
            colTable.setHeaderVisible(true);
            colTable.addListener(SWT.Selection, event -> {
                if (event.detail == SWT.CHECK) {
                    if (((TableItem) event.item).getGrayed()) {
                        ((TableItem) event.item).setChecked(true);
                        event.doit = false;
                    }
                }
            });
            final TableColumn nameColumn = new TableColumn(colTable, SWT.LEFT);
            nameColumn.setText(UINavigatorMessages.label_name);
            final TableColumn descColumn = new TableColumn(colTable, SWT.LEFT);
            descColumn.setText(UINavigatorMessages.label_description);

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

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.DETAILS_ID, UIMessages.button_reset_to_defaults, false); //$NON-NLS-1$
            super.createButtonsForButtonBar(parent);
        }

        @Override
        protected void buttonPressed(int buttonId) {
            if (buttonId == IDialogConstants.DETAILS_ID) {
                resetToDefaults();
            }
            super.buttonPressed(buttonId);
        }

        private void resetToDefaults() {
            for (TableItem item : colTable.getItems()) {
                ColumnInfo ci = (ColumnInfo) item.getData();
                item.setChecked(ci.defaultVisible);
            }
        }
    }

    private static class ColumnInfoComparator implements Comparator<ColumnInfo> {
        @Override
        public int compare(ColumnInfo o1, ColumnInfo o2)
        {
            return o1.order - o2.order;
        }
    }

    private static class SortListener implements Listener
    {
        ColumnViewer viewer;
        ColumnInfo columnInfo;
        int sortDirection = SWT.UP;
        Item prevColumn = null;

        public SortListener(ColumnViewer viewer, ColumnInfo columnInfo) {
            this.viewer = viewer;
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
            if (viewer instanceof TreeViewer) {
                ((TreeViewer)viewer).getTree().setSortColumn((TreeColumn) column);
                ((TreeViewer)viewer).getTree().setSortDirection(sortDirection);
            } else {
                ((TableViewer)viewer).getTable().setSortColumn((TableColumn) column);
                ((TableViewer)viewer).getTable().setSortDirection(sortDirection);
            }

            viewer.refresh();
        }
    }

    public static class DefaultComparator extends ViewerComparator {
        public DefaultComparator(@Nullable Comparator<? super String> comparator) {
            super(comparator);
        }

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            final int cat1 = category(e1);
            final int cat2 = category(e2);

            if (cat1 != cat2) {
                return cat1 - cat2;
            }

            final String name1 = getLabel(viewer, e1);
            final String name2 = getLabel(viewer, e2);

            int result = 0;

            if (CommonUtils.equalObjects(name1, name2)) {
                return 0;
            } else if (name1 == null) {
                result = -1;
            } else if (name2 == null) {
                result = 1;
            }

            if (result == 0 && isNumeric(viewer)) {
                try {
                    final NumberFormat numberFormat = NumberFormat.getInstance();
                    final Number number1 = numberFormat.parse(name1);
                    final Number number2 = numberFormat.parse(name2);
                    result = CommonUtils.compareNumbers(number1, number2);
                } catch (Exception ignored) {
                }
            }

            if (result == 0) {
                result = getComparator().compare(name1, name2);
            }

            return isReversed(viewer) ? -result : result;
        }

        @Nullable
        private String getLabel(@NotNull Viewer viewer, @Nullable Object element) {
            final ColumnInfo column = getColumnInfo(viewer);

            if (column == null) {
                return null;
            }

            if (column.labelProvider instanceof ILabelProviderEx) {
                return ((ILabelProviderEx) column.labelProvider).getText(element, false);
            } else {
                return ((ILabelProvider) column.labelProvider).getText(element);
            }
        }

        private static boolean isNumeric(@NotNull Viewer viewer) {
            final ColumnInfo column = getColumnInfo(viewer);
            return column != null && column.numeric;
        }

        private static boolean isReversed(@NotNull Viewer viewer) {
            return ((ViewerColumnController<?, ?>) getFromControl(viewer.getControl())).getSortDirection() == SWT.DOWN;
        }

        @Nullable
        private static ColumnInfo getColumnInfo(@NotNull Viewer viewer) {
            return ((ViewerColumnController<?, ?>) getFromControl(viewer.getControl())).getSortColumn();
        }
    }
}

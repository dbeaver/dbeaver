package org.jkiss.dbeaver.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.List;

/**
 * Tree/table viewer column controller
 */
public class ViewerColumnController {

    private static final String DATA_KEY = ViewerColumnController.class.getSimpleName();

    private final String configId;
    private final ColumnViewer viewer;
    private boolean columnsMovable = true;
    private final List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
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
        readColumnsConfiguration();
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

    private void createVisibleColumns()
    {
        for (final ColumnInfo columnInfo : getVisibleColumns()) {
            if (viewer instanceof TreeViewer) {
                final TreeViewerColumn item = new TreeViewerColumn((TreeViewer) viewer, columnInfo.style);
                final TreeColumn column = item.getColumn();
                column.setText(columnInfo.name);
                column.setMoveable(columnsMovable);
                column.setWidth(columnInfo.width);
                if (!CommonUtils.isEmpty(columnInfo.description)) {
                    column.setToolTipText(columnInfo.description);
                }
                item.setLabelProvider(columnInfo.labelProvider);
                column.addControlListener(new ControlListener() {
                    @Override
                    public void controlMoved(ControlEvent e)
                    {
//                        if (orderChanged(column.getParent().getColumnOrder())) {
//                            updateColumnOrder(column.getParent().getColumns(), column.getParent().getColumnOrder());
//                        }
                    }

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
                column.setText(columnInfo.name);
                column.setMoveable(columnsMovable);
                column.setWidth(columnInfo.width);
                if (!CommonUtils.isEmpty(columnInfo.description)) {
                    column.setToolTipText(columnInfo.description);
                }
                item.setLabelProvider(columnInfo.labelProvider);
                column.addControlListener(new ControlListener() {
                    @Override
                    public void controlMoved(ControlEvent e)
                    {
//                        if (orderChanged(column.getParent().getColumnOrder())) {
//                            updateColumnOrder(column.getParent().getColumns(), column.getParent().getColumnOrder());
//                        }
                    }

                    @Override
                    public void controlResized(ControlEvent e)
                    {
                        columnInfo.width = column.getWidth();
                        saveColumnConfig();
                    }
                });
                columnInfo.column = column;
            }
        }
    }

/*
    private void updateColumnOrder(Item[] items, int[] orderList)
    {
        for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
            Item item = items[i];
            int order = orderList[i];
            for (ColumnInfo columnInfo : columns) {
                if (columnInfo.column == item) {
                    columnInfo.order = order;
                    break;
                }
            }
        }
        recreateColumns();
        saveColumnConfig();
    }

    private boolean orderChanged(int[] order)
    {
        for (int i = 0; i < order.length; i++) {
            if (order[i] != i) {
                return true;
            }
        }
        return false;
    }
*/

    private Collection<ColumnInfo> getVisibleColumns()
    {
        Set<ColumnInfo> visibleList = new TreeSet<ColumnInfo>(new Comparator<ColumnInfo>() {
            @Override
            public int compare(ColumnInfo o1, ColumnInfo o2)
            {
                return o1.order - o2.order;
            }
        });
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
                }
            }
        }
    }

    private void configureColumns()
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

        public int getWidth()
        {
            return column instanceof TreeColumn ? ((TreeColumn) column).getWidth() :
                (column instanceof TableColumn ? ((TableColumn) column).getWidth() :
                    0);
        }
    }

    private class ConfigDialog extends Dialog {

        private final Map<ColumnInfo, Button> buttonMap = new HashMap<ColumnInfo, Button>();
        protected ConfigDialog()
        {
            super(viewer.getControl().getShell());
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("Configure columns");

            Composite composite = (Composite) super.createDialogArea(parent);

            UIUtils.createControlLabel(composite, "Select columns you want to display");

            Set<ColumnInfo> orderedList = new TreeSet<ColumnInfo>(new Comparator<ColumnInfo>() {
                @Override
                public int compare(ColumnInfo o1, ColumnInfo o2)
                {
                    return o1.order - o2.order;
                }
            });
            orderedList.addAll(columns);
            for (ColumnInfo columnInfo : orderedList) {
                Button check = new Button(composite, SWT.CHECK);
                check.setText(columnInfo.name);
                check.setSelection(columnInfo.visible);
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

}
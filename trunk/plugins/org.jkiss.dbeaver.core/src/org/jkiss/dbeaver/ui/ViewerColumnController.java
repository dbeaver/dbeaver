package org.jkiss.dbeaver.ui;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Tree/table viewer column controller
 */
public class ViewerColumnController {

    private static class ColumnInfo {
        final String name;
        final String description;
        final int style;
        final boolean defaultVisible;
        final boolean required;
        final CellLabelProvider labelProvider;

        boolean visible;
        int order;
        ViewerColumn column;

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
    }

    private final String id;
    private final ColumnViewer viewer;
    private boolean columnsMovable = false;
    private final List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

    public ViewerColumnController(String id, ColumnViewer viewer)
    {
        this.id = id;
        this.viewer = viewer;
    }

    public void addColumn(String name, String description, int style, boolean defaultVisible, boolean required, CellLabelProvider labelProvider)
    {
        columns.add(
            new ColumnInfo(name, description, style, defaultVisible, required, labelProvider, columns.size()));
    }

    public void createColumns()
    {
        readColumnsConfiguration();
        createVisibleColumns();

        viewer.getControl().addControlListener(new ControlAdapter() {
            boolean resized = false;
            @Override
            public void controlResized(ControlEvent e)
            {
                if (!resized) {
                    repackColumns();
                    resized = true;
                }
            }
        });
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
        for (ColumnInfo column : getVisibleColumns()) {
            if (viewer instanceof TreeViewer) {
                TreeViewerColumn item = new TreeViewerColumn((TreeViewer) viewer, column.style);
                item.getColumn().setText(column.name);
                item.getColumn().setMoveable(columnsMovable);
                if (!CommonUtils.isEmpty(column.description)) {
                    item.getColumn().setToolTipText(column.description);
                }
                item.setLabelProvider(column.labelProvider);
                column.column = item;
            } else if (viewer instanceof TableViewer) {
                TableViewerColumn item = new TableViewerColumn((TableViewer) viewer, column.style);
                item.getColumn().setText(column.name);
                item.getColumn().setMoveable(columnsMovable);
                if (!CommonUtils.isEmpty(column.description)) {
                    item.getColumn().setToolTipText(column.description);
                }
                item.setLabelProvider(column.labelProvider);
                column.column = item;
            }
        }
    }

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

    }

}
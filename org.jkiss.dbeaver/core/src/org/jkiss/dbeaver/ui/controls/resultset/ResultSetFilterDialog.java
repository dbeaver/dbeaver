/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.model.data.DBDColumnOrder;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;

public class ResultSetFilterDialog extends Dialog {

    private final ResultSetViewer resultSetViewer;

    private TableViewer columnsViewer;
    //private TableViewer filterViewer;
    private DBDDataFilter dataFilter;

    public ResultSetFilterDialog(ResultSetViewer resultSetViewer)
    {
        super(resultSetViewer.getControl().getShell());
        this.resultSetViewer = resultSetViewer;
        this.dataFilter = new DBDDataFilter(resultSetViewer.getDataFilter());
    }

    protected boolean isResizable() {
    	return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Result Set Order/Filter Settings");
        getShell().setImage(DBIcon.FILTER.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        Composite group = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        //layout.
        group.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        //gd.widthHint = getParentShell().getBounds().width - 200;
        group.setLayoutData(gd);

        TableColumn criteriaColumn;
        {
            Group columnsGroup = UIUtils.createControlGroup(group, "Columns", 1, GridData.FILL_BOTH, 0);

            columnsViewer = new TableViewer(columnsGroup, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
            columnsViewer.setContentProvider(new ListContentProvider());
            columnsViewer.setLabelProvider(new ColumnLabelProvider());
            Table columnsTable = columnsViewer.getTable();
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            //gd.heightHint = 300;
            columnsTable.setLayoutData(gd);
            columnsTable.setHeaderVisible(true);
            columnsTable.setLinesVisible(true);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Column");
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Order");
            criteriaColumn = UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Criteria");

            //columnsTable.addListener(SWT.PaintItem, new ColumnPaintListener());
            TableEditor tableEditor = new TableEditor(columnsTable);
        }

        {
            Group filterGroup = UIUtils.createControlGroup(group, "Custom", 2, GridData.FILL_BOTH, 0);

            UIUtils.createLabelText(filterGroup, "Order by", "");
            UIUtils.createLabelText(filterGroup, "Where", "");
/*
            filterViewer = new TableViewer(filterGroup, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
            filterViewer.setContentProvider(new ListContentProvider());
            Table filterTable = filterViewer.getTable();
            filterTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            filterTable.setHeaderVisible(true);
            filterTable.setLinesVisible(true);
            UIUtils.createTableColumn(filterTable, SWT.LEFT, "Num");
            UIUtils.createTableColumn(filterTable, SWT.LEFT, "Text");
*/
            if (!resultSetViewer.supportsDataFilter()) {
                filterGroup.setEnabled(false);
                ControlEnableState.disable(filterGroup);
                //UIUtils.
            }
        }

        // Fill columns
        columnsViewer.setInput(resultSetViewer.getMetaColumns());

        // Pack UI
        UIUtils.packColumns(columnsViewer.getTable());
        //UIUtils.packColumns(filterViewer.getTable());

        if (criteriaColumn.getWidth() < 200) {
            criteriaColumn.setWidth(200);
        }

        return parent;
    }

    @Override
    public int open()
    {
        return super.open();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
    }

    class ColumnLabelProvider extends LabelProvider implements ITableLabelProvider
    {
        public Image getColumnImage(Object element, int columnIndex)
        {
            DBDColumnBinding column = (DBDColumnBinding) element;
            if (columnIndex == 0 && column.getColumn() instanceof IObjectImageProvider) {
                return ((IObjectImageProvider)column.getColumn()).getObjectImage(); 
            }
            if (columnIndex == 1) {
                DBDColumnOrder orderColumn = dataFilter.getOrderColumn(column.getColumn().getName());
                if (orderColumn != null) {
                    return orderColumn.isDescending() ? DBIcon.SORT_DECREASE.getImage() : DBIcon.SORT_INCREASE.getImage();
                }
            }
            return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            DBDColumnBinding column = (DBDColumnBinding) element;
            switch (columnIndex) {
                case 0: return column.getColumn().getName();
                case 1: {
                    int orderColumnIndex = dataFilter.getOrderColumnIndex(column.getColumn().getName());
                    if (orderColumnIndex >= 0) {
                        return String.valueOf(orderColumnIndex + 1);
                    } else {
                        return "";
                    }
                }
                default: return "";
            }
        }

    }

    class ColumnPaintListener implements Listener {

        public void handleEvent(Event event) {
            Table table = columnsViewer.getTable();
            if (table.isDisposed()) {
                return;
            }
            switch(event.type) {
                case SWT.PaintItem: {
                    if (event.index == 1) {
                        TableItem tableItem = (TableItem) event.item;
                        DBDColumnBinding columnBinding = (DBDColumnBinding)tableItem.getData();
                        DBDColumnOrder orderColumn = dataFilter.getOrderColumn(columnBinding.getColumn().getName());
                        if (orderColumn != null) {
                            int columnWidth = table.getColumn(1).getWidth();
                            Image image = orderColumn.isDescending() ? DBIcon.SORT_DECREASE.getImage() : DBIcon.SORT_INCREASE.getImage();
                            event.gc.drawImage(image, event.x + (columnWidth - image.getBounds().width) / 2, event.y);
                            event.doit = false;
                        }
                    }
                    break;
                }
            }
        }
    }


}
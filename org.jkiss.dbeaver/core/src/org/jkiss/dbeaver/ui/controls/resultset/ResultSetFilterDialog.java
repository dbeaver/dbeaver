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
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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
        //gd.verticalIndent = 0;
        //gd.widthHint = getParentShell().getBounds().width - 200;
        group.setLayoutData(gd);

        TableColumn criteriaColumn;
        {
            Group columnsGroup = UIUtils.createControlGroup(group, "Columns", 1, GridData.FILL_BOTH, 0);

            columnsViewer = new TableViewer(columnsGroup, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
            columnsViewer.setContentProvider(new ListContentProvider());
            columnsViewer.setLabelProvider(new ColumnLabelProvider());
            final Table columnsTable = columnsViewer.getTable();
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
            final TableEditor tableEditor = new TableEditor(columnsTable);
            tableEditor.horizontalAlignment = SWT.CENTER;
            tableEditor.verticalAlignment = SWT.TOP;
            tableEditor.grabHorizontal = true;
            tableEditor.minimumWidth = 50;

            columnsTable.addMouseListener(new ColumnsMouseListener(tableEditor, columnsTable));
        }

        {
            Group filterGroup = UIUtils.createControlGroup(group, "Custom", 2, GridData.FILL_BOTH, 0);

            UIUtils.createLabelText(filterGroup, "Order by", "");
            UIUtils.createLabelText(filterGroup, "Where", "");

            if (!resultSetViewer.supportsDataFilter()) {
                filterGroup.setEnabled(false);
                ControlEnableState.disable(filterGroup);
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

        if (!resultSetViewer.supportsDataFilter()) {
            Label warnLabel = new Label(group, SWT.NONE);
            warnLabel.setText("Data filters and custom orderings are disabled for custom queries");
            warnLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
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

    @Override
    protected void okPressed()
    {
        resultSetViewer.setDataFilter(dataFilter);
        super.okPressed();
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
                DBDColumnOrder orderColumn = dataFilter.getOrderColumn(column.getColumnName());
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
                case 0: return column.getColumnName();
                case 1: {
                    int orderColumnIndex = dataFilter.getOrderColumnIndex(column.getColumnName());
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

/*
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
                        DBDColumnOrder orderColumn = dataFilter.getOrderColumn(columnBinding.getColumnName());
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
*/

    private class ColumnsMouseListener implements MouseListener {
        private final TableEditor tableEditor;
        private final Table columnsTable;

        public ColumnsMouseListener(TableEditor tableEditor, Table columnsTable)
        {
            this.tableEditor = tableEditor;
            this.columnsTable = columnsTable;
        }

        private void disposeOldEditor()
        {
            Control oldEditor = tableEditor.getEditor();
            if (oldEditor != null) oldEditor.dispose();
        }

        public void mouseDoubleClick(MouseEvent e)
        {
            handleColumnClick(e, true);
        }

        public void mouseDown(MouseEvent e)
        {
        }

        public void mouseUp(MouseEvent e)
        {
            handleColumnClick(e, false);
        }

        private void handleColumnClick(MouseEvent e, boolean isDef) {
            // Clean up any previous editor control
            disposeOldEditor();

            TableItem item = columnsTable.getItem(new Point(e.x, e.y));
            if (item == null) {
                return;
            }
            int columnIndex = UIUtils.getColumnAtPos(columnsTable, item, e.x, e.y);
            if (columnIndex <= 0) {
                return;
            }
            if (columnIndex == 1) {
                if (isDef) {
                    toggleColumnOrder(item);
                }
            } else if (columnIndex == 2 && resultSetViewer.supportsDataFilter()) {
                showEditor(item);
            }
        }

        private void toggleColumnOrder(TableItem item)
        {
            DBDColumnBinding column = (DBDColumnBinding) item.getData();
            DBDColumnOrder columnOrder = dataFilter.getOrderColumn(column.getColumnName());
            if (columnOrder == null) {
                dataFilter.addOrderColumn(new DBDColumnOrder(column.getColumnName(), column.getColumnIndex(), false));
            } else if (!columnOrder.isDescending()) {
                columnOrder.setDescending(true);
            } else {
                dataFilter.removeOrderColumn(columnOrder);
            }
            columnsViewer.refresh();
        }

        private void showEditor(TableItem item) {
            // Identify the selected row
            Text text = new Text(columnsTable, SWT.BORDER);
            text.setText(item.getText(2));
            text.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    Text text = (Text) tableEditor.getEditor();
                    tableEditor.getItem().setText(2, text.getText());
                }
            });
            text.selectAll();
            text.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e)
                {
                    if (e.keyCode == SWT.CR) {
                        e.doit = false;
                    }
                }
            });
            //if (isDef) {
                // Selected by mouse
                text.setFocus();
            //}
            tableEditor.setEditor(text, item, 2);
        }
    }
}
/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.query.DBQCondition;
import org.jkiss.dbeaver.model.data.query.DBQOrderColumn;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.utils.CommonUtils;

class ResultSetFilterDialog extends HelpEnabledDialog {

    private final ResultSetViewer resultSetViewer;

    private TableViewer columnsViewer;
    //private TableViewer filterViewer;
    private DBDDataFilter dataFilter;
    private Text orderText;
    private Text whereText;

    public ResultSetFilterDialog(ResultSetViewer resultSetViewer)
    {
        super(resultSetViewer.getControl().getShell(), IHelpContextIds.CTX_DATA_FILTER);
        this.resultSetViewer = resultSetViewer;
        this.dataFilter = new DBDDataFilter(resultSetViewer.getModel().getDataFilter());
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.controls_resultset_filter_title);
        getShell().setImage(DBIcon.FILTER.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        TableColumn criteriaColumn;
        {
            Composite columnsGroup = UIUtils.createPlaceholder(tabFolder, 1);

            columnsViewer = new TableViewer(columnsGroup, SWT.SINGLE | SWT.FULL_SELECTION);
            columnsViewer.setContentProvider(new ListContentProvider());
            columnsViewer.setLabelProvider(new ColumnLabelProvider());
            final Table columnsTable = columnsViewer.getTable();
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            //gd.heightHint = 300;
            columnsTable.setLayoutData(gd);
            columnsTable.setHeaderVisible(true);
            columnsTable.setLinesVisible(true);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.controls_resultset_filter_column_column);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.controls_resultset_filter_column_order);
            criteriaColumn = UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.controls_resultset_filter_column_criteria);

            //columnsTable.addListener(SWT.PaintItem, new ColumnPaintListener());
            final TableEditor tableEditor = new TableEditor(columnsTable);
            tableEditor.horizontalAlignment = SWT.CENTER;
            tableEditor.verticalAlignment = SWT.TOP;
            tableEditor.grabHorizontal = true;
            tableEditor.minimumWidth = 50;

            ColumnsMouseListener mouseListener = new ColumnsMouseListener(tableEditor, columnsTable);
            columnsTable.addMouseListener(mouseListener);
            columnsTable.addTraverseListener(
                new UIUtils.ColumnTextEditorTraverseListener(columnsTable, tableEditor, 2, mouseListener));


            TabItem libsTab = new TabItem(tabFolder, SWT.NONE);
            libsTab.setText(CoreMessages.controls_resultset_filter_group_columns);
            libsTab.setToolTipText("Set criteria and order for individual column(s)");
            libsTab.setControl(columnsGroup);
        }

        {
            Composite filterGroup = new Composite(tabFolder, SWT.NONE);
            filterGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            filterGroup.setLayout(new GridLayout(1, false));

            UIUtils.createControlLabel(filterGroup, CoreMessages.controls_resultset_filter_label_orderby);
            orderText = new Text(filterGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            orderText.setLayoutData(new GridData(GridData.FILL_BOTH));
            if (dataFilter.getOrder() != null) {
                orderText.setText(dataFilter.getOrder());
            }

            UIUtils.createControlLabel(filterGroup, CoreMessages.controls_resultset_filter_label_where);
            whereText = new Text(filterGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            whereText.setLayoutData(new GridData(GridData.FILL_BOTH));
            if (dataFilter.getWhere() != null) {
                whereText.setText(dataFilter.getWhere());
            }

            if (!resultSetViewer.supportsDataFilter()) {
                filterGroup.setEnabled(false);
                ControlEnableState.disable(filterGroup);
            }

            TabItem libsTab = new TabItem(tabFolder, SWT.NONE);
            libsTab.setText(CoreMessages.controls_resultset_filter_group_custom);
            libsTab.setToolTipText("Set custom criteria and order for whole query");
            libsTab.setControl(filterGroup);
        }

        // Fill columns
        columnsViewer.setInput(resultSetViewer.getModel().getVisibleColumns());

        // Pack UI
        UIUtils.packColumns(columnsViewer.getTable());
        //UIUtils.packColumns(filterViewer.getTable());

        if (criteriaColumn.getWidth() < 200) {
            criteriaColumn.setWidth(200);
        }

        if (!resultSetViewer.supportsDataFilter()) {
            Label warnLabel = new Label(composite, SWT.NONE);
            warnLabel.setText(CoreMessages.controls_resultset_filter_warning_custom_order_disabled);
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
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        createButton(parent, IDialogConstants.ABORT_ID, CoreMessages.controls_resultset_filter_button_reset, false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.ABORT_ID) {
            dataFilter.clearOrderColumns();
            dataFilter.clearFilterColumns();
            dataFilter.setOrder(null);
            dataFilter.setWhere(null);

            columnsViewer.setInput(resultSetViewer.getModel().getVisibleColumns());
            orderText.setText(""); //$NON-NLS-1$
            whereText.setText(""); //$NON-NLS-1$
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void okPressed()
    {
        if (!CommonUtils.isEmpty(orderText.getText())) {
            dataFilter.setOrder(orderText.getText());
        } else {
            dataFilter.setOrder(null);
        }
        if (!CommonUtils.isEmpty(whereText.getText())) {
            dataFilter.setWhere(whereText.getText());
        } else {
            dataFilter.setWhere(null);
        }
        resultSetViewer.setDataFilter(dataFilter);
        super.okPressed();
    }

    class ColumnLabelProvider extends LabelProvider implements ITableLabelProvider
    {
        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            DBDAttributeBinding column = (DBDAttributeBinding) element;
            if (columnIndex == 0 && column.getMetaAttribute() instanceof IObjectImageProvider) {
                return ((IObjectImageProvider)column.getMetaAttribute()).getObjectImage();
            }
            if (columnIndex == 1) {
                DBQOrderColumn orderColumn = dataFilter.getOrderColumn(column.getAttributeName());
                if (orderColumn != null) {
                    return orderColumn.isDescending() ? DBIcon.SORT_DECREASE.getImage() : DBIcon.SORT_INCREASE.getImage();
                }
            }
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            DBDAttributeBinding column = (DBDAttributeBinding) element;
            switch (columnIndex) {
                case 0: return column.getAttributeName();
                case 1: {
                    int orderColumnIndex = dataFilter.getOrderColumnIndex(column.getAttributeName());
                    if (orderColumnIndex >= 0) {
                        return String.valueOf(orderColumnIndex + 1);
                    } else {
                        return ""; //$NON-NLS-1$
                    }
                }
                case 2: {
                    DBQCondition filterColumn = dataFilter.getFilterColumn(column.getAttributeName());
                    if (filterColumn != null) {
                        return filterColumn.getCondition();
                    } else {
                        return ""; //$NON-NLS-1$
                    }
                }
                default: return ""; //$NON-NLS-1$
            }
        }

    }

    private class ColumnsMouseListener extends MouseAdapter implements UIUtils.TableEditorController {
        private final TableEditor tableEditor;
        private final Table columnsTable;

        public ColumnsMouseListener(TableEditor tableEditor, Table columnsTable)
        {
            this.tableEditor = tableEditor;
            this.columnsTable = columnsTable;
        }

        @Override
        public void mouseUp(MouseEvent e)
        {
            // Clean up any previous editor control
            closeEditor(tableEditor);

            TableItem item = columnsTable.getItem(new Point(e.x, e.y));
            if (item == null) {
                return;
            }
            int columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
            if (columnIndex <= 0) {
                return;
            }
            if (columnIndex == 1) {
                //if (isDef) {
                    toggleColumnOrder(item);
                //}
            } else if (columnIndex == 2 && resultSetViewer.supportsDataFilter()) {
                showEditor(item);
            }
        }

        private void toggleColumnOrder(TableItem item)
        {
            DBDAttributeBinding column = (DBDAttributeBinding) item.getData();
            DBQOrderColumn columnOrder = dataFilter.getOrderColumn(column.getAttributeName());
            if (columnOrder == null) {
                dataFilter.addOrderColumn(new DBQOrderColumn(column.getAttributeName(), false));
            } else if (!columnOrder.isDescending()) {
                columnOrder.setDescending(true);
            } else {
                dataFilter.removeOrderColumn(columnOrder);
            }
            columnsViewer.refresh();
        }

        @Override
        public void showEditor(final TableItem item) {
            // Identify the selected row
            Text text = new Text(columnsTable, SWT.BORDER);
            text.setText(item.getText(2));
            text.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    Text text = (Text) tableEditor.getEditor();
                    String criteria = text.getText().trim();
                    DBDAttributeBinding column = (DBDAttributeBinding) item.getData();
                    DBQCondition filterColumn = dataFilter.getFilterColumn(column.getAttributeName());
                    if (CommonUtils.isEmpty(criteria)) {
                        if (filterColumn != null) {
                            dataFilter.removeFilterColumn(filterColumn);
                        }
                    } else {
                        if (filterColumn != null) {
                            filterColumn.setCondition(criteria);
                        } else {
                            dataFilter.addFilterColumn(new DBQCondition(column.getAttributeName(), criteria));
                        }
                    }
                    tableEditor.getItem().setText(2, criteria);
                }
            });
            text.selectAll();

            // Selected by mouse
            text.setFocus();

            tableEditor.setEditor(text, item, 2);
        }

        @Override
        public void closeEditor(TableEditor tableEditor)
        {
            Control oldEditor = this.tableEditor.getEditor();
            if (oldEditor != null) oldEditor.dispose();
        }

    }
}
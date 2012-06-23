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
package org.jkiss.dbeaver.ui.dialogs.struct;

import org.eclipse.osgi.util.NLS;
import org.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ColumnsSelectorDialog
 *
 * @author Serge Rider
 */
public abstract class ColumnsSelectorDialog extends Dialog {

    private String title;
    private DBNDatabaseNode tableNode;
    private Table columnsTable;
    private List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

    private static class ColumnInfo {
        TableItem item;
        DBNDatabaseNode columnNode;
        int position;

        public ColumnInfo(TableItem columnItem, DBNDatabaseNode columnNode)
        {
            this.item = columnItem;
            this.columnNode = columnNode;
            this.position = -1;
        }
    }

    public ColumnsSelectorDialog(
        Shell shell,
        String title,
        DBSTable table) {
        super(shell);
        setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        this.title = title;
        this.tableNode = DBeaverCore.getInstance().getNavigatorModel().findNode(table);
        Assert.isNotNull(this.tableNode);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        final Composite panel = UIUtils.createPlaceholder(dialogGroup, 1);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            final Composite tableGroup = new Composite(panel, SWT.NONE);
            tableGroup.setLayout(new GridLayout(2, false));
            tableGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            UIUtils.createLabelText(tableGroup, CoreMessages.dialog_struct_columns_select_label_table, tableNode.getNodeName(), SWT.BORDER | SWT.READ_ONLY);

            createContentsBeforeColumns(tableGroup);
        }

        {
            Composite columnsGroup = UIUtils.createControlGroup(panel, CoreMessages.dialog_struct_columns_select_group_columns, 1, GridData.FILL_BOTH, 0);
            columnsTable = new Table(columnsGroup, SWT.BORDER | SWT.SINGLE | SWT.CHECK);
            columnsTable.setHeaderVisible(true);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 200;
            gd.heightHint = 200;
            columnsTable.setLayoutData(gd);
            columnsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    handleItemSelect((TableItem) e.item);
                }
            });
            TableColumn colName = new TableColumn(columnsTable, SWT.NONE);
            colName.setText(CoreMessages.dialog_struct_columns_select_column);
            colName.setWidth(170);

            TableColumn colPosition = new TableColumn(columnsTable, SWT.CENTER);
            colPosition.setText("#"); //$NON-NLS-1$
            colPosition.setWidth(30);
        }
        createContentsAfterColumns(panel);

        // Collect columns
        final List<DBNDatabaseNode> columnNodes = new ArrayList<DBNDatabaseNode>();
        try {
            DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        final List<DBNDatabaseNode> folders = tableNode.getChildren(monitor);
                        for (DBNDatabaseNode node : folders) {
                            if (node instanceof DBNContainer) {
                                final Class<?> itemsClass = ((DBNContainer) node).getChildrenClass();
                                if (itemsClass != null && DBSTableColumn.class.isAssignableFrom(itemsClass)) {
                                    final List<DBNDatabaseNode> children = node.getChildren(monitor);
                                    if (!CommonUtils.isEmpty(children)) {
                                        for (DBNDatabaseNode child : children) {
                                            if (child.getObject() instanceof DBSTableColumn) {
                                                columnNodes.add(child);
                                            }
                                        }
                                    }
                                }
                            } else if (node.getObject() instanceof DBSTableColumn) {
                                columnNodes.add(node);
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(
                    getShell(),
                    CoreMessages.dialog_struct_columns_select_error_load_columns_title,
                    CoreMessages.dialog_struct_columns_select_error_load_columns_message,
                    e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }

        for (DBNDatabaseNode columnNode : columnNodes) {
            TableItem columnItem = new TableItem(columnsTable, SWT.NONE);

            ColumnInfo col = new ColumnInfo(columnItem, columnNode);
            columns.add(col);

            columnItem.setImage(0, columnNode.getNodeIcon());
            columnItem.setText(0, columnNode.getNodeName());
            columnItem.setData(col);
        }
        //columnsTable.set

        return dialogGroup;
    }

    protected void createContentsBeforeColumns(Composite panel)
    {

    }

    protected void createContentsAfterColumns(Composite panel)
    {

    }

    private void handleItemSelect(TableItem item)
    {
        final ColumnInfo col = (ColumnInfo) item.getData();
        if (item.getChecked() && col.position < 0) {
            // Checked
            col.position = 0;
            for (ColumnInfo tmp : columns) {
                if (tmp != col && tmp.position >= col.position) {
                    col.position = tmp.position + 1;
                }
            }
            item.setText(1, String.valueOf(col.position + 1));
        } else if (!item.getChecked() && col.position >= 0) {
            // Unchecked
            item.setText(1, ""); //$NON-NLS-1$
            for (ColumnInfo tmp : columns) {
                if (tmp != col && tmp.position >= col.position) {
                    tmp.position--;
                    tmp.item.setText(1, String.valueOf(tmp.position + 1));
                }
            }
            col.position = -1;
        }
        boolean hasCheckedColumns = false;
        for (ColumnInfo tmp : columns) {
            if (tmp.position >= 0) {
                hasCheckedColumns = true;
                break;
            }
        }
        getButton(IDialogConstants.OK_ID).setEnabled(hasCheckedColumns);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(NLS.bind(CoreMessages.dialog_struct_columns_select_title, title, tableNode.getNodeName()));
        shell.setImage(tableNode.getNodeIcon());
    }

    public Collection<DBSTableColumn> getSelectedColumns()
    {
        List<DBSTableColumn> tableColumns = new ArrayList<DBSTableColumn>();
        for (ColumnInfo col : columns) {
            if (col.position >= 0) {
                tableColumns.add((DBSTableColumn) col.columnNode.getObject());
            }
        }
        return tableColumns;
    }

}

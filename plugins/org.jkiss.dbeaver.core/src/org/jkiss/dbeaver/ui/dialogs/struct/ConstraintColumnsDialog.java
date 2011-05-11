/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.struct;

import net.sf.jkiss.utils.CommonUtils;
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
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ConstraintColumnsDialog
 *
 * @author Serge Rider
 */
public class ConstraintColumnsDialog extends Dialog {

    private List<DBSConstraintType> constraintTypes;
    private DBSConstraintType selectedConstraintType;
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

    public ConstraintColumnsDialog(Shell shell, DBSTable table, Collection<DBSConstraintType> constraintTypes) {
        super(shell);
        setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        this.tableNode = DBeaverCore.getInstance().getNavigatorModel().findNode(table);
        Assert.isNotNull(this.tableNode);
        this.constraintTypes = new ArrayList<DBSConstraintType>(constraintTypes);
        Assert.isTrue(!CommonUtils.isEmpty(this.constraintTypes));
    }

    @Override
    public boolean close() {
        return super.close();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        final Composite panel = UIUtils.createPlaceholder(dialogGroup, 1);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            final Composite typeGroup = new Composite(panel, SWT.NONE);
            typeGroup.setLayout(new GridLayout(2, false));
            typeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(typeGroup, "Type");
            final Combo typeCombo = new Combo(typeGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            for (DBSConstraintType constraintType : constraintTypes) {
                typeCombo.add(constraintType.getName());
                if (selectedConstraintType == null) {
                    selectedConstraintType = constraintType;
                }
            }
            typeCombo.select(0);
            typeCombo.setEnabled(constraintTypes.size() > 1);
            typeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    selectedConstraintType = constraintTypes.get(typeCombo.getSelectionIndex());
                }
            });
        }
        {
            Composite columnsGroup = UIUtils.createControlGroup(panel, "Columns", 1, GridData.FILL_BOTH, 0);
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
            colName.setText("Column");
            colName.setWidth(170);

            TableColumn colPosition = new TableColumn(columnsTable, SWT.CENTER);
            colPosition.setText("#");
            colPosition.setWidth(30);
        }

        // Load columns
        final List<DBNDatabaseNode> columnNodes = new ArrayList<DBNDatabaseNode>();
        try {
            DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        final List<DBNDatabaseNode> folders = tableNode.getChildren(monitor);
                        for (DBNDatabaseNode node : folders) {
                            if (node instanceof DBNContainer && DBSTableColumn.class.isAssignableFrom(((DBNContainer) node).getItemsClass())) {
                                final List<DBNDatabaseNode> children = node.getChildren(monitor);
                                if (!CommonUtils.isEmpty(children)) {
                                    for (DBNDatabaseNode child : children) {
                                        if (child.getObject() instanceof DBSTableColumn) {
                                            columnNodes.add(child);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getShell(), "Load columns", "Error loading table columns", e.getTargetException());
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
            item.setText(1, "");
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

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(tableNode.getNodeName() + " Constraint Columns");
    }

    public Collection<DBSTableColumn> getConstraintColumns()
    {
        List<DBSTableColumn> tableColumns = new ArrayList<DBSTableColumn>();
        for (ColumnInfo col : columns) {
            if (col.position >= 0) {
                tableColumns.add((DBSTableColumn) col.columnNode.getObject());
            }
        }
        return tableColumns;
    }

    public DBSConstraintType getConstraintType()
    {
        return selectedConstraintType;
    }

}

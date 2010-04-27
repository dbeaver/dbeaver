/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;

/**
 * Column info panel.
 */
public class ColumnInfoPanel extends Composite {

    public ColumnInfoPanel(Composite parent, int style, DBDValueController valueController) {
        super(parent, style);
        this.createPanel(valueController);
    }

    protected void createPanel(final DBDValueController valueController)
    {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        this.setLayoutData(gd);

        GridLayout layout = new GridLayout(1, false);
        layout.marginTop = 0;
        layout.marginWidth = 0;
        this.setLayout(layout);
        {
            Label label = new Label(this, SWT.NONE);
            label.setText("Value Info: ");

            InfoTreePanel infoTree = new InfoTreePanel(this, SWT.NONE) {
                protected void createItems(Tree infoTree) {

                    TreeItem tableNameitem = new TreeItem(infoTree, SWT.NONE);
                    tableNameitem.setText(new String[] { "Table Name", valueController.getColumnMetaData().getTableName() });

                    TreeItem columnNameitem = new TreeItem(infoTree, SWT.NONE);
                    columnNameitem.setText(new String[] { "Column Name", valueController.getColumnMetaData().getColumnName() });

                    TreeItem columnTypeItem = new TreeItem(infoTree, SWT.NONE);
                    columnTypeItem.setText(new String[] { "Column Type", valueController.getColumnMetaData().getTypeName() });

                    createInfoItems(infoTree, valueController);

                    TreeItem keyItem = new TreeItem(infoTree, SWT.NONE);
                    keyItem.setText(new String[] { "Key", valueController.getValueLocator().getUniqueKey().getConstraintType().name() });
                    {
                        java.util.List<? extends DBCColumnMetaData> keyColumns = valueController.getValueLocator().getKeyColumns();

                        TreeItem keyNameitem = new TreeItem(keyItem, SWT.NONE);
                        keyNameitem.setText(new String[] { "Name", valueController.getValueLocator().getUniqueKey().getName() });

                        TreeItem columnsItem = new TreeItem(keyItem, SWT.NONE);
                        columnsItem.setText(new String[] { "Columns", String.valueOf(keyColumns.size()) });

                        for (DBCColumnMetaData keyColumn : keyColumns) {

                            TreeItem columnItem = new TreeItem(columnsItem, SWT.NONE);
                            String columnName = keyColumn.getColumnName();

                            Object keyValue = valueController.getColumnValue(keyColumn);
                            String strValue = keyValue == null ? "[NULL" : keyValue.toString();

                            columnItem.setText(0, columnName);
                            columnItem.setText(1, strValue);
                        }
                        columnsItem.setExpanded(true);
                    }
                    keyItem.setExpanded(false);
                }
            };
            infoTree.createControl();

        }
        createInfoGroups(this, valueController);
    }

    protected void createInfoItems(Tree infoTree, DBDValueController valueController)
    {
        TreeItem columnTypeItem = new TreeItem(infoTree, SWT.NONE);
        columnTypeItem.setText(new String[] {
            "Column Size",
            String.valueOf(valueController.getColumnMetaData().getDisplaySize()) });
    }

    protected int createInfoGroups(Composite infoGroup, DBDValueController valueController)
    {
        return 0;
    }
}

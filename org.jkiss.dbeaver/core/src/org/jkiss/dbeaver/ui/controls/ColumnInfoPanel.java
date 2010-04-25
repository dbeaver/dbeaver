/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
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

    protected void createPanel(DBDValueController valueController)
    {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        this.setLayoutData(gd);

        {
            Group infoGroup = new Group(this, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            infoGroup.setLayoutData(gd);
            infoGroup.setLayout(new GridLayout(2, false));
            infoGroup.setText("Column info");

            Label label = new Label(infoGroup, SWT.NONE);
            label.setText("Table Name: ");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 50;
            Text text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getColumnMetaData().getTableName());
            text.setLayoutData(gd);

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Name: ");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 50;
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getColumnMetaData().getColumnName());
            text.setLayoutData(gd);

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Type: ");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 50;
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getColumnMetaData().getTypeName());
            text.setLayoutData(gd);

            createInfoControls(infoGroup, valueController);
        }
        {
            Group idGroup = new Group(this, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            idGroup.setLayoutData(gd);
            idGroup.setLayout(new GridLayout(2, false));
            idGroup.setText("Key info");

            Label label = new Label(idGroup, SWT.NONE);
            label.setText("Key: ");

            Text text = new Text(idGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getValueLocator().getUniqueKey().getName());
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 50;
            text.setLayoutData(gd);

            label = new Label(idGroup, SWT.NONE);
            label.setText("Columns: ");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            label.setLayoutData(gd);

            Table keyColumnTable = new Table(idGroup, SWT.BORDER| SWT.FULL_SELECTION);
            //keyColumnTable.setHeaderVisible(true);
            keyColumnTable.setLinesVisible(true);
            TableCursor cursor = new TableCursor(keyColumnTable, SWT.NONE);

            TableColumn nameColumn = new TableColumn(keyColumnTable, SWT.LEFT);
            nameColumn.setText("Name");

            TableColumn valueColumn = new TableColumn(keyColumnTable, SWT.LEFT);
            valueColumn.setText("Value");

            int maxNameWidth = 0, maxValueWidth = 0;
            GC gc = new GC(this);
            gc.setFont(keyColumnTable.getFont());
            java.util.List<? extends DBCColumnMetaData> keyColumns = valueController.getValueLocator().getKeyColumns();
            for (DBCColumnMetaData keyColumn : keyColumns) {

                TableItem keyItem = new TableItem(keyColumnTable, SWT.NONE);
                String columnName = keyColumn.getColumnName();
                keyItem.setText(0, columnName);

                Object keyValue = valueController.getColumnValue(keyColumn);
                String strValue = keyValue == null ? "[NULL" : keyValue.toString();
                keyItem.setText(1, strValue);

                maxNameWidth = Math.max(maxNameWidth, gc.stringExtent(columnName).x);
                maxValueWidth = Math.max(maxValueWidth, gc.stringExtent(strValue).x);
            }

            int maxColumnWidth = 300;
            nameColumn.pack();
            nameColumn.setWidth(Math.min(maxColumnWidth, nameColumn.getWidth() + 20));
            valueColumn.pack();
            valueColumn.setWidth(Math.min(maxColumnWidth, valueColumn.getWidth() + 20));

            gd = new GridData(GridData.FILL_BOTH);
            gd.minimumWidth = 100;// + maxNameWidth + maxValueWidth;
            gd.widthHint = nameColumn.getWidth() + valueColumn.getWidth();
            gd.horizontalSpan = 2;
            keyColumnTable.setLayoutData(gd);
        }
        int extraGroupsNum = createInfoGroups(this, valueController);

        this.setLayout(new GridLayout(2 + extraGroupsNum, false));
    }

    protected void createInfoControls(Composite infoGroup, DBDValueController valueController)
    {
        Label label = new Label(infoGroup, SWT.NONE);
        label.setText("Column Size: ");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 50;
        Text text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
        text.setText(String.valueOf(valueController.getColumnMetaData().getDisplaySize()));
        text.setLayoutData(gd);
    }

    protected int createInfoGroups(Composite infoGroup, DBDValueController valueController)
    {
        return 0;
    }
}

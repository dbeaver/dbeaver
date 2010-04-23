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

            java.util.List<? extends DBCColumnMetaData> keyColumns = valueController.getValueLocator().getKeyColumns();
            for (DBCColumnMetaData keyColumn : keyColumns) {
                Label label = new Label(idGroup, SWT.NONE);
                label.setText(keyColumn.getColumnName() + ":");

                Text text = new Text(idGroup, SWT.BORDER | SWT.READ_ONLY);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.minimumWidth = 50;
                text.setLayoutData(gd);
                Object keyValue = valueController.getColumnValue(keyColumn);
                String strValue = keyValue == null ? "[NULL" : keyValue.toString();
                text.setText(strValue);
            }

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

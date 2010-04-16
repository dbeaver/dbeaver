/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.util.*;
import java.util.List;

/**
 * ValueViewDialog
 *
 * @author Serge Rider
 */
public abstract class ValueViewDialog extends Dialog {

    private DBDValueController valueController;

    protected ValueViewDialog(DBDValueController valueController) {
        super(valueController.getValueSite().getShell());
        setShellStyle(SWT.SHELL_TRIM);
        this.valueController = valueController;
    }

    public DBDValueController getValueController() {
        return valueController;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Composite valueInfoGroup = new Composite(dialogGroup, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        valueInfoGroup.setLayoutData(gd);
        valueInfoGroup.setLayout(new GridLayout(2, false));

        {
            Group infoGroup = new Group(valueInfoGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            infoGroup.setLayoutData(gd);
            infoGroup.setLayout(new GridLayout(2, false));
            infoGroup.setText("Column info");

            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 50;

            Label label = new Label(infoGroup, SWT.NONE);
            label.setText("Table Name: ");
            Text text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getColumnMetaData().getTableName());
            text.setLayoutData(gd);

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Name: ");
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getColumnMetaData().getColumnName());
            text.setLayoutData(gd);

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Type: ");
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getColumnMetaData().getTypeName());
            text.setLayoutData(gd);

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Size: ");
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(String.valueOf(valueController.getColumnMetaData().getDisplaySize()));
            text.setLayoutData(gd);
        }
        {
            Group idGroup = new Group(valueInfoGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            idGroup.setLayoutData(gd);
            idGroup.setLayout(new GridLayout(2, false));
            idGroup.setText("Key info");

            List<? extends DBCColumnMetaData> keyColumns = this.valueController.getValueLocator().getKeyColumns();
            for (DBCColumnMetaData keyColumn : keyColumns) {
                Label label = new Label(idGroup, SWT.NONE);
                label.setText(keyColumn.getColumnName() + ":");

                Text text = new Text(idGroup, SWT.BORDER | SWT.READ_ONLY);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.minimumWidth = 50;
                text.setLayoutData(gd);
                Object keyValue = this.valueController.getColumnValue(keyColumn);
                String strValue = keyValue == null ? "[NULL" : keyValue.toString();
                text.setText(strValue);
            }

        }

        return dialogGroup;
    }

    @Override
    protected void okPressed()
    {
        try {
            applyChanges();

            super.okPressed();
        }
        catch (Exception e) {
            DBeaverUtils.showErrorDialog(getShell(), "Error updating column", "Could not update column value", e);
            super.cancelPressed();
        }
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        DBCColumnMetaData meta = valueController.getColumnMetaData();
        shell.setText(meta.getTableName() + "." + meta.getColumnName());
    }

    protected abstract void applyChanges();
}

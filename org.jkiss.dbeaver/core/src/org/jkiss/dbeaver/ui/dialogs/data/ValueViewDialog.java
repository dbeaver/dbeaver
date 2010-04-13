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

/**
 * ValueViewDialog
 *
 * @author Serge Rider
 */
public abstract class ValueViewDialog extends Dialog {

    private DBDValueController valueController;
    private Composite infoGroup;

    protected ValueViewDialog(DBDValueController valueController) {
        super(valueController.getValueSite().getShell());
        setShellStyle(SWT.SHELL_TRIM);
        this.valueController = valueController;
    }

    public DBDValueController getValueController() {
        return valueController;
    }

    public Composite getInfoGroup() {
        return infoGroup;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        {
            infoGroup = new Composite(dialogGroup, SWT.NONE);
            GridData gd = new GridData();
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            infoGroup.setLayoutData(gd);
            infoGroup.setLayout(new GridLayout(2, false));

            Label label = new Label(infoGroup, SWT.NONE);
            label.setText("Table Name: ");
            Text text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getColumnMetaData().getTableName());

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Name: ");
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getColumnMetaData().getColumnName());

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Type: ");
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(valueController.getColumnMetaData().getTypeName());

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Size: ");
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(String.valueOf(valueController.getColumnMetaData().getDisplaySize()));
        }

        return dialogGroup;
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        DBCColumnMetaData meta = valueController.getColumnMetaData();
        shell.setText(meta.getTableName() + "." + meta.getColumnName());
    }

}

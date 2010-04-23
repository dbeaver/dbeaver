/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.util.List;

/**
 * ValueViewDialog
 *
 * @author Serge Rider
 */
public abstract class ValueViewDialog extends Dialog implements DBDValueEditor {

    private static int dialogCount = 0;

    private DBDValueController valueController;

    protected ValueViewDialog(DBDValueController valueController) {
        super(valueController.getValueSite().getShell());
        setShellStyle(SWT.SHELL_TRIM);
        this.valueController = valueController;
        this.valueController.registerEditor(this);
        dialogCount++;
    }

    public DBDValueController getValueController() {
        return valueController;
    }

    public void showValueEditor() {
        getShell().setFocus();
    }

    public void closeValueEditor() {
        this.setReturnCode(CANCEL);
        this.close();
    }

    @Override
    public boolean close() {
        dialogCount--;
        if (this.valueController != null) {
            this.valueController.unregisterEditor(this);
            this.valueController = null;
        }
        return super.close();
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

            createInfoControls(infoGroup);
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

    protected void createInfoControls(Composite infoGroup)
    {
        Label label = new Label(infoGroup, SWT.NONE);
        label.setText("Column Size: ");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 50;
        Text text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
        text.setText(String.valueOf(valueController.getColumnMetaData().getDisplaySize()));
        text.setLayoutData(gd);
    }

    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, IDialogConstants.OK_ID, "&Save", true);
        createButton(parent, IDialogConstants.IGNORE_ID, "Set &NULL", false);
        createButton(parent, IDialogConstants.CANCEL_ID, "&Cancel", false);
    }

    protected void initializeBounds()
    {
        super.initializeBounds();

        Shell shell = getShell();
        Monitor primary = shell.getMonitor();
        Rectangle bounds = primary.getBounds ();
        Rectangle rect = shell.getBounds ();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 3;
        x += dialogCount * 20;
        y += dialogCount * 20;
        shell.setLocation (x, y);
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

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.IGNORE_ID) {
            getValueController().updateValue(null);
            super.okPressed();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        DBCColumnMetaData meta = valueController.getColumnMetaData();
        shell.setText(meta.getTableName() + "." + meta.getColumnName());
    }

    protected abstract void applyChanges();
}

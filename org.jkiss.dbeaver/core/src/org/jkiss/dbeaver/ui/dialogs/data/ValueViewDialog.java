/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.ui.controls.ColumnInfoPanel;

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

        new ColumnInfoPanel(dialogGroup, SWT.NONE, getValueController()) {
            @Override
            protected void createInfoItems(Tree infoTree, DBDValueController valueController) {
                ValueViewDialog.this.createInfoControls(infoTree);
            }
        };

        return dialogGroup;
    }

    protected void createInfoControls(Tree infoTree)
    {
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

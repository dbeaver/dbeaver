/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.data.DBDValueController;

/**
 * TextViewDialog
 */
public class TextViewDialog extends ValueViewDialog {

    private Text textEdit;

    public TextViewDialog(DBDValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Object value = getValueController().getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText("Value: ");

        int style = SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP;
        if (getValueController().isReadOnly()) {
            style |= SWT.READ_ONLY;
        }
        textEdit = new Text(dialogGroup, style);

        textEdit.setText(value == null ? "" : value.toString());
        int maxSize = getValueController().getColumnMetaData().getDisplaySize();
        if (maxSize > 0) {
            textEdit.setTextLimit(maxSize);
        }
        textEdit.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 200;
        gd.widthHint = 300;
        gd.grabExcessVerticalSpace = true;
        textEdit.setLayoutData(gd);
        textEdit.setFocus();
        textEdit.setEditable(!getValueController().isReadOnly());
        return dialogGroup;
    }

    protected void createInfoControls(Tree infoTree)
    {
        TreeItem columnTypeItem = new TreeItem(infoTree, SWT.NONE);
        columnTypeItem.setText(new String[] {
            "Length",
            String.valueOf(getValueController().getColumnMetaData().getDisplaySize()) });
    }


    @Override
    protected void applyChanges()
    {
        getValueController().updateValue(textEdit.getText(), false);
    }

}

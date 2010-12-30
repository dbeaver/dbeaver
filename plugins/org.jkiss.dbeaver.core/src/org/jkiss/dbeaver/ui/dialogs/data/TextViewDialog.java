/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
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
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Object value = getValueController().getValue();
        boolean isForeignKey = super.isForeignKey();

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
        GridData gd = new GridData(isForeignKey ? GridData.FILL_HORIZONTAL : GridData.FILL_BOTH);
        gd.widthHint = 300;
        if (!isForeignKey) {
            gd.heightHint = 200;
            gd.grabExcessVerticalSpace = true;
        }
        textEdit.setLayoutData(gd);
        textEdit.setFocus();
        textEdit.setEditable(!getValueController().isReadOnly());

        if (isForeignKey) {
            super.createEditorSelector(dialogGroup, textEdit);
        }

        return dialogGroup;
    }

    @Override
    protected Object getEditorValue()
    {
        return textEdit.getText();
    }

}

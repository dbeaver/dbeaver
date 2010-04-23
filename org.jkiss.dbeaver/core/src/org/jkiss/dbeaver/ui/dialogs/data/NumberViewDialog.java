/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.impl.data.JDBCNumberValueHandler;
import org.jkiss.dbeaver.ui.UIUtils;

import java.sql.Types;

/**
 * TextViewDialog
 */
public class NumberViewDialog extends ValueViewDialog {

    private Text textEdit;
    private Combo bitEdit;

    public NumberViewDialog(DBDValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Object value = getValueController().getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText("Value: ");

        int style = SWT.BORDER;
        if (getValueController().isReadOnly()) {
            style |= SWT.READ_ONLY;
        }
        if (getValueController().getColumnMetaData().getValueType() == Types.BIT) {
            // Bit (boolean)
            style |= SWT.READ_ONLY;
            bitEdit = new Combo(dialogGroup, style);
            bitEdit.add("0 (FALSE)");
            bitEdit.add("1 (TRUE)");
            int intValue = ((Number) value).intValue();
            bitEdit.select(intValue == 0 ? 0 : 1);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 50;
            gd.grabExcessVerticalSpace = true;
            bitEdit.setLayoutData(gd);
            bitEdit.setFocus();
        } else {
            // Numbers
            textEdit = new Text(dialogGroup, style);
            textEdit.addVerifyListener(
                getValueController().getColumnMetaData().getScale() == 0 ?
                    UIUtils.INTEGER_VERIFY_LISTENER :
                    UIUtils.NUMBER_VERIFY_LISTENER);

            textEdit.setText(value == null ? "" : value.toString());
            int maxSize = getValueController().getColumnMetaData().getPrecision();
            if (maxSize > 0) {
                textEdit.setTextLimit(maxSize + 2);
            }
            textEdit.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 300;
            gd.grabExcessVerticalSpace = true;
            textEdit.setLayoutData(gd);
            textEdit.setFocus();
        }
        return dialogGroup;
    }

    protected void createInfoControls(Composite infoGroup)
    {
        if (getValueController().getColumnMetaData().getValueType() == Types.BIT) {
            return;
        }
        Label label = new Label(infoGroup, SWT.NONE);
        label.setText("Precision: ");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 50;
        Text text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
        text.setText(String.valueOf(getValueController().getColumnMetaData().getPrecision()));
        text.setLayoutData(gd);

        label = new Label(infoGroup, SWT.NONE);
        label.setText("Scale: ");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 50;
        text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
        text.setText(String.valueOf(getValueController().getColumnMetaData().getScale()));
        text.setLayoutData(gd);
    }

    @Override
    protected void applyChanges()
    {
        if (textEdit != null) {
            getValueController().updateValue(
                JDBCNumberValueHandler.convertStringToNumber(
                    textEdit.getText(),
                    getValueController().getColumnMetaData()));
        } else if (bitEdit != null) {
            getValueController().updateValue((byte)bitEdit.getSelectionIndex());
        }
    }

}
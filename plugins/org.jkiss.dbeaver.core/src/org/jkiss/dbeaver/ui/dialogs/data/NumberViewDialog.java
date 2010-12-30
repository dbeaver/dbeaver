/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCNumberValueHandler;
import org.jkiss.dbeaver.ui.UIUtils;

import java.sql.Types;

/**
 * TextViewDialog
 */
public class NumberViewDialog extends ValueViewDialog {

    private Text textEdit;
    private Combo bitEdit;
    private boolean isBoolean = false;

    public NumberViewDialog(DBDValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        DBDValueController valueController = getValueController();
        Object value = valueController.getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText("Value: ");

        int style = SWT.BORDER;
        if (getValueController().isReadOnly()) {
            style |= SWT.READ_ONLY;
        }
        int valueType = getValueController().getColumnMetaData().getValueType();
        if (valueType == Types.BIT || valueType == Types.BOOLEAN) {
            // Bit (boolean)
            style |= SWT.READ_ONLY;
            bitEdit = new Combo(dialogGroup, style);
            if (valueType == Types.BOOLEAN || value instanceof Boolean) {
                isBoolean = true;
                bitEdit.add("FALSE");
                bitEdit.add("TRUE");
                Boolean boolValue = ((Boolean) value);
                bitEdit.select(Boolean.TRUE.equals(boolValue) ? 1 : 0);
            } else {
                bitEdit.add("0 (FALSE)");
                bitEdit.add("1 (TRUE)");
                int intValue = ((Number) value).intValue();
                bitEdit.select(intValue == 0 ? 0 : 1);
            }
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 50;
            gd.grabExcessVerticalSpace = true;
            bitEdit.setLayoutData(gd);
            bitEdit.setFocus();
            bitEdit.setEnabled(!getValueController().isReadOnly());
        } else {
            // Numbers
            textEdit = new Text(dialogGroup, style);
            textEdit.addVerifyListener(
                getValueController().getColumnMetaData().getScale() <= 0 ?
                    UIUtils.INTEGER_VERIFY_LISTENER :
                    UIUtils.NUMBER_VERIFY_LISTENER);

            if (value != null) {
                // Use simple toString() because we don't want formatted values.
                String textValue = value.toString();
                textEdit.setText(textValue);
            }
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
            textEdit.setEditable(!getValueController().isReadOnly());

            if (super.isForeignKey()) {
                super.createEditorSelector(dialogGroup, textEdit);
            }
        }
        return dialogGroup;
    }

    @Override
    protected Object getEditorValue()
    {
        if (textEdit != null) {
            return JDBCNumberValueHandler.convertStringToNumber(
                textEdit.getText(),
                getValueController().getColumnMetaData());
        } else if (bitEdit != null) {
            if (isBoolean) {
                return bitEdit.getSelectionIndex() == 0 ? Boolean.FALSE : Boolean.TRUE;
            } else {
                return (byte)bitEdit.getSelectionIndex();
            }
        } else {
            return null;
        }
    }

}
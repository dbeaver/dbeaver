/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
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
    private Object value;

    public NumberViewDialog(DBDValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        DBDValueController valueController = getValueController();
        value = valueController.getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText(CoreMessages.dialog_data_label_value);

        int style = SWT.BORDER;
        if (getValueController().isReadOnly()) {
            style |= SWT.READ_ONLY;
        }
        int valueType = getValueController().getAttributeMetaData().getTypeID();
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
            switch (getValueController().getAttributeMetaData().getTypeID()) {
            case java.sql.Types.BIGINT:
            case java.sql.Types.INTEGER:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.TINYINT:
            case java.sql.Types.BIT:
                textEdit.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
                break;
            default:
                textEdit.addVerifyListener(UIUtils.NUMBER_VERIFY_LISTENER);
                break;
            }

            if (value != null) {
                // Use simple toString() because we don't want formatted values.
                textEdit.setText(value.toString());
            }
            int maxSize = getValueController().getAttributeMetaData().getPrecision();
            if (maxSize > 0) {
                textEdit.setTextLimit(maxSize + 2);
            }
            textEdit.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 300;
            //gd.grabExcessVerticalSpace = true;
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
                value,
                getValueController().getAttributeMetaData());
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

    @Override
    public void refreshValue()
    {
        value = getValueController().getValue();
        textEdit.setText(value.toString());
    }
}
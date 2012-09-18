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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBUtils;
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
        if (value == null) {
            value = "";
        } else {
            value = DBUtils.getDefaultValueDisplayString(value);
        }
        boolean isForeignKey = super.isForeignKey();

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText(CoreMessages.dialog_data_label_value);

        int style = SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP;
        if (getValueController().isReadOnly()) {
            style |= SWT.READ_ONLY;
        }
        textEdit = new Text(dialogGroup, style);

        textEdit.setText(value == null ? "" : value.toString());
        long maxSize = getValueController().getAttributeMetaData().getMaxLength();
        if (maxSize > 0) {
            textEdit.setTextLimit((int) maxSize);
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

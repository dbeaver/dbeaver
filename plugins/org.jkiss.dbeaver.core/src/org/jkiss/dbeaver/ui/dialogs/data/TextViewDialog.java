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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.binary.BinaryContent;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;

import java.nio.ByteBuffer;

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
        String stringValue = value == null ? "" : value.toString();
        boolean isForeignKey = super.isForeignKey();

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText(CoreMessages.dialog_data_label_value);

        boolean readOnly = getValueController().isReadOnly();
        boolean useHex = !readOnly && !isForeignKey;
        Composite container = dialogGroup;
        if (useHex) {
            container = new TabFolder(dialogGroup, SWT.TOP);
            //container.setLayout(new GridLayout(1, true));
            container.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        {
            int style = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP;
            if (!useHex) {
                style |= SWT.BORDER;
            }
            if (readOnly) {
                style |= SWT.READ_ONLY;
            }
            textEdit = new Text(container, style);

            textEdit.setText(stringValue);
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
            textEdit.setEditable(!readOnly);

            if (useHex) {
                TabItem item = new TabItem((TabFolder) container, SWT.NONE);
                item.setText("Text");
                item.setImage(DBIcon.TYPE_TEXT.getImage());
                item.setControl(textEdit);
            }
        }
        Point minSize = null;
        if (useHex) {
            HexEditControl hexEditControl = new HexEditControl(container, SWT.NONE, 6, 8);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 200;
            gd.minimumWidth = hexEditControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            hexEditControl.setLayoutData(gd);
            BinaryContent binaryContent = new BinaryContent();
            binaryContent.insert(ByteBuffer.wrap(stringValue.getBytes()), 0);
            hexEditControl.setContentProvider(binaryContent);
            minSize = hexEditControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            minSize.x += 50;
            minSize.y += 50;
            TabItem item = new TabItem((TabFolder) container, SWT.NONE);
            item.setText("Hex");
            item.setImage(DBIcon.TYPE_BINARY.getImage());
            item.setControl(hexEditControl);

            ((TabFolder) container).setSelection(0);
        }

        if (isForeignKey) {
            super.createEditorSelector(dialogGroup, textEdit);
        }
        if (minSize != null) {
            // Set default size as minimum
            getShell().setMinimumSize(minSize);
        }

        return dialogGroup;
    }

    @Override
    protected Object getEditorValue()
    {
        return textEdit.getText();
    }

}

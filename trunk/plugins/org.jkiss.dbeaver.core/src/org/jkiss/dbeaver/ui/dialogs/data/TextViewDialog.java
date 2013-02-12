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
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.binary.BinaryContent;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * TextViewDialog
 */
public class TextViewDialog extends ValueViewDialog {

    //private static final int DEFAULT_MAX_SIZE = 100000;
    private static final String VALUE_TYPE_SELECTOR = "string.value.type";

    private Text textEdit;
    private Label lengthLabel;
    private HexEditControl hexEditControl;
    private CTabFolder editorContainer;

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
        String stringValue = CommonUtils.toString(value);
        boolean isForeignKey = super.isForeignKey();

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText(CoreMessages.dialog_data_label_value);

        boolean readOnly = getValueController().isReadOnly();
        boolean useHex = !isForeignKey;
        long maxSize = getValueController().getValueType().getMaxLength();
        if (useHex) {
            editorContainer = new CTabFolder(dialogGroup, SWT.FLAT | SWT.TOP);
            editorContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

            lengthLabel = new Label(editorContainer, SWT.RIGHT);
            lengthLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            editorContainer.setTopRight(lengthLabel, SWT.FILL);
        }

        int selectedType = 0;
        if (getDialogSettings().get(VALUE_TYPE_SELECTOR) != null) {
            selectedType = getDialogSettings().getInt(VALUE_TYPE_SELECTOR);
        }
        {
            int style = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP;
            if (readOnly) {
                style |= SWT.READ_ONLY;
            }
            textEdit = new Text(useHex ? editorContainer : dialogGroup, style);

            textEdit.setText(stringValue);
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
            textEdit.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    updateValueLength();
                }
            });

            if (useHex) {
                CTabItem item = new CTabItem(editorContainer, SWT.NO_FOCUS);
                item.setText("Text");
                item.setImage(DBIcon.TYPE_TEXT.getImage());
                item.setControl(textEdit);
            }
        }
        Point minSize = null;
        if (useHex) {
            hexEditControl = new HexEditControl(editorContainer, readOnly ? SWT.READ_ONLY : SWT.NONE, 6, 8);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 200;
            gd.minimumWidth = hexEditControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            hexEditControl.setLayoutData(gd);
            setBinaryContent(stringValue);
            minSize = hexEditControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            minSize.x += 50;
            minSize.y += 50;
            CTabItem item = new CTabItem(editorContainer, SWT.NO_FOCUS);
            item.setText("Hex");
            item.setImage(DBIcon.TYPE_BINARY.getImage());
            item.setControl(hexEditControl);

            if (selectedType >= editorContainer.getItemCount()) {
                selectedType = 0;
            }
            editorContainer.setSelection(selectedType);
            editorContainer.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event)
                {
                    getDialogSettings().put(VALUE_TYPE_SELECTOR, editorContainer.getSelectionIndex());
/*
                switch (editorContainer.getSelectionIndex()) {
                    case 0: {
                        textEdit.setText(getBinaryContent());
                        break;
                    }
                    case 1:
                        setBinaryContent(textEdit.getText());
                        break;
                }
*/
                }
            });
            updateValueLength();
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

    private String getBinaryContent()
    {
        BinaryContent content = hexEditControl.getContent();
        ByteBuffer buffer = ByteBuffer.allocate((int) content.length());
        try {
            content.get(buffer, 0);
        } catch (IOException e) {
            log.error(e);
        }
        byte[] bytes = buffer.array();
        int length = bytes.length;
//        for (length = 0; length < bytes.length; length++) {
//            if (bytes[length] == 0) {
//                break;
//            }
//        }
        String stringValue;
        try {
            stringValue = new String(
                bytes, 0, length,
                ContentUtils.getDefaultBinaryFileEncoding(getValueController().getDataSource()));
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            stringValue = new String(buffer.array());
        }
        return stringValue;
    }

    private void setBinaryContent(String stringValue)
    {
        byte[] bytes;
        try {
            bytes = stringValue.getBytes(
                ContentUtils.getDefaultBinaryFileEncoding(getValueController().getDataSource()));
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            bytes = stringValue.getBytes();
        }
        hexEditControl.setContent(bytes);
    }

    @Override
    protected Object getEditorValue()
    {
        if (editorContainer == null || editorContainer.getSelectionIndex() == 0) {
            return textEdit.getText();
        } else {
            return getBinaryContent();
        }
    }

    private void updateValueLength()
    {
        if (lengthLabel != null) {
            long maxSize = getValueController().getValueType().getMaxLength();
            long length = textEdit.getText().length();
            lengthLabel.setText("Length: " + length + (maxSize > 0 ? " [" + maxSize + "]" : ""));
        }
    }

    @Override
    public void refreshValue()
    {
        String value = CommonUtils.toString(getValueController().getValue());
        textEdit.setText(value);
        if (hexEditControl != null) {
            setBinaryContent(value);
        }
    }

}

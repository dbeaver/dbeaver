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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * TextViewDialog
 */
public class TextViewDialog extends ValueViewDialog {

    private Text textEdit;
    private Label lengthLabel;
    private HexEditControl hexEditControl;
    private static final int DEFAULT_MAX_SIZE = 100000;

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
        boolean useHex = !isForeignKey;
        long maxSize = getValueController().getAttributeMetaData().getMaxLength();
        final CTabFolder container = new CTabFolder(dialogGroup, SWT.TOP);
        lengthLabel = new Label(container, SWT.RIGHT);
        lengthLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        container.setTopRight(lengthLabel, SWT.FILL);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

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

            CTabItem item = new CTabItem(container, SWT.NO_FOCUS);
            item.setText("Text");
            item.setImage(DBIcon.TYPE_TEXT.getImage());
            item.setControl(textEdit);
        }
        Point minSize = null;
        if (useHex) {
            hexEditControl = new HexEditControl(container, SWT.NONE, 6, 8);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 200;
            gd.minimumWidth = hexEditControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            hexEditControl.setLayoutData(gd);
            setBinaryContent(stringValue);
            minSize = hexEditControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            minSize.x += 50;
            minSize.y += 50;
            CTabItem item = new CTabItem(container, SWT.NO_FOCUS);
            item.setText("Hex");
            item.setImage(DBIcon.TYPE_BINARY.getImage());
            item.setControl(hexEditControl);
        }
        container.setSelection(0);
        container.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event)
            {
                switch(container.getSelectionIndex()) {
                    case 0:
                    {
                        try {
                            BinaryContent content = hexEditControl.getContent();
                            ByteBuffer buffer = ByteBuffer.allocate((int) content.length());
                            content.get(buffer, 0);
                            String stringValue = new String(buffer.array());
                            textEdit.setText(stringValue);
                        } catch (IOException e) {
                            log.error(e);
                        }
                        break;
                    }
                    case 1:
                    {
                        String stringValue = textEdit.getText();
                        setBinaryContent(stringValue);
                        break;
                    }
                }
            }
        });
        updateValueLength();

        if (isForeignKey) {
            super.createEditorSelector(dialogGroup, textEdit);
        }
        if (minSize != null) {
            // Set default size as minimum
            getShell().setMinimumSize(minSize);
        }

        return dialogGroup;
    }

    private void setBinaryContent(String stringValue)
    {
        int maxSize = (int) getValueController().getAttributeMetaData().getMaxLength();
        if (maxSize <= 0) {
            maxSize = DEFAULT_MAX_SIZE;
        }
        BinaryContent binaryContent = new BinaryContent();
        byte[] bytes = stringValue.getBytes();
        if (bytes.length > maxSize) {
            maxSize = bytes.length;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(maxSize);
        byteBuffer.put(bytes, 0, bytes.length);
        int tailSize = (maxSize - bytes.length);
        if (tailSize > 0) {
            byteBuffer.put(new byte[tailSize], 0, tailSize);
        }
        byteBuffer.position(0);
        binaryContent.insert(byteBuffer, 0);
        hexEditControl.setContentProvider(binaryContent);
    }

    @Override
    protected Object getEditorValue()
    {
        return textEdit.getText();
    }

    private void updateValueLength()
    {
        long maxSize = getValueController().getAttributeMetaData().getMaxLength();
        lengthLabel.setText("Length: " + textEdit.getText().length() + (maxSize > 0 ? " [" + maxSize + "]" : ""));
    }

}

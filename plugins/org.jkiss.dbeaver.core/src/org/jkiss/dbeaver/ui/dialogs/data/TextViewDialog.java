/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;
import org.jkiss.dbeaver.ui.editors.binary.BinaryContent;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * TextViewDialog
 */
public class TextViewDialog extends ValueViewDialog {

    private static final Log log = Log.getLog(TextViewDialog.class);

    //private static final int DEFAULT_MAX_SIZE = 100000;
    private static final String VALUE_TYPE_SELECTOR = "string.value.type";

    private StyledText textEdit;
    private Label lengthLabel;
    private HexEditControl hexEditControl;
    private CTabFolder editorContainer;
    private boolean dirty;

    public TextViewDialog(IValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        ReferenceValueEditor referenceValueEditor = new ReferenceValueEditor(getValueController(), this);
        boolean isForeignKey = referenceValueEditor.isReferenceValue();

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText(CoreMessages.dialog_data_label_value);

        boolean readOnly = getValueController().isReadOnly();
        boolean useHex = !isForeignKey;
        final DBSTypedObject valueType = getValueController().getValueType();
        long maxSize = valueType.getMaxLength();
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
            int style = SWT.NONE;
            if (readOnly) {
                style |= SWT.READ_ONLY;
            }
            if (useHex) {
                style |= SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP;
            } else {
                // Use border only for plain text editor, otherwise tab folder's border will be used
                style |= SWT.BORDER;
            }
            textEdit = new StyledText(useHex ? editorContainer : dialogGroup, style);
            textEdit.setMargins(3, 3, 3, 3);
            if (maxSize > 0 && valueType.getDataKind() == DBPDataKind.STRING) {
                textEdit.setTextLimit((int) maxSize);
            }
            if (readOnly) {
                //textEdit.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            }
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
                    dirty = true;
                    updateValueLength();
                }
            });
            UIUtils.fillDefaultStyledTextContextMenu(textEdit);

            if (useHex) {
                CTabItem item = new CTabItem(editorContainer, SWT.NO_FOCUS);
                item.setText("Text");
                item.setImage(DBeaverIcons.getImage(DBIcon.TYPE_TEXT));
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
            minSize = hexEditControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            minSize.x += 50;
            minSize.y += 50;
            CTabItem item = new CTabItem(editorContainer, SWT.NO_FOCUS);
            item.setText("Hex");
            item.setImage(DBeaverIcons.getImage(DBIcon.TYPE_BINARY));
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
                }
            });
            hexEditControl.addListener(SWT.Modify, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    dirty = true;
                }
            });
            updateValueLength();
        }

        primeEditorValue(getValueController().getValue());

        if (isForeignKey) {
            referenceValueEditor.createEditorSelector(dialogGroup);
        }

        if (minSize != null) {
            // Set default size as minimum
            getShell().setMinimumSize(minSize);
        }

        return dialogGroup;
    }

    private byte[] getBinaryContent()
    {
        BinaryContent content = hexEditControl.getContent();
        ByteBuffer buffer = ByteBuffer.allocate((int) content.length());
        try {
            content.get(buffer, 0);
        } catch (IOException e) {
            log.error(e);
        }
        return buffer.array();
    }

    private String getBinaryString()
    {
        byte[] bytes = getBinaryContent();
        int length = bytes.length;
        String stringValue;
        try {
            stringValue = new String(
                bytes, 0, length,
                getDefaultCharset());
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            stringValue = new String(bytes);
        }
        return stringValue;
    }

    private void setBinaryContent(String stringValue)
    {
        String charset = getDefaultCharset();
        byte[] bytes;
        try {
            bytes = stringValue.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            bytes = stringValue.getBytes(Charset.defaultCharset());
        }
        hexEditControl.setContent(bytes, charset);
    }

    private String getDefaultCharset() {
        return DBValueFormatting.getDefaultBinaryFileEncoding(getValueController().getExecutionContext().getDataSource());
    }

    @Override
    public Object extractEditorValue()
    {
        Object prevValue = getValueController().getValue();
        Object rawValue;
        if (prevValue instanceof DBDContent) {
            if (ContentUtils.isTextContent((DBDContent) prevValue)) {
                rawValue = isTextEditorActive() ? textEdit.getText() : getBinaryString();
            } else {
                rawValue = isTextEditorActive() ? GeneralUtils.convertToBytes(textEdit.getText()) : getBinaryContent();
            }
        } else {
            if (isTextEditorActive()) {
                rawValue = textEdit.getText();
            } else {
                rawValue = getBinaryString();
            }
        }

        try (DBCSession session = getValueController().getExecutionContext().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Make text value from editor")) {
            return getValueController().getValueHandler().getValueFromObject(
                session,
                getValueController().getValueType(),
                rawValue,
                false);
        } catch (Exception e) {
            DBUserInterface.getInstance().showError("Extract editor value", "Can't extract editor value", e);
            return null;
        }
    }

    @Override
    public Control getControl()
    {
        if (isTextEditorActive()) {
            return textEdit;
        } else {
            return hexEditControl;
        }
    }

    private boolean isTextEditorActive()
    {
        return editorContainer == null || editorContainer.getSelectionIndex() == 0;
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
    public void primeEditorValue(@Nullable Object value)
    {
        if (value instanceof DBDContentCached) {
            value = ((DBDContentCached) value).getCachedValue();
        }
        if (value instanceof byte[]) {
            // Binary
            byte[] bytes = (byte[]) value;
            textEdit.setText(GeneralUtils.convertToString(bytes, 0, bytes.length));
            if (hexEditControl != null) {
                hexEditControl.setContent(bytes, getDefaultCharset());
            }
        } else {
            // Should be string
            final IValueController valueController = getValueController();
            final String strValue = valueController.getValueHandler().getValueDisplayString(valueController.getValueType(), value, DBDDisplayFormat.EDIT);
            textEdit.setText(strValue);
            if (hexEditControl != null) {
                setBinaryContent(strValue);
            }
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

}

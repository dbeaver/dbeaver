/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.storage.BytesContentStorage;
import org.jkiss.dbeaver.model.data.storage.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.nio.ByteBuffer;

/**
* ContentInlineEditor
*/
public class ContentInlineEditor extends BaseValueEditor<Text> {
    private static final int MAX_STRING_LENGTH = 0xfffff;
    private static final Log log = Log.getLog(ContentInlineEditor.class);

    private final boolean isText;

    public ContentInlineEditor(IValueController controller) {
        super(controller);
        this.isText = ContentUtils.isTextContent(((DBDContent) controller.getValue()));
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        if (value instanceof DBDContentCached) {
            DBDContentCached newValue = (DBDContentCached)value;
            Object cachedValue = newValue.getCachedValue();
            String stringValue;
            if (cachedValue == null) {
                stringValue = "";  //$NON-NLS-1$
            } else if (cachedValue instanceof byte[]) {
                byte[] bytes = (byte[]) cachedValue;
                stringValue = DBValueFormatting.getBinaryPresentation(valueController.getExecutionContext().getDataSource()).toString(bytes, 0, bytes.length);
            } else if (cachedValue instanceof ByteBuffer) {
                byte[] bytes = ((ByteBuffer) cachedValue).array();
                stringValue = DBValueFormatting.getBinaryPresentation(valueController.getExecutionContext().getDataSource()).toString(bytes, 0, bytes.length);
            } else {
                stringValue = cachedValue.toString();
            }
            control.setText(stringValue);
            control.selectAll();
        }
    }

    @Override
    protected Text createControl(Composite editPlaceholder)
    {
        final Text editor = new Text(editPlaceholder, SWT.BORDER);
        editor.setEditable(!valueController.isReadOnly());
        editor.setFont(UIUtils.getMonospaceFont());
        long maxLength = valueController.getValueType().getMaxLength();
        if (maxLength <= 0) {
            maxLength = MAX_STRING_LENGTH;
        } else {
            maxLength = Math.min(maxLength, MAX_STRING_LENGTH);
        }
        editor.setTextLimit((int) maxLength);
        return editor;
    }

    @Override
    public Object extractEditorValue()
    {
        String newValue = control.getText();
        final DBDContent content = (DBDContent) valueController.getValue();
        assert content != null;
        try {
            if (isText) {
                content.updateContents(
                    new VoidProgressMonitor(),
                    new StringContentStorage(newValue));
            } else {
                content.updateContents(
                    new VoidProgressMonitor(),
                    new BytesContentStorage(newValue.getBytes(GeneralUtils.getDefaultFileEncoding()), GeneralUtils.getDefaultFileEncoding()));
            }
        } catch (Exception e) {
            log.error(e);
        }
        return content;
    }
}

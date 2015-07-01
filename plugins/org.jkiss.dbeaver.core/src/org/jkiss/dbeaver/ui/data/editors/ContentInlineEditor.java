/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.resource.JFaceResources;
import org.jkiss.dbeaver.Log;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.model.impl.BytesContentStorage;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.nio.ByteBuffer;

/**
* ContentInlineEditor
*/
public class ContentInlineEditor extends BaseValueEditor<Text> {
    private static final int MAX_STRING_LENGTH = 0xfffff;
    static final Log log = Log.getLog(ContentInlineEditor.class);

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
                stringValue = DBUtils.getBinaryPresentation(valueController.getExecutionContext().getDataSource()).toString(bytes, 0, bytes.length);
            } else if (cachedValue instanceof ByteBuffer) {
                byte[] bytes = ((ByteBuffer) cachedValue).array();
                stringValue = DBUtils.getBinaryPresentation(valueController.getExecutionContext().getDataSource()).toString(bytes, 0, bytes.length);
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
        editor.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
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
                    VoidProgressMonitor.INSTANCE,
                    new StringContentStorage(newValue));
            } else {
                content.updateContents(
                    VoidProgressMonitor.INSTANCE,
                    new BytesContentStorage(newValue.getBytes(GeneralUtils.getDefaultFileEncoding()), GeneralUtils.getDefaultFileEncoding()));
            }
        } catch (Exception e) {
            log.error(e);
        }
        return content;
    }
}

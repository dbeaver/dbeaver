/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.data.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.impl.BytesContentStorage;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

/**
* ContentInlineEditor
*/
public class ContentInlineEditor extends BaseValueEditor<Text> {
    private static final int MAX_STRING_LENGTH = 0xfffff;
    static final Log log = LogFactory.getLog(ContentInlineEditor.class);

    private final boolean isText;

    public ContentInlineEditor(DBDValueController controller, boolean isText) {
        super(controller);
        this.isText = isText;
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
                stringValue = DBUtils.getBinaryPresentation(valueController.getDataSource()).toString(bytes, 0, bytes.length);
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
        try {
            if (isText) {
                content.updateContents(
                    VoidProgressMonitor.INSTANCE,
                    new StringContentStorage(newValue));
            } else {
                content.updateContents(
                    VoidProgressMonitor.INSTANCE,
                    new BytesContentStorage(newValue.getBytes(ContentUtils.getDefaultFileEncoding()), ContentUtils.getDefaultFileEncoding()));
            }
        } catch (Exception e) {
            log.error(e);
        }
        return content;
    }
}

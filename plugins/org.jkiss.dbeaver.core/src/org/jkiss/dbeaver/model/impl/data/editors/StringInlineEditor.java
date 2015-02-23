/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.utils.CommonUtils;

/**
* StringInlineEditor
*/
public class StringInlineEditor extends BaseValueEditor<Text> {

    private static final int MAX_STRING_LENGTH = 0xffff;

    public StringInlineEditor(DBDValueController controller) {
        super(controller);
    }

    @Override
    protected Text createControl(Composite editPlaceholder)
    {
        final boolean inline = valueController.getEditType() == DBDValueController.EditType.INLINE;
        final Text editor = new Text(valueController.getEditPlaceholder(),
            SWT.BORDER | (inline ? SWT.NONE : SWT.MULTI | SWT.WRAP | SWT.V_SCROLL));
        editor.setTextLimit(MAX_STRING_LENGTH);
        editor.setEditable(!valueController.isReadOnly());
        return editor;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        control.setText(CommonUtils.toString(value));
        if (valueController.getEditType() == DBDValueController.EditType.INLINE) {
            control.selectAll();
        }
    }

    @Override
    public Object extractEditorValue()
    {
        return control.getText();
    }
}

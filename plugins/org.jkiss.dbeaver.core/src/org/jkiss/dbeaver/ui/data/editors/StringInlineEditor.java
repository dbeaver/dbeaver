/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.data.IValueController;

/**
* StringInlineEditor
*/
public class StringInlineEditor extends BaseValueEditor<Text> {

    private static final int MAX_STRING_LENGTH = 0xffff;

    public StringInlineEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected Text createControl(Composite editPlaceholder)
    {
        final boolean inline = valueController.getEditType() == IValueController.EditType.INLINE;
        final Text editor = new Text(valueController.getEditPlaceholder(),
            (inline ? SWT.BORDER : SWT.MULTI | SWT.WRAP | SWT.V_SCROLL));
        editor.setTextLimit(MAX_STRING_LENGTH);
        editor.setEditable(!valueController.isReadOnly());
        return editor;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        final String strValue = valueController.getValueHandler().getValueDisplayString(valueController.getValueType(), value, DBDDisplayFormat.EDIT);
        control.setText(strValue);
        if (valueController.getEditType() == IValueController.EditType.INLINE) {
            control.selectAll();
        }
    }

    @Override
    public Object extractEditorValue() throws DBCException {
        try (DBCSession session = valueController.getExecutionContext().openSession(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.UTIL, "Make string value from editor")) {
            return valueController.getValueHandler().getValueFromObject(
                session,
                valueController.getValueType(),
                control.getText(),
                false);
        }
    }
}

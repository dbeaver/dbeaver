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
package org.jkiss.dbeaver.model.impl.data.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTimeEditor;

import java.util.Date;

/**
* DateTimeInlineEditor
*/
public class DateTimeInlineEditor extends BaseValueEditor<Control> {
    private final DateTimeEditorHelper helper;
    private CustomTimeEditor timeEditor;

    public DateTimeInlineEditor(DBDValueController controller, DateTimeEditorHelper helper) {
        super(controller);
        this.helper = helper;
    }

    @Override
    protected Control createControl(Composite editPlaceholder)
    {
        boolean inline = valueController.getEditType() == DBDValueController.EditType.INLINE;
        final Composite dateTimeGroup = inline ?
            valueController.getEditPlaceholder() :
            new Composite(valueController.getEditPlaceholder(), SWT.BORDER);
        if (!inline) {
            dateTimeGroup.setLayout(new GridLayout(2, false));
        }

        if (!inline) {
            UIUtils.createControlLabel(dateTimeGroup, "Value");
        }
        timeEditor = new CustomTimeEditor(dateTimeGroup,
            (inline ? SWT.BORDER : SWT.NONE) | SWT.TIME | SWT.LONG,
            helper.getFormatter(valueController.getValueType()));
        timeEditor.setEnabled(!valueController.isReadOnly());
        if (!inline) {
            timeEditor.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        return timeEditor.getControl();
    }

    @Override
    public Date extractEditorValue() throws DBException {
        return timeEditor.getValue();
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        timeEditor.setValue(value);
    }

}

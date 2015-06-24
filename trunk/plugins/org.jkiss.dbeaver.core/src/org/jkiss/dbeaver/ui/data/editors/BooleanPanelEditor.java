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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.data.IValueController;

/**
* BooleanPanelEditor
*/
public class BooleanPanelEditor extends BaseValueEditor<List> {
    public BooleanPanelEditor(IValueController controller) {
        super(controller);
    }

    @Override
    public Object extractEditorValue()
    {
        return control.getSelectionIndex() == 1;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        control.setSelection(Boolean.TRUE.equals(value) ? 1 : 0);
    }

    @Override
    protected List createControl(Composite editPlaceholder)
    {
        final List editor = new List(valueController.getEditPlaceholder(), SWT.SINGLE | SWT.READ_ONLY);
        editor.add("FALSE");
        editor.add("TRUE");
        return editor;
    }
}

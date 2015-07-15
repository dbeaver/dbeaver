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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.data.IValueController;

/**
* BitInlineEditor
*/
public class BitInlineEditor extends BaseValueEditor<Combo> {
    public BitInlineEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected Combo createControl(Composite editPlaceholder)
    {
        final Combo editor = new Combo(valueController.getEditPlaceholder(), SWT.READ_ONLY);
        editor.add(Boolean.FALSE.toString()); //$NON-NLS-1$
        editor.add(Boolean.TRUE.toString()); //$NON-NLS-1$
        editor.setEnabled(!valueController.isReadOnly());
        return editor;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        control.setText(value == null ? Boolean.FALSE.toString() : value.toString()); //$NON-NLS-1$
    }

    @Override
    public Object extractEditorValue()
    {
        switch (control.getSelectionIndex()) {
            case 0:
                return Boolean.FALSE;
            case 1:
                return Boolean.TRUE;
            default:
                return null;
        }
    }
}

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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;

/**
* BitInlineEditor
*/
public class BitInlineEditor extends BaseValueEditor<Combo> {
    public BitInlineEditor(DBDValueController controller) {
        super(controller);
    }

    @Override
    protected Combo createControl(Composite editPlaceholder)
    {
        final Combo editor = new Combo(valueController.getEditPlaceholder(), SWT.READ_ONLY);
        editor.add("0"); //$NON-NLS-1$
        editor.add("1"); //$NON-NLS-1$
        editor.setEnabled(!valueController.isReadOnly());
        return editor;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        control.setText(value == null ? "0" : value.toString()); //$NON-NLS-1$
    }

    @Override
    public Object extractEditorValue()
    {
        switch (control.getSelectionIndex()) {
            case 0:
                return (byte) 0;
            case 1:
                return (byte) 1;
            default:
                return null;
        }
    }
}

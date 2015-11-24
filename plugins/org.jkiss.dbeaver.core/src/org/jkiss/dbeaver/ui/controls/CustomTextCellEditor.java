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

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.utils.CommonUtils;

/**
 * Common text cell editor.
 * Allows null values.
 */
public class CustomTextCellEditor extends TextCellEditor {

    private boolean wasNull;

    public CustomTextCellEditor(Composite parent)
    {
        super(parent);
    }

    @Override
    protected Object doGetValue()
    {
        final Object value = super.doGetValue();
        if (wasNull && "".equals(value)) {
            return null;
        } else {
            return value;
        }
    }

    @Override
    protected void doSetValue(Object value)
    {
        if (value == null) {
            wasNull = true;
        }
        super.doSetValue(CommonUtils.toString(value));
    }

    protected int getDoubleClickTimeout() {
        return 0;
    }
}

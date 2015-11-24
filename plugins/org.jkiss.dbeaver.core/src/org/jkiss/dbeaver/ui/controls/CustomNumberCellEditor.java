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
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Number cell editor
 */
public class CustomNumberCellEditor extends TextCellEditor {

    private final Class<?> valueType;

    public CustomNumberCellEditor(Composite parent, Class<?> valueType)
    {
        super();
        this.valueType = valueType;
        create(parent);
    }

    @Override
    protected Control createControl(Composite parent)
    {
        super.createControl(parent);
        if (valueType == Float.class ||
            valueType == Float.TYPE ||
            valueType == Double.class ||
            valueType == Double.TYPE ||
            valueType == BigDecimal.class)
        {
            text.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        } else {
            text.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        }
        return text;
    }

    @Override
    protected Object doGetValue()
    {
        return GeneralUtils.convertString((String) super.doGetValue(), valueType);
    }

    @Override
    protected void doSetValue(Object value)
    {
        super.doSetValue(CommonUtils.toString(value));
    }

    protected int getDoubleClickTimeout() {
        return 0;
    }
}

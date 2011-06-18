/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

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

}

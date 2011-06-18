/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.math.BigDecimal;

/**
 * Number cell editor
 */
public class CustomNumberCellEditor extends TextCellEditor {

    private final Class<?> valueType;

    public CustomNumberCellEditor(Composite parent, Class<?> valueType)
    {
        super(parent);
        this.valueType = valueType;
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
            text.addVerifyListener(UIUtils.NUMBER_VERIFY_LISTENER);
        } else {
            text.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
        }
        return text;
    }

    @Override
    protected Object doGetValue()
    {
        return RuntimeUtils.convertString((String) super.doGetValue(), valueType);
    }

    @Override
    protected void doSetValue(Object value)
    {
        super.doSetValue(CommonUtils.toString(value));
    }

}

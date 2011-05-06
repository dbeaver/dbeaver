/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import net.sf.jkiss.utils.BeanUtils;
import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Number cell editor
 */
public class CustomNumberCellEditor extends TextCellEditor {

    static final Log log = LogFactory.getLog(CustomNumberCellEditor.class);

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
        text.addVerifyListener(UIUtils.NUMBER_VERIFY_LISTENER);
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

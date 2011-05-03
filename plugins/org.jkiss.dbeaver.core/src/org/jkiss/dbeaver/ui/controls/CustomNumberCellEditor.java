/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
        try {
            final String value = (String) super.doGetValue();
            if (valueType == Long.class) {
                return new Long(value);
            } else if (valueType == Long.TYPE) {
                return Long.parseLong(value);
            } else if (valueType == Integer.class) {
                return new Integer(value);
            } else if (valueType == Integer.TYPE) {
                return Integer.parseInt(value);
            } else if (valueType == Short.class) {
                return new Short(value);
            } else if (valueType == Short.TYPE) {
                return Short.parseShort(value);
            } else if (valueType == Byte.class) {
                return new Byte(value);
            } else if (valueType == Byte.TYPE) {
                return Byte.parseByte(value);
            } else if (valueType == Double.class) {
                return new Double(value);
            } else if (valueType == Double.TYPE) {
                return Double.parseDouble(value);
            } else if (valueType == Float.class) {
                return new Float(value);
            } else if (valueType == Float.TYPE) {
                return Float.parseFloat(value);
            } else if (valueType == BigInteger.class) {
                return new BigInteger(value);
            } else if (valueType == BigDecimal.class) {
                return new BigDecimal(value);
            } else {
                throw new IllegalArgumentException("Unsupported numeric type: " + valueType.getName());
            }
        } catch (RuntimeException e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    protected void doSetValue(Object value)
    {
        super.doSetValue(CommonUtils.toString(value));
    }

}

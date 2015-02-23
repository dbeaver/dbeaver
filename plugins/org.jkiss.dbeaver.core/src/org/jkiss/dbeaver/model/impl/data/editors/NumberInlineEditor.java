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

import org.jkiss.dbeaver.core.Log;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;

/**
* NumberInlineEditor
*/
public class NumberInlineEditor extends BaseValueEditor<Text> {

    static final Log log = Log.getLog(NumberInlineEditor.class);
    private static final int MAX_NUMBER_LENGTH = 100;
    private static final String BAD_DOUBLE_VALUE = "2.2250738585072012e-308"; //$NON-NLS-1$
    private final NumberEditorHelper helper;

    public NumberInlineEditor(DBDValueController controller, NumberEditorHelper helper) {
        super(controller);
        this.helper = helper;
    }

    @Override
    protected Text createControl(Composite editPlaceholder)
    {
        final Text editor = new Text(valueController.getEditPlaceholder(), SWT.BORDER);
        editor.setEditable(!valueController.isReadOnly());
        editor.setTextLimit(MAX_NUMBER_LENGTH);
        Class<? extends Number> type = helper.getNumberType(valueController.getValueType(), valueController.getValue());
        if (type == Float.class || type == Double.class || type == BigDecimal.class) {
            editor.addVerifyListener(UIUtils.getNumberVerifyListener(helper.getLocale()));
        } else {
            editor.addVerifyListener(UIUtils.getIntegerVerifyListener(helper.getLocale()));
        }
        return editor;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        if (value != null) {
            control.setText(valueController.getValueHandler().getValueDisplayString(valueController.getValueType(), value, DBDDisplayFormat.UI));
        } else {
            control.setText("");
        }
        if (valueController.getEditType() == DBDValueController.EditType.INLINE) {
            control.selectAll();
        }
    }

    @Nullable
    @Override
    public Object extractEditorValue()
    {
        String text = control.getText();
        if (CommonUtils.isEmpty(text)) {
            return null;
        }
        Class<? extends Number> hintType = helper.getNumberType(valueController.getValueType(), valueController.getValue());
        return convertStringToNumber(text, hintType, helper.getFormatter());
    }

    @Nullable
    public static Number convertStringToNumber(String text, Class<? extends Number> hintType, DBDDataFormatter formatter)
    {
        if (text == null || text.length() == 0) {
            return null;
        }
        try {
            if (hintType == Long.class) {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException e) {
                    return new BigInteger(text);
                }
            } else if (hintType == Integer.class) {
                return Integer.valueOf(text);
            } else if (hintType == Short.class) {
                return Short.valueOf(text);
            } else if (hintType == Byte.class) {
                return Byte.valueOf(text);
            } else if (hintType == Float.class) {
                return Float.valueOf(text);
            } else if (hintType == Double.class) {
                return toDouble(text);
            } else if (hintType == BigInteger.class) {
                return new BigInteger(text);
            } else {
                return new BigDecimal(text);
            }
        } catch (NumberFormatException e) {
            log.debug("Bad numeric value '" + text + "' - " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                return (Number)formatter.parseValue(text, hintType);
            } catch (ParseException e1) {
                log.debug("Can't parse numeric value [" + text + "] using formatter", e);
                return null;
            }
        }
    }

    private static Number toDouble(String text)
    {
        if (text.equals(BAD_DOUBLE_VALUE)) {
            return Double.MIN_VALUE;
        }
        return Double.valueOf(text);
    }

}

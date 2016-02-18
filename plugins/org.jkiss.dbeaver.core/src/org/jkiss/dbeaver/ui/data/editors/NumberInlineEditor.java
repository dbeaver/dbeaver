/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.util.Locale;

/**
* NumberInlineEditor
*/
public class NumberInlineEditor extends BaseValueEditor<Text> {

    static final Log log = Log.getLog(NumberInlineEditor.class);
    private static final int MAX_NUMBER_LENGTH = 100;

    private DBDDataFormatterProfile formatterProfile;

    public NumberInlineEditor(IValueController controller) {
        super(controller);
        this.formatterProfile = valueController.getExecutionContext().getDataSource().getContainer().getDataFormatterProfile();
    }

    @Override
    protected Text createControl(Composite editPlaceholder)
    {
        final Text editor = new Text(valueController.getEditPlaceholder(), SWT.BORDER);
        editor.setEditable(!valueController.isReadOnly());
        editor.setTextLimit(MAX_NUMBER_LENGTH);
        Object curValue = valueController.getValue();
        Class<?> type = curValue instanceof Number ?
            curValue.getClass() :
            valueController.getValueHandler().getValueObjectType(valueController.getValueType());
        Locale locale = formatterProfile.getLocale();
        if (type == Float.class || type == Double.class || type == BigDecimal.class) {
            editor.addVerifyListener(UIUtils.getNumberVerifyListener(locale));
        } else {
            editor.addVerifyListener(UIUtils.getIntegerVerifyListener(locale));
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
        if (valueController.getEditType() == IValueController.EditType.INLINE) {
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
        Object curValue = valueController.getValue();
        Class<?> hintType = curValue instanceof Number ?
            curValue.getClass() :
            valueController.getValueHandler().getValueObjectType(valueController.getValueType());
        try {
            return DBUtils.convertStringToNumber(text, hintType, formatterProfile.createFormatter(DBDDataFormatter.TYPE_NAME_NUMBER));
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

}

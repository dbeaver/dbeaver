/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.registry.formatter.DataFormatterRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.util.Locale;

/**
* NumberInlineEditor
*/
public class NumberInlineEditor extends BaseValueEditor<Text> {

    private static final Log log = Log.getLog(NumberInlineEditor.class);
    private static final int MAX_NUMBER_LENGTH = 100;

    private DBDDataFormatterProfile formatterProfile;

    public NumberInlineEditor(IValueController controller) {
        super(controller);
        if (valueController.getExecutionContext() != null) {
            this.formatterProfile = valueController.getExecutionContext().getDataSource().getContainer().getDataFormatterProfile();
        } else {
            this.formatterProfile = DataFormatterRegistry.getInstance().getGlobalProfile();
        }
    }

    @Override
    protected Text createControl(Composite editPlaceholder)
    {
        final boolean inline = valueController.getEditType() == IValueController.EditType.INLINE;
        final Text editor = new Text(valueController.getEditPlaceholder(), inline ? SWT.BORDER : SWT.MULTI);
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
            return DBValueFormatting.convertStringToNumber(text, hintType, formatterProfile.createFormatter(DBDDataFormatter.TYPE_NAME_NUMBER));
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

}

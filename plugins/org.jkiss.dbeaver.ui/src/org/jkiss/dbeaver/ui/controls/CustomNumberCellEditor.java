/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

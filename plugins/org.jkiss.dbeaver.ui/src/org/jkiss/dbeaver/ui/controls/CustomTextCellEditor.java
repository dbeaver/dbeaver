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

    public CustomTextCellEditor(Composite parent, int style) {
        super(parent, style);
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

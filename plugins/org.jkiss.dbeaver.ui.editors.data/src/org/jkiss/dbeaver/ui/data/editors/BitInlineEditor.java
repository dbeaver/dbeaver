/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IValueController;

/**
* BitInlineEditor
*/
public class BitInlineEditor extends BaseValueEditor<Combo> {
    public BitInlineEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected Combo createControl(Composite editPlaceholder)
    {
        final Combo editor = new Combo(valueController.getEditPlaceholder(), SWT.READ_ONLY);
        DBSTypedObject attr = valueController.getValueType();
        editor.add(Boolean.FALSE.toString());
        editor.add(Boolean.TRUE.toString());
        if (attr instanceof DBSAttributeBase && !((DBSAttributeBase) attr).isRequired()) {
            editor.add(DBConstants.NULL_VALUE_LABEL);
        }
        editor.setEnabled(!valueController.isReadOnly());
        editor.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveValue();
            }
        });
        return editor;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        if (value instanceof Number) {
            value = ((Number) value).byteValue() != 0;
        }
        control.setText(value == null ? DBConstants.NULL_VALUE_LABEL : value.toString()); //$NON-NLS-1$
    }

    @Override
    public Object extractEditorValue()
    {
        switch (control.getSelectionIndex()) {
            case 0:
                return Boolean.FALSE;
            case 1:
                return Boolean.TRUE;
            default:
                return null;
        }
    }
}

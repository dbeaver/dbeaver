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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
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
        editor.add(Boolean.FALSE.toString()); //$NON-NLS-1$
        editor.add(Boolean.TRUE.toString()); //$NON-NLS-1$
        editor.setEnabled(!valueController.isReadOnly());
        return editor;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        control.setText(value == null ? Boolean.FALSE.toString() : value.toString()); //$NON-NLS-1$
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

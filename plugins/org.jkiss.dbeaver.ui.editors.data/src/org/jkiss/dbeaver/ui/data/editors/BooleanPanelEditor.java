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
import org.eclipse.swt.widgets.List;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.data.IValueController;

/**
* BooleanPanelEditor
*/
public class BooleanPanelEditor extends BaseValueEditor<List> {
    public BooleanPanelEditor(IValueController controller) {
        super(controller);
    }

    @Override
    public Object extractEditorValue()
    {
        return control.getSelectionIndex() == 1;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        control.setSelection(Boolean.TRUE.equals(value) ? 1 : 0);
    }

    @Override
    protected List createControl(Composite editPlaceholder)
    {
        final List editor = new List(valueController.getEditPlaceholder(), SWT.SINGLE | SWT.READ_ONLY);
        editor.add("FALSE");
        editor.add("TRUE");
        return editor;
    }
}

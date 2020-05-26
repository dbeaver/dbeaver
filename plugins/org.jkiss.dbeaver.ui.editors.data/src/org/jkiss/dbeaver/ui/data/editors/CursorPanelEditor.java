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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.data.IValueController;

/**
* CursorPanelEditor
*/
public class CursorPanelEditor extends BaseValueEditor<CursorViewComposite> {

    public CursorPanelEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected CursorViewComposite createControl(Composite editPlaceholder)
    {
        CursorViewComposite viewComposite = new CursorViewComposite(editPlaceholder, getValueController());
        // Set browser settings

        return viewComposite;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        if (control != null) {
            control.refresh();
        }
    }

    @Override
    public Object extractEditorValue() throws DBCException {
        return null;
    }

}

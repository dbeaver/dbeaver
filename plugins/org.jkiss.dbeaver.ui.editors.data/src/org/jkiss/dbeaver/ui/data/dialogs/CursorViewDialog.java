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
package org.jkiss.dbeaver.ui.data.dialogs;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.CursorViewComposite;

/**
 * CursorViewDialog
 */
public class CursorViewDialog extends ValueViewDialog {

    private CursorViewComposite cursorViewComposite;

    public CursorViewDialog(IValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);
        cursorViewComposite = new CursorViewComposite(dialogGroup, getValueController());
        cursorViewComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        cursorViewComposite.refresh();
        return dialogGroup;
    }

    @Override
    public Object extractEditorValue()
    {
        return null;
    }

    @Override
    public Control getControl()
    {
        return cursorViewComposite == null ? null : cursorViewComposite.getControl();
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        cursorViewComposite.refresh();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isDirty() {
        return cursorViewComposite.isDirty();
    }

    @Override
    public void setDirty(boolean dirty) {

    }

}
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
package org.jkiss.dbeaver.model.impl.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDComplexValue;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.impl.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.dialogs.data.ComplexObjectEditor;

/**
* ComplexValueInlineEditor
*/
public class ComplexValueInlineEditor extends BaseValueEditor<Tree> {
    private final DBDValueController controller;
    ComplexObjectEditor editor;

    public ComplexValueInlineEditor(DBDValueController controller) {
        super(controller);
        this.controller = controller;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        editor.setModel(controller.getExecutionContext(), (DBDComplexValue) value);
    }

    @Override
    protected Tree createControl(Composite editPlaceholder)
    {
        editor = new ComplexObjectEditor(controller.getValueSite(), controller.getEditPlaceholder(), SWT.BORDER);
        editor.setModel(controller.getExecutionContext(), (DBDComplexValue) controller.getValue());
        return editor.getTree();
    }

    @Override
    public Object extractEditorValue()
    {
        return editor.getInput();
    }
}

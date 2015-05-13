/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.text.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;

/**
 * Abstract text editor handler
 */
public abstract class AbstractTextHandler extends AbstractHandler {

    public static BaseTextEditor getEditor(ExecutionEvent event) {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        return getTextEditor(editor);
    }

    public static BaseTextEditor getTextEditor(IEditorPart editor)
    {
        if (editor == null) {
            return null;
        }
        if (editor instanceof BaseTextEditor) {
            return (BaseTextEditor) editor;
        }
        return (BaseTextEditor) editor.getAdapter(BaseTextEditor.class);
    }
}

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
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;

/**
 * DatabaseEditorPropertyTester
 */
public class ERDEditorPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.ext.erd.editor";
    public static final String PROP_CAN_UNDO = "canUndo";
    public static final String PROP_CAN_REDO = "canRedo";
    public static final String PROP_CAN_DELETE = "canDelete";
    public static final String PROP_EDITABLE = "editable";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof ERDEditorPart)) {
            return false;
        }
        ERDEditorPart erdEditor = (ERDEditorPart) receiver;
        switch (property) {
            case PROP_CAN_UNDO:
                return erdEditor.getCommandStack().canUndo();
            case PROP_CAN_REDO:
                return erdEditor.getCommandStack().canRedo();
            case PROP_EDITABLE:
                return !erdEditor.isReadOnly();
            case PROP_CAN_DELETE:
                DeleteAction deleteAction = new DeleteAction((IWorkbenchPart) erdEditor);
                deleteAction.update();
                return deleteAction.isEnabled();
        }
        return false;
    }

}
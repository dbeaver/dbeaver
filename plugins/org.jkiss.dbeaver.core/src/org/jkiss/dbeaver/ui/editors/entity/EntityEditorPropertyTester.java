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
package org.jkiss.dbeaver.ui.editors.entity;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * EntityEditorPropertyTester
 */
public class EntityEditorPropertyTester extends PropertyTester
{
    static final Log log = Log.getLog(EntityEditorPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.entity";
    public static final String PROP_DIRTY = "dirty";
    public static final String PROP_CAN_UNDO = "canUndo";
    public static final String PROP_CAN_REDO = "canRedo";

    public EntityEditorPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof EntityEditor)) {
            return false;
        }
        EntityEditor editor = (EntityEditor)receiver;
        DBECommandContext commandContext = editor.getEditorInput().getCommandContext();
        if (commandContext != null) {
            switch (property) {
                case PROP_CAN_UNDO:
                    return commandContext.getUndoCommand() != null;
                case PROP_CAN_REDO:
                    return commandContext.getRedoCommand() != null;
                case PROP_DIRTY:
                    return commandContext.isDirty();
            }
        }

        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}
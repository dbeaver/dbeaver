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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.text.IUndoManager;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * EntityEditorPropertyTester
 */
public class EntityEditorPropertyTester extends PropertyTester
{
    private static final Log log = Log.getLog(EntityEditorPropertyTester.class);

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
        if (PROP_DIRTY.equals(property)) {
            return editor.isDirty();
        }

        IUndoManager undoManager = editor.getAdapter(IUndoManager.class);
        if (undoManager != null) {
            switch (property) {
                case PROP_CAN_UNDO:
                    return undoManager.undoable();
                case PROP_CAN_REDO:
                    return undoManager.redoable();
            }
        } else {
            DBECommandContext commandContext = editor.getEditorInput().getCommandContext();
            if (commandContext != null) {
                switch (property) {
                    case PROP_CAN_UNDO:
                        return commandContext.getUndoCommand() != null;
                    case PROP_CAN_REDO:
                        return commandContext.getRedoCommand() != null;
                }
            }
        }

        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}
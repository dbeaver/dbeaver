/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.editors.entity.handlers.ObjectPropertySwitchToSourceHandler;

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
    public static final String PROP_HAS_SOURCE = "hasSource";
    public static final String PROP_OBJECT_TYPE = "objectType";

    public EntityEditorPropertyTester() {
        super();
    }

    @Override
    public boolean test(@Nullable Object receiver, @NotNull String property, @NotNull Object[] args, @Nullable Object expectedValue) {
        if (!(receiver instanceof EntityEditor)) {
            return false;
        }
        EntityEditor editor = (EntityEditor)receiver;
        if (PROP_DIRTY.equals(property)) {
            return editor.isDirty();
        } else if (PROP_HAS_SOURCE.equals(property)) {
            return ObjectPropertySwitchToSourceHandler.findSourceTextEditorId(editor) != null;
        } else if (PROP_OBJECT_TYPE.equals(property)) {
            DBSObject object = editor.getEditorInput() == null ? null : editor.getEditorInput().getDatabaseObject();
            if (object != null && expectedValue instanceof String typeName) {
                for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
                    if (type.getName().equals(typeName)) {
                        return true;
                    }
                }
            }
            return false;
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

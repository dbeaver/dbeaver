/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.entity.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorDescriptor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorsRegistry;
import org.jkiss.dbeaver.ui.editors.entity.properties.ObjectPropertiesEditor;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;

import java.util.List;

public class ObjectPropertySwitchToSourceHandler extends AbstractHandler {

    @Override
    @Nullable
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
        if (editorPart instanceof EntityEditor) {
            EntityEditor editor = (EntityEditor)editorPart;
            String sourceEditorId = findSourceTextEditorId(editor);
            if (sourceEditorId != null) {
                editor.switchFolder(sourceEditorId);
            }
        }
        return null;
    }
    
    
    @Nullable
    public static String findSourceTextEditorId(@NotNull EntityEditor editor) {
        ObjectPropertiesEditor part = (ObjectPropertiesEditor)editor.getPageEditor(EntityEditorDescriptor.DEFAULT_OBJECT_EDITOR_ID);
        if (part != null) {
            List<EntityEditorDescriptor> descrs = EntityEditorsRegistry.getInstance().getEntityEditors(editor.getDatabaseObject(), part, null);
            for (EntityEditorDescriptor descr: descrs) {
                if (BaseTextEditor.class.isAssignableFrom(descr.getEditorType().getObjectClass())) {
                    return descr.getId();
                }
            }
        } 
        return null;
    }
}

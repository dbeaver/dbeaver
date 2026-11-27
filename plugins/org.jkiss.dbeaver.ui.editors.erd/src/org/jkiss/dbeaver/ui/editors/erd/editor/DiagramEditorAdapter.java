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
package org.jkiss.dbeaver.ui.editors.erd.editor;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.MultiPageAbstractEditor;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Adapts controls and multi-page editors without loading GEF-backed editor classes.
 */
public class DiagramEditorAdapter implements IAdapterFactory {

    private static final Map<Control, IDiagramEditorPart> editorsMap = new IdentityHashMap<>();

    static synchronized void mapControl(@NotNull Control control, @NotNull IDiagramEditorPart editor) {
        editorsMap.put(control, editor);
    }

    static synchronized void unmapControl(@NotNull Control control) {
        editorsMap.remove(control);
    }

    @Nullable
    public static synchronized IDiagramEditorPart getEditor(@NotNull Control control) {
        return editorsMap.get(control);
    }

    @Nullable
    @Override
    public <T> T getAdapter(@NotNull Object adaptableObject, @NotNull Class<T> adapterType) {
        if (adapterType == IDiagramEditorPart.class) {
            if (adaptableObject instanceof Control control) {
                return adapterType.cast(getEditor(control));
            } else if (adaptableObject instanceof MultiPageAbstractEditor editor) {
                if (editor.getActiveEditor() instanceof IDiagramEditorPart diagramEditor) {
                    return adapterType.cast(diagramEditor);
                }
            } else if (adaptableObject instanceof IDiagramEditorPart) {
                return adapterType.cast(adaptableObject);
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Class<?>[] getAdapterList() {
        return new Class<?>[] { IDiagramEditorPart.class };
    }
}

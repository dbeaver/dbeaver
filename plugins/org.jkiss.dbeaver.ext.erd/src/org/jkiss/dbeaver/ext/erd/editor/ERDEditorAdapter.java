/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.swt.widgets.Control;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * ERDEditorAdapter
 */
public class ERDEditorAdapter implements IAdapterFactory {

    private static Map<Control, ERDEditorPart> editorsMap = new IdentityHashMap<>();

    static synchronized void mapControl(Control control, ERDEditorPart editor)
    {
        editorsMap.put(control, editor);
    }

    static synchronized void unmapControl(Control control)
    {
        editorsMap.remove(control);
    }

    public static synchronized ERDEditorPart getEditor(Control control)
    {
        return editorsMap.get(control);
    }

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == ERDEditorPart.class) {
            if (adaptableObject instanceof Control) {
                return adapterType.cast(getEditor((Control) adaptableObject));
            }
        }
        return null;
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[] { ERDEditorPart.class };
    }
}

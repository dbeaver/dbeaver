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
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == ERDEditorPart.class) {
            if (adaptableObject instanceof Control) {
                return getEditor((Control) adaptableObject);
            }
        }
        return null;
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[] { ERDEditorPart.class };
    }
}

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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.swt.widgets.Control;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * ERDEditorAdapter
 */
public class ERDEditorAdapter implements IAdapterFactory {

    private static Map<Control, ERDEditorPart> editorsMap = new IdentityHashMap<Control, ERDEditorPart>();

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

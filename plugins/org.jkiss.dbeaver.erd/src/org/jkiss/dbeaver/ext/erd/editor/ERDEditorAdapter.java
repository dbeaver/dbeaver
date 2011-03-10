/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == ERDEditorPart.class) {
            if (adaptableObject instanceof Control) {
                return getEditor((Control) adaptableObject);
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { ERDEditorPart.class };
    }
}

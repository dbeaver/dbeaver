/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.gef.EditPart;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditor;
import org.jkiss.dbeaver.ext.erd.model.ERDObject;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * ERDEditorAdapter
 */
public class ERDEditorAdapter implements IAdapterFactory {

    private static Map<Control, ERDEditor> editorsMap = new IdentityHashMap<Control, ERDEditor>();

    static synchronized void mapControl(Control control, ERDEditor editor)
    {
        editorsMap.put(control, editor);
    }

    static synchronized void unmapControl(Control control)
    {
        editorsMap.remove(control);
    }

    public static synchronized ERDEditor getEditor(Control control)
    {
        return editorsMap.get(control);
    }

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == ERDEditor.class) {
            if (adaptableObject instanceof Control) {
                return getEditor((Control) adaptableObject);
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { ERDEditor.class };
    }
}

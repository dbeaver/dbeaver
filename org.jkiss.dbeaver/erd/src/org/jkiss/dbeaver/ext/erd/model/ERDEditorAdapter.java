/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.gef.EditPart;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditor;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * ERDEditorAdapter
 */
public class ERDEditorAdapter implements IAdapterFactory {

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == ERDEditor.class) {
            if (adaptableObject instanceof MultiPageEditorPart) {
                //((MultiPageEditorPart)adaptableObject).get
                //Object adapter = ((IAdaptable) adaptableObject).getAdapter(adapterType);
                //if (adapter != null) {
                //    System.out.println("sdafsdf");
                //}
                //return adapter;
                return null;
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { ERDObject.class };
    }
}

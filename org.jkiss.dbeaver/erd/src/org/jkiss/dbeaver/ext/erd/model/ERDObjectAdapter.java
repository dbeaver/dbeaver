/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.gef.EditPart;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object adapter
 */
public class ERDObjectAdapter implements IAdapterFactory {

    public ERDObjectAdapter() {
    }

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == DBPNamedObject.class || adapterType == DBPObject.class || adapterType == DBSObject.class) {
            if (adaptableObject instanceof EditPart) {
                Object model = ((EditPart) adaptableObject).getModel();
                if (model instanceof ERDObject) {
                    return ((ERDObject<?>)model).getObject();
                }
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { ERDObject.class };
    }
}

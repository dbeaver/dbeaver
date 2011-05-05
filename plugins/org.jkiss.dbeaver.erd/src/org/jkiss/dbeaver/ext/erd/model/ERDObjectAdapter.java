/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.gef.EditPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * ERD object adapter
 */
public class ERDObjectAdapter implements IAdapterFactory {

    public ERDObjectAdapter() {
    }

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (DBPObject.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof EditPart) {
                Object model = ((EditPart) adaptableObject).getModel();
                if (model != null && adapterType.isAssignableFrom(model.getClass())) {
                    return model;
                }
                if (model instanceof ERDObject) {
                    Object object = ((ERDObject<?>) model).getObject();
                    if (object != null && adapterType.isAssignableFrom(object.getClass())) {
                        return object;
                    }
                }
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { ERDObject.class, DBPNamedObject.class, DBSObject.class, DBNNode.class };
    }
}

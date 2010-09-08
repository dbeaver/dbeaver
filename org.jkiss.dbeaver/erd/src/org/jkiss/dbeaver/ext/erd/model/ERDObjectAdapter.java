/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.model.navigator.DBNNode;

/**
 * Object adapter
 */
public class ERDObjectAdapter implements IAdapterFactory {

    public ERDObjectAdapter() {
    }

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == DBNNode.class) {
            return null;
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { ERDObject.class };
    }
}

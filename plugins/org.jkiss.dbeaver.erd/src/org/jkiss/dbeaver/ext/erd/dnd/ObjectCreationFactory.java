/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.dnd;

import org.eclipse.gef.requests.CreationFactory;

/**
 * Simple object creation factory
 */
public class ObjectCreationFactory implements CreationFactory {

    private final Object newObject;
    private final Object objectType;

    public ObjectCreationFactory(Object newObject, Object objectType)
    {
        this.newObject = newObject;
        this.objectType = objectType;
    }

    @Override
    public Object getNewObject()
    {
        return newObject;
    }

    @Override
    public Object getObjectType()
    {
        return objectType;
    }
}

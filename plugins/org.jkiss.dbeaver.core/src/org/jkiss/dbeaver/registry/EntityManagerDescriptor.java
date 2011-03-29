/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;

/**
 * EntityEditorDescriptor
 */
public class EntityManagerDescriptor extends AbstractDescriptor
{
    private String id;
    private String className;
    private String objectType;
    private String name;

    private Class objectClass;
    private Class managerClass;
    private DBEObjectManager managerInstance;

    EntityManagerDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute("id");
        this.className = config.getAttribute("class");
        this.objectType = config.getAttribute("objectType");
        this.name = config.getAttribute("label");
    }

    void dispose()
    {
        objectClass = null;
        managerClass = null;
        managerInstance = null;
    }

    public String getId()
    {
        return id;
    }

/*
    public String getClassName()
    {
        return className;
    }

    public String getObjectType()
    {
        return objectType;
    }

*/
    public String getName()
    {
        return name;
    }

    public boolean appliesToType(Class objectType)
    {
        return this.getObjectClass() != null && this.getObjectClass().isAssignableFrom(objectType);
    }

    public Class getObjectClass()
    {
        if (objectClass == null) {
            objectClass = getObjectClass(objectType);
        }
        return objectClass;
    }

    public Class getManagerClass()
    {
        if (managerClass == null) {
            managerClass = getObjectClass(className);
        }
        return managerClass;
    }

    public synchronized DBEObjectManager getManager()
    {
        if (managerInstance != null) {
            return managerInstance;
        }
        Class clazz = getManagerClass();
        if (clazz == null) {
            throw new IllegalStateException("Can't load manager class '" + className + "'");
        }
        try {
            managerInstance = (DBEObjectManager) clazz.newInstance();
        } catch (Throwable ex) {
            throw new IllegalStateException("Error instantiating entity manager '" + className + "'", ex);
        }
        return managerInstance;
    }
}
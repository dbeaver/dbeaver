/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.edit.DBOManager;

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

    public EntityManagerDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute("id");
        this.className = config.getAttribute("class");
        this.objectType = config.getAttribute("objectType");
        this.name = config.getAttribute("label");
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

    public DBOManager<?> createManager()
    {
        Class clazz = getManagerClass();
        if (clazz == null) {
            return null;
        }
        try {
            return (DBOManager) clazz.newInstance();
        } catch (Throwable ex) {
            log.error("Error instantiating entity manager '" + className + "'", ex);
            return null;
        }
    }
}
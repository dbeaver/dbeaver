/*
 * Copyright (C) 2010-2012 Serge Rieder
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

    private Class objectClass;
    private Class managerClass;
    private DBEObjectManager managerInstance;

    EntityManagerDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = this.className = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.objectType = config.getAttribute(RegistryConstants.ATTR_OBJECT_TYPE);
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
    public boolean appliesToType(Class objectType)
    {
        return this.getObjectClass() != null && this.getObjectClass().isAssignableFrom(objectType);
    }

    public Class<?> getObjectClass()
    {
        if (objectClass == null) {
            objectClass = getObjectClass(objectType);
        }
        return objectClass;
    }

    public Class<?> getManagerClass()
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
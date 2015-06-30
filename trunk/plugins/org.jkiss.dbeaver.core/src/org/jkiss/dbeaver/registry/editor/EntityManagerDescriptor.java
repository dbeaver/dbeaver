/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.registry.editor;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

/**
 * EntityEditorDescriptor
 */
public class EntityManagerDescriptor extends AbstractDescriptor
{
    private String id;
    private ObjectType managerType;
    private ObjectType objectType;
    private DBEObjectManager managerInstance;

    EntityManagerDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.managerType = new ObjectType(id);
        this.objectType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_OBJECT_TYPE));
    }

    void dispose()
    {
        objectType = null;
        managerType = null;
        managerInstance = null;
    }

    public String getId()
    {
        return id;
    }

    public boolean appliesToType(Class clazz)
    {
        return objectType.matchesType(clazz);
    }

    public synchronized DBEObjectManager getManager()
    {
        if (managerInstance != null) {
            return managerInstance;
        }
        Class<? extends DBEObjectManager> clazz = managerType.getObjectClass(DBEObjectManager.class);
        if (clazz == null) {
            throw new IllegalStateException("Can't instantiate entity manager '" + managerType.getImplName() + "'");
        }
        try {
            managerInstance = clazz.newInstance();
        } catch (Throwable ex) {
            throw new IllegalStateException("Error instantiating entity manager '" + clazz.getName() + "'", ex);
        }
        return managerInstance;
    }
}
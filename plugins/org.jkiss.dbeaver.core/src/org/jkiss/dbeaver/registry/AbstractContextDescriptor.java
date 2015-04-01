/*
 * Copyright (C) 2010-2015 Serge Rieder
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
import org.jkiss.dbeaver.DBeaverConstants;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ToolDescriptor
 */
public abstract class AbstractContextDescriptor extends AbstractDescriptor
{
    private List<ObjectType> objectTypes = new ArrayList<ObjectType>();

    public AbstractContextDescriptor(IConfigurationElement config)
    {
        super(config == null ? DBeaverConstants.PLUGIN_ID : config.getContributor().getName());
        if (config != null) {
            String objectType = config.getAttribute(RegistryConstants.ATTR_OBJECT_TYPE);
            if (objectType != null) {
                objectTypes.add(new ObjectType(objectType));
            }
            for (IConfigurationElement typeCfg : config.getChildren(RegistryConstants.TAG_OBJECT_TYPE)) {
                objectTypes.add(new ObjectType(typeCfg));
            }
        }
    }

    public boolean appliesTo(DBPObject object)
    {
        return appliesTo(object, null);
    }

    public boolean appliesTo(DBPObject object, Object context)
    {
        object = DBUtils.getPublicObject(object);
        if (object == null) {
            return false;
        }
        Object adapted = adaptType(object);
        for (ObjectType objectType : objectTypes) {
            if (objectType.appliesTo(object, context) || (adapted != null && objectType.appliesTo(adapted, context))) {
                return true;
            }
        }
        return false;
    }

    protected Object adaptType(DBPObject object)
    {
        return null;
    }
}

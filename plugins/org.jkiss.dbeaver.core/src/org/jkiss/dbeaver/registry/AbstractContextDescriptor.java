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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * ToolDescriptor
 */
public abstract class AbstractContextDescriptor extends AbstractDescriptor
{
    private static final String OBJECT_TYPE = "objectType";

    private List<ObjectType> objectTypes = new ArrayList<>();

    public AbstractContextDescriptor(IConfigurationElement config)
    {
        super(config == null ? DBeaverCore.PLUGIN_ID : config.getContributor().getName());
        if (config != null) {
            String objectType = config.getAttribute(OBJECT_TYPE);
            if (objectType != null) {
                objectTypes.add(new ObjectType(objectType));
            }
            for (IConfigurationElement typeCfg : config.getChildren(OBJECT_TYPE)) {
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

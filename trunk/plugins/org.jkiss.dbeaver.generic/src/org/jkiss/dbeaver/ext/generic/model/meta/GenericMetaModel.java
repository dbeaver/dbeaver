/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.ext.generic.model.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic meta model
 */
public class GenericMetaModel {

    private String id;
    private final Map<String, GenericMetaObject> objects = new HashMap<String, GenericMetaObject>();

    public GenericMetaModel(IConfigurationElement cfg)
    {
        this.id = cfg.getAttribute(RegistryConstants.ATTR_ID);
        IConfigurationElement[] objectList = cfg.getChildren("object");
        if (!CommonUtils.isEmpty(objectList)) {
            for (IConfigurationElement childConfig : objectList) {
                GenericMetaObject metaObject = new GenericMetaObject(childConfig);
                objects.put(metaObject.getType(), metaObject);
            }
        }
    }

    public String getId()
    {
        return id;
    }

    public GenericMetaObject getObject(String id)
    {
        return objects.get(id);
    }

}

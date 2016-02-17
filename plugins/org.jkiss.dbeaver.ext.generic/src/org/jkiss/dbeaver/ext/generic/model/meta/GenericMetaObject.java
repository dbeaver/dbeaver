/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.generic.model.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.registry.RegistryConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Meta object description
 */
public class GenericMetaObject {

    private final String type;
    private final String readQuery;
    private final Map<String, GenericMetaColumn> columnsMap = new HashMap<>();

    public GenericMetaObject(IConfigurationElement cfg)
    {
        this.type = cfg.getAttribute(RegistryConstants.ATTR_TYPE);
        this.readQuery = cfg.getAttribute("read-query");
        for (IConfigurationElement columnCfg : cfg.getChildren("column")) {
            GenericMetaColumn column = new GenericMetaColumn(columnCfg);
            columnsMap.put(column.getId(), column);
        }
    }

    public String getType()
    {
        return type;
    }

    public String getReadQuery()
    {
        return readQuery;
    }

    public GenericMetaColumn getColumn(String id)
    {
        return columnsMap.get(id);
    }

}

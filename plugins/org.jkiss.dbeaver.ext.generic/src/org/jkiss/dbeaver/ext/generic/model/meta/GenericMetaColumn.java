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
import org.jkiss.utils.CommonUtils;

/**
 * Meta column mapping
 */
public class GenericMetaColumn {

    private final String id;
    private final String columnName;
    private final Integer columnIndex;
    private final boolean supported;

    public GenericMetaColumn(IConfigurationElement cfg)
    {
        this.id = cfg.getAttribute(RegistryConstants.ATTR_ID);
        this.columnName = cfg.getAttribute(RegistryConstants.ATTR_NAME);
        String indexStr = cfg.getAttribute("index");
        if (!CommonUtils.isEmpty(indexStr)) {
            this.columnIndex = Integer.valueOf(indexStr);
        } else {
            this.columnIndex = null;
        }
        String supportedStr = cfg.getAttribute("supported");
        this.supported = !"false".equals(supportedStr);
    }

    public String getId()
    {
        return id;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public int getColumnIndex()
    {
        return columnIndex;
    }

    public boolean isSupported()
    {
        return supported;
    }

    public Object getColumnIdentifier()
    {
        return columnIndex == null ? columnName : columnIndex;
    }
}

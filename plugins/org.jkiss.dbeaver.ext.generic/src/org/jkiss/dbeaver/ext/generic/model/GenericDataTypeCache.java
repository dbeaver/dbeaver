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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;

/**
 * GenericDataTypeCache
 */
public class GenericDataTypeCache extends JDBCBasicDataTypeCache
{

    public GenericDataTypeCache(DBPDataSourceContainer owner) {
        super(owner);

        // Ignore abstract types. There can be multiple numeric types with the same name
        // but different scale/precision properties
        ignoredTypes.add("NUMBER");
        ignoredTypes.add("NUMERIC");
    }

}

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

package org.jkiss.dbeaver.model.struct;

/**
 * DBSDataKind
 */
public enum DBSDataKind
{
    BOOLEAN("BOOLEAN"),
    NUMERIC("NUMERIC"),
    STRING("VARCHAR"),
    DATETIME("TIMESTAMP"),
    BINARY("BLOB"),
    LOB("BLOB"),
    STRUCT("VARCHAR"),
    ARRAY("VARCHAR"),
    OBJECT("VARCHAR"),
    REFERENCE("VARCHAR"),
    ROWID("ROWID"),
    UNKNOWN("VARCHAR");

    private final String defaultTypeName;

    private DBSDataKind(String defaultTypeName)
    {
        this.defaultTypeName = defaultTypeName;
    }

    public String getDefaultTypeName()
    {
        return defaultTypeName;
    }
}

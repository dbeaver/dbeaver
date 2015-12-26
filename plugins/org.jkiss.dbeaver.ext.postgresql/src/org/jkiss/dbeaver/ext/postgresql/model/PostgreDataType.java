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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * PostgreTypeType
 */
public class PostgreDataType extends JDBCDataType implements PostgreObject
{
    private final int typeId;
    private final String ownerSchema;
    private final PostgreTypeType typeType;
    private final PostgreTypeCategory typeCategory;

    public PostgreDataType(DBSObject owner, int valueType, String name, int length, int typeId, String ownerSchema, PostgreTypeType typeType, PostgreTypeCategory typeCategory) {
        super(owner, valueType, name, null, false, true, length, -1, -1);
        this.typeId = typeId;
        this.ownerSchema = ownerSchema;
        this.typeType = typeType;
        this.typeCategory = typeCategory;
    }

    @Override
    @Property
    public int getObjectId() {
        return typeId;
    }

    @Property
    public String getOwnerSchema() {
        return ownerSchema;
    }

    @Property
    public PostgreTypeType getTypeType() {
        return typeType;
    }

    @Property
    public PostgreTypeCategory getTypeCategory() {
        return typeCategory;
    }

}

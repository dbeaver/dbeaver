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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.model.DBIcon;

/**
 * Relational database object type.
 * Used by structure assistants
 */
public class RelationalObjectType implements DBSObjectType {

    public static final DBSObjectType TYPE_TABLE = new RelationalObjectType("Table", "Table or View", DBIcon.TREE_TABLE, DBSTable.class);
    public static final DBSObjectType TYPE_TABLE_COLUMN = new RelationalObjectType("Table column", "Table column", DBIcon.TREE_COLUMN, DBSTableColumn.class);
    public static final DBSObjectType TYPE_INDEX = new RelationalObjectType("Index", "Index", DBIcon.TREE_INDEX, DBSTableIndex.class);
    public static final DBSObjectType TYPE_CONSTRAINT = new RelationalObjectType("Constraint", "Table constraint", DBIcon.TREE_CONSTRAINT, DBSTableConstraint.class);
    public static final DBSObjectType TYPE_PROCEDURE = new RelationalObjectType("Procedure", "Procedure or function", DBIcon.TREE_PROCEDURE, DBSProcedure.class);
    public static final DBSObjectType TYPE_TRIGGER = new RelationalObjectType("Trigger", "Trigger", DBIcon.TREE_TRIGGER, DBSTrigger.class);

    private final String typeName;
    private final String description;
    private final DBPImage image;
    private final Class<? extends DBSObject> objectClass;

    RelationalObjectType(String typeName, String description, DBPImage image, Class<? extends DBSObject> objectClass)
    {
        this.typeName = typeName;
        this.description = description;
        this.image = image;
        this.objectClass = objectClass;
    }

    @Override
    public String getTypeName()
    {
        return typeName;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public DBPImage getImage()
    {
        return image;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass()
    {
        return objectClass;
    }

}

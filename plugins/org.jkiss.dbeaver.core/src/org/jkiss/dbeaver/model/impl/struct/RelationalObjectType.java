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
package org.jkiss.dbeaver.model.impl.struct;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Relational database object type.
 * Used by structure assistants
 */
public class RelationalObjectType implements DBSObjectType {

    public static final DBSObjectType TYPE_TABLE = new RelationalObjectType("Table", "Table or View", DBIcon.TREE_TABLE.getImage(), DBSTable.class);
    public static final DBSObjectType TYPE_TABLE_COLUMN = new RelationalObjectType("Table column", "Table column", DBIcon.TREE_COLUMN.getImage(), DBSTableColumn.class);
    public static final DBSObjectType TYPE_INDEX = new RelationalObjectType("Index", "Index", DBIcon.TREE_INDEX.getImage(), DBSTableIndex.class);
    public static final DBSObjectType TYPE_CONSTRAINT = new RelationalObjectType("Constraint", "Table constraint", DBIcon.TREE_CONSTRAINT.getImage(), DBSTableConstraint.class);
    public static final DBSObjectType TYPE_PROCEDURE = new RelationalObjectType("Procedure", "Procedure or function", DBIcon.TREE_PROCEDURE.getImage(), DBSProcedure.class);
    public static final DBSObjectType TYPE_TRIGGER = new RelationalObjectType("Trigger", "Trigger", DBIcon.TREE_TRIGGER.getImage(), DBSTrigger.class);

    private final String typeName;
    private final String description;
    private final Image image;
    private final Class<? extends DBSObject> objectClass;

    RelationalObjectType(String typeName, String description, Image image, Class<? extends DBSObject> objectClass)
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
    public Image getImage()
    {
        return image;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass()
    {
        return objectClass;
    }

}

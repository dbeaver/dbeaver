/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Relational database object type.
 * Used by structure assistants
 */
public class RelationalObjectType implements DBSObjectType {

    public static final DBSObjectType TYPE_TABLE = new RelationalObjectType("Table", "Table or View", DBIcon.TREE_TABLE.getImage(), DBSTable.class);
    public static final DBSObjectType TYPE_TABLE_COLUMN = new RelationalObjectType("Table column", "Table column", DBIcon.TREE_COLUMN.getImage(), DBSTableColumn.class);
    public static final DBSObjectType TYPE_INDEX = new RelationalObjectType("Index", "Index", DBIcon.TREE_INDEX.getImage(), DBSTableIndex.class);
    public static final DBSObjectType TYPE_CONSTRAINT = new RelationalObjectType("Constraint", "Table constraint (primary key, foreign key, unique, etc)", DBIcon.TREE_CONSTRAINT.getImage(), DBSTableConstraint.class);
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

    public String getTypeName()
    {
        return typeName;
    }

    public String getDescription()
    {
        return description;
    }

    public Image getImage()
    {
        return image;
    }

    public Class<? extends DBSObject> getTypeClass()
    {
        return objectClass;
    }

}

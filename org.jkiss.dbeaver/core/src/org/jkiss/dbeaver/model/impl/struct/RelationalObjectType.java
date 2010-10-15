/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Relational database object type.
 * Used by structure assistants
 */
public class RelationalObjectType implements DBSObjectType {

    public static final RelationalObjectType TYPE_TABLE = new RelationalObjectType("Table", "Table or View", DBIcon.TREE_TABLE.getImageDescriptor(), DBSTable.class);
    public static final RelationalObjectType TYPE_TABLE_COLUMN = new RelationalObjectType("Table column", "Table column", DBIcon.TREE_COLUMN.getImageDescriptor(), DBSTableColumn.class);
    public static final RelationalObjectType TYPE_INDEX = new RelationalObjectType("Index", "Index", DBIcon.TREE_INDEX.getImageDescriptor(), DBSIndex.class);
    public static final RelationalObjectType TYPE_CONSTRAINT = new RelationalObjectType("Constraint", "Table constraint (primary key, foreign key, unique, etc)", DBIcon.TREE_CONSTRAINT.getImageDescriptor(), DBSConstraint.class);
    public static final RelationalObjectType TYPE_PROCEDURE = new RelationalObjectType("Procedure", "Procedure or function", DBIcon.TREE_PROCEDURE.getImageDescriptor(), DBSProcedure.class);
    public static final RelationalObjectType TYPE_TRIGGER = new RelationalObjectType("Trigger", "Trigger", DBIcon.TREE_TRIGGER.getImageDescriptor(), DBSTrigger.class);

    private final String typeName;
    private final String description;
    private final ImageDescriptor image;
    private final Class<? extends DBSObject> objectClass;

    public RelationalObjectType(String typeName, String description, ImageDescriptor image, Class<? extends DBSObject> objectClass)
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

    public ImageDescriptor getImage()
    {
        return image;
    }

    public boolean validInstance(DBSObject object)
    {
        return object != null && objectClass.isAssignableFrom(object.getClass());
    }

}

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
public enum RelationalObjectType implements DBSObjectType {

    TYPE_TABLE("Table", "Table or View", DBIcon.TREE_TABLE.getImageDescriptor(), DBSTable.class),
    TYPE_TABLE_COLUMN("Table column", "Table column", DBIcon.TREE_COLUMN.getImageDescriptor(), DBSTableColumn.class),
    TYPE_INDEX("Index", "Index", DBIcon.TREE_INDEX.getImageDescriptor(), DBSIndex.class),
    TYPE_CONSTRAINT("Constraint", "Table constraint (primary key, foreign key, unique, etc)", DBIcon.TREE_CONSTRAINT.getImageDescriptor(), DBSConstraint.class),
    TYPE_PROCEDURE("Procedure", "Procedure or function", DBIcon.TREE_PROCEDURE.getImageDescriptor(), DBSProcedure.class),
    TYPE_TRIGGER("Trigger", "Trigger", DBIcon.TREE_TRIGGER.getImageDescriptor(), DBSTrigger.class);

    private final String typeName;
    private final String description;
    private final ImageDescriptor image;
    private final Class<? extends DBSObject> objectClass;

    RelationalObjectType(String typeName, String description, ImageDescriptor image, Class<? extends DBSObject> objectClass)
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

    public Class<? extends DBSObject> getTypeClass()
    {
        return objectClass;
    }

}

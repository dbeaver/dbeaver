/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSTableConstraintColumn
 */
public interface DBSTableConstraintColumn extends DBSObject, DBSEntityAttributeRef
{
    DBSTableConstraint getParentObject();

    DBSTableColumn getAttribute();

    int getOrdinalPosition();
}

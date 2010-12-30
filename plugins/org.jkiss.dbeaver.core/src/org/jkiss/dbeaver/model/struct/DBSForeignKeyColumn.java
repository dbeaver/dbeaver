/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSForeignKeyColumn
 */
public interface DBSForeignKeyColumn extends DBSConstraintColumn
{
    DBSTableColumn getReferencedColumn();
}

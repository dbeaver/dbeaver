/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSTableForeignKeyColumn
 */
public interface DBSTableForeignKeyColumn extends DBSTableConstraintColumn
{
    DBSTableColumn getReferencedColumn();
}

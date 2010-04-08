package org.jkiss.dbeaver.model.struct;

/**
 * DBSForeignKeyColumn
 */
public interface DBSForeignKeyColumn extends DBSConstraintColumn
{
    DBSTableColumn getReferencedColumn();
}

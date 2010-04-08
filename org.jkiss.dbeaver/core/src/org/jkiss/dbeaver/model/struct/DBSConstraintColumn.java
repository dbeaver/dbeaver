package org.jkiss.dbeaver.model.struct;

/**
 * DBSConstraintColumn
 */
public interface DBSConstraintColumn extends DBSObject
{
    DBSConstraint getConstraint();

    DBSTableColumn getTableColumn();

    int getOrdinalPosition();
}

package org.jkiss.dbeaver.model.struct;

import java.util.Collection;

/**
 * DBSConstraint
 */
public interface DBSConstraint extends DBSObject
{
    DBSConstraintType getConstraintType();

    DBSTable getTable();

    Collection<? extends DBSConstraintColumn> getColumns();

    DBSConstraintColumn getColumn(DBSTableColumn tableColumn);

}

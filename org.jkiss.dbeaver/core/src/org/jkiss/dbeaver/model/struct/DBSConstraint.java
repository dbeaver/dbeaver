package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

import java.util.Collection;

/**
 * DBSConstraint
 */
public interface DBSConstraint<DATASOURCE extends DBPDataSource, TABLE extends DBSTable> extends DBSObject<DATASOURCE>
{
    DBSConstraintType getConstraintType();

    TABLE getTable();

    Collection<? extends DBSConstraintColumn<DATASOURCE>> getColumns();

    DBSConstraintColumn<DATASOURCE> getColumn(DBSTableColumn tableColumn);

}

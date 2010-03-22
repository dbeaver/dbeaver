package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSConstraintColumn
 */
public interface DBSConstraintColumn<DATASOURCE extends DBPDataSource> extends DBSObject<DATASOURCE>
{
    DBSConstraint getConstraint();

    DBSTableColumn getTableColumn();

    int getOrdinalPosition();
}

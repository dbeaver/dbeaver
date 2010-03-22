package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSForeignKeyColumn
 */
public interface DBSForeignKeyColumn<DATASOURCE extends DBPDataSource> extends DBSConstraintColumn<DATASOURCE>
{
    DBSTableColumn getReferencedColumn();
}

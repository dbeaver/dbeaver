package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSPrimaryKey
 */
public interface DBSPrimaryKey<DATASOURCE extends DBPDataSource, TABLE extends DBSTable>
    extends DBSUniqueKey<DATASOURCE, TABLE>
{
}

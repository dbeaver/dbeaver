package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSPrimaryKey
 */
public interface DBSUniqueKey<DATASOURCE extends DBPDataSource, TABLE extends DBSTable> 
    extends DBSConstraint<DATASOURCE, TABLE>
{
}
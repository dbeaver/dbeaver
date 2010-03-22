package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSIndex
 */
public interface DBSProcedureColumn<DATASOURCE extends DBPDataSource> extends DBSColumnDefinition<DATASOURCE>
{

    DBSProcedure getProcedure();

    DBSProcedureColumnType getColumnType();
}
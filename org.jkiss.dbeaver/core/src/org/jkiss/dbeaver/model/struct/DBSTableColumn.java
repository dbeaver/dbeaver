package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSTableColumn
 */
public interface DBSTableColumn<DATASOURCE extends DBPDataSource> extends DBSColumnDefinition<DATASOURCE>
{
    DBSTable getTable();

    int getOrdinalPosition();

    String getDefaultValue();
}

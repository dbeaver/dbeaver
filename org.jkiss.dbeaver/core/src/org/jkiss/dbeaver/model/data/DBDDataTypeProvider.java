package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBPDataTypeProvider
 */
public interface DBDDataTypeProvider
{
    String getName();

    String[] getSupportedDatabases();

    String[] getSupportedDataTypes();

    DBDValueHandler createHandler(
        DBPDataSource dataSource,
        int typeNumber,
        String typeName);

}
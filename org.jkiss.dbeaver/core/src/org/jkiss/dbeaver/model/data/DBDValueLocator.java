/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSTable;

import java.util.List;

/**
 * Value locator.
 * Unique identifier of row in certain table.
 */
public class DBDValueLocator {

    private DBSTable table;
    private DBSConstraint uniqueKey;
    private List<? extends DBCColumnMetaData> keyColumns;

    public DBDValueLocator(DBSTable table, DBSConstraint uniqueKey, List<? extends DBCColumnMetaData> keyColumns)
    {
        this.table = table;
        this.uniqueKey = uniqueKey;
        this.keyColumns = keyColumns;
    }

    public DBSTable getTable() {
        return table;
    }

    public DBSConstraint getUniqueKey() {
        return uniqueKey;
    }

    public List<? extends DBCColumnMetaData> getKeyColumns()
    {
        return keyColumns;
    }

/*
    public Object[] getKeyValues(Object[] row) {
        Object[] keyValues = new Object[keyColumns.size()];
        for (DBSTableColumn column : keyColumns) {
            keyColumns
        }
        return keyValues;
    }
*/
}

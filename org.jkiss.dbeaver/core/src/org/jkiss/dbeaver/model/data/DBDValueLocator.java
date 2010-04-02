package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.DBSUniqueKey;

import java.util.Map;

/**
 * Value locator.
 * Unique identifier of row in certain table.
 */
public class DBDValueLocator {

    private DBSTable table;
    private DBSUniqueKey uniqueKey;
    private Map<DBSTableColumn, Object> keyValues;

    public DBDValueLocator(DBSTable table, DBSUniqueKey uniqueKey, Map<DBSTableColumn, Object> keyValues) {
        this.table = table;
        this.uniqueKey = uniqueKey;
        this.keyValues = keyValues;
    }

    public DBSTable getTable() {
        return table;
    }

    public DBSUniqueKey getUniqueKey() {
        return uniqueKey;
    }

    public Map<DBSTableColumn, Object> getKeyValues() {
        return keyValues;
    }
}

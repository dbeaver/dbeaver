/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

/**
 * Column value binding info
 */
public class DBDColumnBinding {
    private final DBCColumnMetaData column;
    private final DBDValueHandler valueHandler;
    private DBSEntityAttribute tableColumn;
    private DBDValueLocator valueLocator;

    public DBDColumnBinding(DBCColumnMetaData column, DBDValueHandler valueHandler) {
        this.column = column;
        this.valueHandler = valueHandler;
    }

    public String getColumnName()
    {
        return column.getName();
    }

    public int getColumnIndex()
    {
        return column.getIndex();
    }

    public DBCColumnMetaData getColumn() {
        return column;
    }

    public DBDValueHandler getValueHandler() {
        return valueHandler;
    }

    public DBSEntityAttribute getTableColumn()
    {
        return tableColumn;
    }

    public DBDValueLocator getValueLocator() {
        return valueLocator;
    }

    public void initValueLocator(DBSEntityAttribute tableColumn, DBDValueLocator valueLocator) {
        this.tableColumn = tableColumn;
        this.valueLocator = valueLocator;
    }
}
